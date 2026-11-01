package com.olehkobylianskyi.familypoints.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.crypto.SecretKey;
import java.util.*;

/**
 * Transaction-rolled-back PostgreSQL archive roundtrip for the explicitly
 * supported group/media slice. It cannot write to live family tables and is
 * not a production restore executor or a VERIFIED backup certificate.
 */
@Service
public final class WorkspaceDisposableRestoreRehearsal {
    private static final Set<String> SLICE=Set.of("MEMBER_GROUPS","MEDIA_ASSETS","MEDIA_BINDINGS");
    private final JdbcTemplate db;
    private final TransactionTemplate tx;
    private final ProtectiveBackupPortableFile portable;
    private final ProtectiveBackupArtifactCodec codec;
    private final ProtectiveBackupImportInspector inspector;
    private final ObjectMapper json=new ObjectMapper();

    public WorkspaceDisposableRestoreRehearsal(JdbcTemplate db,PlatformTransactionManager manager,
            ProtectiveBackupPortableFile portable,ProtectiveBackupArtifactCodec codec,
            ProtectiveBackupImportInspector inspector) {
        this.db=db;this.tx=new TransactionTemplate(manager);
        this.portable=portable;this.codec=codec;this.inspector=inspector;
    }
    public record Report(long workspaceId,UUID snapshotId,int groups,int assets,int bindings,
                         int checkedVariants,boolean rolledBack) {}

    public Report rehearse(byte[] file,long workspaceId,SecretKey key) {
        var inspection=inspector.inspect(file,workspaceId,key);
        if(!inspection.availableCategories().containsAll(SLICE))
            throw new IllegalArgumentException("REHEARSAL_REQUIRES_GROUPS_AND_MEDIA");
        var envelope=portable.decode(file);
        byte[] bytes=codec.open(envelope,envelope.manifest().snapshotId(),workspaceId,key);
        final JsonNode cats;
        try {
            cats=json.readTree(bytes).path("categories");
        } catch(Exception e) {
            throw new IllegalArgumentException("Malformed archive",e);
        } finally {
            Arrays.fill(bytes,(byte)0);
        }
        return tx.execute(status->{
            // Fixed SQL only. No production family table insert, delete or update.
            db.execute("CREATE TEMP TABLE fp_rehearsal_groups(id bigint PRIMARY KEY, family_id bigint NOT NULL, name text NOT NULL) ON COMMIT DROP");
            db.execute("CREATE TEMP TABLE fp_rehearsal_assets(id uuid PRIMARY KEY, workspace_id bigint NOT NULL, digest text NOT NULL) ON COMMIT DROP");
            db.execute("CREATE TEMP TABLE fp_rehearsal_variants(asset_id uuid REFERENCES fp_rehearsal_assets(id), variant text NOT NULL, data bytea NOT NULL, PRIMARY KEY(asset_id,variant)) ON COMMIT DROP");
            db.execute("CREATE TEMP TABLE fp_rehearsal_bindings(group_id bigint REFERENCES fp_rehearsal_groups(id), asset_id uuid REFERENCES fp_rehearsal_assets(id), slot text NOT NULL) ON COMMIT DROP");
            // All archived category identities are staged as JSONB, even
            // categories that cannot yet be safely reconstructed in normal
            // FamilyPoints tables. This allows a complete byte-for-byte
            // PostgreSQL archive roundtrip without enabling Restore Apply.
            db.execute("""
                    CREATE TEMP TABLE fp_rehearsal_archive_rows(
                      category text NOT NULL, ordinal int NOT NULL,
                      payload jsonb NOT NULL, PRIMARY KEY(category,ordinal)
                    ) ON COMMIT DROP
                    """);
            int archivedRows=0;
            for(var fieldIterator=cats.fields();fieldIterator.hasNext();) {
                var category=fieldIterator.next();
                int ordinal=0;
                for(JsonNode item:category.getValue()) {
                    String serialized;
                    try {serialized=json.writeValueAsString(item);}
                    catch(Exception e) {throw new IllegalArgumentException("REHEARSAL_JSON_ENCODE_FAILURE",e);}
                    db.update("INSERT INTO fp_rehearsal_archive_rows(category,ordinal,payload) VALUES (?,?,?::jsonb)",
                            category.getKey(),ordinal++,serialized);
                    archivedRows++;
                }
                int expected=inspection.counts().get(category.getKey());
                if(ordinal!=expected)throw new IllegalStateException("REHEARSAL_ARCHIVE_COUNT_MISMATCH");
            }
            Long sqlRows=db.queryForObject("SELECT count(*) FROM fp_rehearsal_archive_rows",Long.class);
            if(sqlRows==null || sqlRows!=archivedRows)
                throw new IllegalStateException("REHEARSAL_ARCHIVE_ROWS_MISMATCH");
            for(var category:inspection.availableCategories()) {
                Long persisted=db.queryForObject(
                        "SELECT count(*) FROM fp_rehearsal_archive_rows WHERE category=?",
                        Long.class,category);
                if(persisted==null || persisted!=inspection.counts().get(category))
                    throw new IllegalStateException("REHEARSAL_CATEGORY_MISMATCH:"+category);
            }
            int groups=0,assets=0,bindings=0,variants=0;
            for(JsonNode row:cats.path("MEMBER_GROUPS")) {
                if(row.path("family_id").asLong()!=workspaceId)throw new SecurityException("GROUP_TENANT_MISMATCH");
                db.update("INSERT INTO fp_rehearsal_groups(id,family_id,name) VALUES (?,?,?)",
                        row.path("id").asLong(),workspaceId,row.path("name").asText());
                groups++;
            }
            for(JsonNode row:cats.path("MEDIA_ASSETS")) {
                if(row.path("workspace_id").asLong()!=workspaceId)throw new SecurityException("MEDIA_TENANT_MISMATCH");
                UUID id=UUID.fromString(row.path("id").asText());
                String digest=row.path("sha256").asText();
                db.update("INSERT INTO fp_rehearsal_assets(id,workspace_id,digest) VALUES (?,?,?)",id,workspaceId,digest);
                assets++;
                for(JsonNode variant:row.path("blobs")) {
                    byte[] decoded=Base64.getDecoder().decode(variant.path("bytesBase64").asText());
                    try {
                        db.update("INSERT INTO fp_rehearsal_variants(asset_id,variant,data) VALUES (?,?,?)",
                                id,variant.path("variant").asText(),decoded);
                        var roundtrip=db.queryForObject(
                                "SELECT data FROM fp_rehearsal_variants WHERE asset_id=? AND variant=?",
                                byte[].class,id,variant.path("variant").asText());
                        if(!Arrays.equals(decoded,roundtrip))
                            throw new IllegalStateException("REHEARSAL_MEDIA_BYTES_MISMATCH");
                        variants++;
                    } finally {Arrays.fill(decoded,(byte)0);}
                }
            }
            for(JsonNode row:cats.path("MEDIA_BINDINGS")) {
                if(row.path("workspace_id").asLong()!=workspaceId)
                    throw new SecurityException("BINDING_TENANT_MISMATCH");
                // This slice deliberately supports only group-owned asset bindings.
                // Catalog bindings and other targets require their own approved mapping.
                if(!"MEMBER_GROUP".equals(row.path("target_type").asText())
                        || !row.hasNonNull("asset_id"))
                    throw new IllegalArgumentException("REHEARSAL_BINDING_MAPPING_UNSUPPORTED");
                db.update("INSERT INTO fp_rehearsal_bindings(group_id,asset_id,slot) VALUES (?,?,?)",
                        row.path("target_id").asLong(),
                        UUID.fromString(row.path("asset_id").asText()),row.path("slot").asText());
                bindings++;
            }
            if(groups!=inspection.counts().get("MEMBER_GROUPS")
                    || assets!=inspection.counts().get("MEDIA_ASSETS")
                    || bindings!=inspection.counts().get("MEDIA_BINDINGS"))
                throw new IllegalStateException("REHEARSAL_ROW_COUNT_MISMATCH");
            if(!Objects.equals((long)groups,db.queryForObject("SELECT count(*) FROM fp_rehearsal_groups",Long.class))
                    || !Objects.equals((long)assets,db.queryForObject("SELECT count(*) FROM fp_rehearsal_assets",Long.class))
                    || !Objects.equals((long)variants,db.queryForObject("SELECT count(*) FROM fp_rehearsal_variants",Long.class)))
                throw new IllegalStateException("REHEARSAL_STAGED_ROW_COUNT_MISMATCH");
            if(!Objects.equals((long)bindings,db.queryForObject(
                    "SELECT count(*) FROM fp_rehearsal_bindings",Long.class)))
                throw new IllegalStateException("REHEARSAL_BINDINGS_MISMATCH");
            // Verify every binding points to the declared tenant and that no
            // nullable FK evaded the staging constraints.
            Long invalid=db.queryForObject("""
                    SELECT count(*) FROM fp_rehearsal_bindings b
                    JOIN fp_rehearsal_groups g ON g.id=b.group_id
                    JOIN fp_rehearsal_assets a ON a.id=b.asset_id
                    WHERE g.family_id<>? OR a.workspace_id<>?
                    """,Long.class,workspaceId,workspaceId);
            if(invalid==null || invalid!=0)
                throw new SecurityException("REHEARSAL_CROSS_TENANT_REFERENCE");
            status.setRollbackOnly();
            return new Report(workspaceId,inspection.snapshotId(),groups,assets,bindings,variants,true);
        });
    }
}
