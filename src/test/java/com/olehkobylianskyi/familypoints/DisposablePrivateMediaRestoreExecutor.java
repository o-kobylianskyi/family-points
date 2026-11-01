package com.olehkobylianskyi.familypoints;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.olehkobylianskyi.familypoints.service.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import javax.crypto.SecretKey;
import java.util.*;

/**
 * Committing PRIVATE media reconstruction exclusively under src/test.
 * The owner and group IDs are explicitly mapped into a disposable target.
 * Never wire into application controllers or production services.
 */
final class DisposablePrivateMediaRestoreExecutor {
    private final JdbcTemplate db;
    private final TransactionTemplate tx;
    private final ProtectiveBackupImportInspector inspector;
    private final ProtectiveBackupPortableFile portable;
    private final ProtectiveBackupArtifactCodec codec;
    private final ObjectMapper mapper=new ObjectMapper();

    record Result(Map<UUID,UUID> assetIds,int blobs,int bindings) {}

    DisposablePrivateMediaRestoreExecutor(JdbcTemplate db,PlatformTransactionManager manager,
            ProtectiveBackupImportInspector inspector,ProtectiveBackupPortableFile portable,
            ProtectiveBackupArtifactCodec codec) {
        this.db=db;this.tx=new TransactionTemplate(manager);
        this.inspector=inspector;this.portable=portable;this.codec=codec;
    }

    Result apply(byte[] file,long source,long target,Map<Long,Long> memberIds,
                 Map<Long,Long> groupIds,SecretKey key) {
        if(source<=0 || target<=0 || source==target)
            throw new IllegalArgumentException("DISPOSABLE_DISTINCT_FAMILIES_REQUIRED");
        var inspection=inspector.inspect(file,source,key);
        if(!inspection.availableCategories().equals(List.of("MEDIA_ASSETS","MEDIA_BINDINGS")))
            throw new IllegalArgumentException("PRIVATE_MEDIA_SCOPE_UNSUPPORTED");
        var artifact=portable.decode(file);
        byte[] plain=codec.open(artifact,artifact.manifest().snapshotId(),source,key);
        final JsonNode cats;
        try {cats=mapper.readTree(plain).path("categories");}
        catch(Exception ex){throw new IllegalArgumentException("MALFORMED_MEDIA_ARCHIVE",ex);}
        finally {Arrays.fill(plain,(byte)0);}
        // Source IDs are not safe to reuse, and an unmapped owner must never
        // silently become the target family's owner.
        for(JsonNode asset:cats.path("MEDIA_ASSETS")) {
            if(asset.path("workspace_id").asLong()!=source ||
                    !memberIds.containsKey(asset.path("created_by_member_id").asLong()))
                throw new SecurityException("UNMAPPED_MEDIA_OWNER");
        }
        for(JsonNode binding:cats.path("MEDIA_BINDINGS")) {
            if(binding.path("workspace_id").asLong()!=source ||
                    !"MEMBER_GROUP".equals(binding.path("target_type").asText()) ||
                    !groupIds.containsKey(binding.path("target_id").asLong()) ||
                    !binding.hasNonNull("asset_id") || binding.hasNonNull("catalog_key"))
                throw new IllegalArgumentException("PRIVATE_MEDIA_BINDING_UNSUPPORTED");
        }
        return tx.execute(status->{
            Long targetExists=db.queryForObject("SELECT id FROM families WHERE id=? FOR UPDATE",
                    Long.class,target);
            if(targetExists==null)throw new IllegalArgumentException("TARGET_MISSING");
            Long epoch=db.queryForObject("SELECT data_epoch FROM families WHERE id=?",Long.class,target);
            if(epoch==null || epoch!=0L)throw new IllegalStateException("TARGET_EPOCH_STALE");
            if(!Objects.equals(0L,db.queryForObject(
                    "SELECT count(*) FROM media_assets WHERE workspace_id=?",Long.class,target)))
                throw new IllegalStateException("TARGET_MEDIA_NOT_EMPTY");
            for(long targetMember:memberIds.values()) {
                if(!Objects.equals(1L,db.queryForObject(
                        "SELECT count(*) FROM family_members WHERE id=? AND family_id=?",
                        Long.class,targetMember,target)))
                    throw new SecurityException("TARGET_MEDIA_OWNER_NOT_IN_FAMILY");
            }
            for(long targetGroup:groupIds.values()) {
                if(!Objects.equals(1L,db.queryForObject(
                        "SELECT count(*) FROM member_groups WHERE id=? AND family_id=?",
                        Long.class,targetGroup,target)))
                    throw new SecurityException("TARGET_GROUP_NOT_IN_FAMILY");
            }
            var ids=new TreeMap<UUID,UUID>();
            int blobs=0,bindings=0;
            for(JsonNode asset:cats.path("MEDIA_ASSETS")) {
                UUID oldId=UUID.fromString(asset.path("id").asText());
                UUID newId=UUID.randomUUID();
                Long owner=memberIds.get(asset.path("created_by_member_id").asLong());
                db.update("""
                        INSERT INTO media_assets(id,workspace_id,mime_type,width,height,byte_size,sha256,created_by_member_id)
                        VALUES (?,?,?,?,?,?,?,?)
                        """,newId,target,asset.path("mime_type").asText(),asset.path("width").asInt(),
                        asset.path("height").asInt(),asset.path("byte_size").asInt(),
                        asset.path("sha256").asText(),owner);
                ids.put(oldId,newId);
                int originalVariantBytes=0;
                for(JsonNode variant:asset.path("blobs")) {
                    byte[] bytes=Base64.getDecoder().decode(variant.path("bytesBase64").asText());
                    try {
                        if(bytes.length==0 || bytes.length>5_242_880)
                            throw new IllegalArgumentException("PRIVATE_MEDIA_INVALID_BLOB_SIZE");
                        originalVariantBytes+=bytes.length;
                        db.update("INSERT INTO media_blobs(asset_id,variant,bytes) VALUES (?,?,?)",
                                newId,variant.path("variant").asText(),bytes);
                        byte[] retrieved=db.queryForObject(
                                "SELECT bytes FROM media_blobs WHERE asset_id=? AND variant=?",
                                byte[].class,newId,variant.path("variant").asText());
                        if(!Arrays.equals(bytes,retrieved))
                            throw new IllegalStateException("PRIVATE_MEDIA_BYTES_CHANGED");
                        blobs++;
                    } finally {Arrays.fill(bytes,(byte)0);}
                }
                if(originalVariantBytes==0)
                    throw new IllegalArgumentException("PRIVATE_MEDIA_MISSING_BINARY");
                Long persistedBlobs=db.queryForObject(
                        "SELECT count(*) FROM media_blobs WHERE asset_id=?",Long.class,newId);
                if(persistedBlobs==null ||
                        persistedBlobs.longValue()!=asset.path("blobs").size())
                    throw new IllegalStateException("PRIVATE_MEDIA_BLOB_COUNT_MISMATCH");
            }
            for(JsonNode binding:cats.path("MEDIA_BINDINGS")) {
                UUID original=UUID.fromString(binding.path("asset_id").asText());
                UUID assetId=ids.get(original);
                if(assetId==null)throw new IllegalArgumentException("PRIVATE_MEDIA_DANGLING_ASSET");
                db.update("""
                        INSERT INTO media_bindings(workspace_id,target_type,target_id,slot,asset_id)
                        VALUES (?,'MEMBER_GROUP',?,?,?)
                        """,target,groupIds.get(binding.path("target_id").asLong()),
                        binding.path("slot").asText(),assetId);
                bindings++;
            }
            if(ids.size()!=inspection.counts().get("MEDIA_ASSETS")
                    || bindings!=inspection.counts().get("MEDIA_BINDINGS"))
                throw new IllegalStateException("PRIVATE_MEDIA_COUNT_MISMATCH");
            return new Result(Map.copyOf(ids),blobs,bindings);
        });
    }
}
