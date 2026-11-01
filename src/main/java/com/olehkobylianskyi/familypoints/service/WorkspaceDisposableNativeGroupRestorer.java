package com.olehkobylianskyi.familypoints.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.crypto.SecretKey;
import java.util.*;

/**
 * Deliberately rollback-only native-schema group restore proof. NOT callable
 * from controllers; no production restore or same-family destructive mutation.
 * Source and destination must be distinct disposable family identities.
 */
@Component
public final class WorkspaceDisposableNativeGroupRestorer {
    private final JdbcTemplate db;
    private final TransactionTemplate tx;
    private final ProtectiveBackupImportInspector inspector;
    private final ProtectiveBackupPortableFile portable;
    private final ProtectiveBackupArtifactCodec codec;
    private final ObjectMapper mapper=new ObjectMapper();

    public WorkspaceDisposableNativeGroupRestorer(JdbcTemplate db,PlatformTransactionManager manager,
            ProtectiveBackupImportInspector inspector,ProtectiveBackupPortableFile portable,
            ProtectiveBackupArtifactCodec codec) {
        this.db=db;this.tx=new TransactionTemplate(manager);
        this.inspector=inspector;this.portable=portable;this.codec=codec;
    }
    public record Proof(UUID snapshotId,long sourceWorkspace,long targetWorkspace,
            Map<Long,Long> groupIdMapping,int inserted,boolean rolledBack) {}

    public Proof rehearse(byte[] file,long sourceWorkspace,long targetWorkspace,SecretKey key) {
        if(sourceWorkspace<=0 || targetWorkspace<=0 || sourceWorkspace==targetWorkspace)
            throw new IllegalArgumentException("NATIVE_REHEARSAL_REQUIRES_DISTINCT_FAMILIES");
        var inspection=inspector.inspect(file,sourceWorkspace,key);
        boolean bindings=inspection.availableCategories().equals(
                List.of("MEDIA_BINDINGS","MEMBER_GROUPS"));
        if(!bindings && !inspection.availableCategories().equals(List.of("MEMBER_GROUPS")))
            throw new IllegalArgumentException("NATIVE_REHEARSAL_UNSUPPORTED_CATEGORIES");
        var artifact=portable.decode(file);
        byte[] clear=codec.open(artifact,artifact.manifest().snapshotId(),sourceWorkspace,key);
        final JsonNode rows;
        final JsonNode categories;
        try {categories=mapper.readTree(clear).path("categories");
            rows=categories.path("MEMBER_GROUPS");}
        catch(Exception e){throw new IllegalArgumentException("INVALID_REHEARSAL_ARCHIVE",e);}
        finally {Arrays.fill(clear,(byte)0);}
        return tx.execute(status->{
            // A rollback-only transaction still uses the real schema and FK
            // constraints; it must never be exposed as a production endpoint.
            Long source=db.queryForObject("SELECT id FROM families WHERE id=? FOR UPDATE",
                    Long.class,sourceWorkspace);
            Long target=db.queryForObject("SELECT id FROM families WHERE id=? FOR UPDATE",
                    Long.class,targetWorkspace);
            if(source==null || target==null)throw new IllegalArgumentException("REHEARSAL_FAMILY_MISSING");
            Long conflicts=db.queryForObject("SELECT count(*) FROM member_groups WHERE family_id=?",
                    Long.class,targetWorkspace);
            if(conflicts==null || conflicts>0)
                throw new IllegalArgumentException("REHEARSAL_TARGET_NOT_EMPTY");
            var mappings=new TreeMap<Long,Long>();
            for(JsonNode row:rows) {
                if(row.path("family_id").asLong()!=sourceWorkspace)
                    throw new SecurityException("REHEARSAL_TENANT_MISMATCH");
                long oldId=row.path("id").asLong();
                if(oldId<=0 || mappings.containsKey(oldId))
                    throw new IllegalArgumentException("REHEARSAL_DUPLICATE_GROUP");
                Long newId=db.queryForObject("""
                        INSERT INTO member_groups(family_id,name,active,show_in_navigation)
                        VALUES (?,?,?,?) RETURNING id
                        """,Long.class,targetWorkspace,row.path("name").asText(),
                        row.path("active").asBoolean(),row.path("show_in_navigation").asBoolean());
                if(newId==null || newId==oldId)
                    throw new IllegalStateException("REHEARSAL_ID_REMAP_FAILURE");
                mappings.put(oldId,newId);
            }
            if(bindings) {
                // Catalog-backed group binding has no asset ownership FK to
                // remap. Every other binding kind remains explicitly blocked.
                for(JsonNode row:categories.path("MEDIA_BINDINGS")) {
                    long originalGroup=row.path("target_id").asLong();
                    Long remapped=mappings.get(originalGroup);
                    if(remapped==null || !"MEMBER_GROUP".equals(row.path("target_type").asText())
                            || row.hasNonNull("asset_id") || !row.path("catalog_key").isTextual()
                            || row.path("workspace_id").asLong()!=sourceWorkspace)
                        throw new IllegalArgumentException("NATIVE_REHEARSAL_UNSUPPORTED_BINDING");
                    db.update("""
                            INSERT INTO media_bindings(workspace_id,target_type,target_id,slot,catalog_key)
                            VALUES (?,'MEMBER_GROUP',?,?,?)
                            """,targetWorkspace,remapped,row.path("slot").asText(),
                            row.path("catalog_key").asText());
                }
                Long restored=db.queryForObject(
                        "SELECT count(*) FROM media_bindings WHERE workspace_id=? AND target_type='MEMBER_GROUP'",
                        Long.class,targetWorkspace);
                if(restored==null || restored.longValue()!=inspection.counts().get("MEDIA_BINDINGS"))
                    throw new IllegalStateException("NATIVE_REHEARSAL_BINDINGS_MISMATCH");
            }
            if(mappings.size()!=inspection.counts().get("MEMBER_GROUPS"))
                throw new IllegalStateException("REHEARSAL_GROUP_COUNT_MISMATCH");
            Long stored=db.queryForObject("SELECT count(*) FROM member_groups WHERE family_id=?",
                    Long.class,targetWorkspace);
            if(stored==null || stored!=mappings.size())
                throw new IllegalStateException("REHEARSAL_NATIVE_APPLY_MISMATCH");
            status.setRollbackOnly();
            return new Proof(inspection.snapshotId(),sourceWorkspace,targetWorkspace,
                    Map.copyOf(mappings),mappings.size(),true);
        });
    }
}
