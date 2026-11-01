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
 * Disposable Testcontainers-only native SQL restore executor.
 * Deliberately located under src/test: cannot be invoked by production.
 * Only MEMBER_GROUPS and catalog-backed MEMBER_GROUP MEDIA_BINDINGS.
 */
final class DisposableNativeRestoreExecutor {
    private final JdbcTemplate db;
    private final TransactionTemplate tx;
    private final ProtectiveBackupPortableFile portable;
    private final ProtectiveBackupArtifactCodec codec;
    private final ProtectiveBackupImportInspector inspector;
    private final ObjectMapper json=new ObjectMapper();
    record Result(Map<Long,Long> groupIds,int bindings,UUID snapshotId) {}

    DisposableNativeRestoreExecutor(JdbcTemplate db,PlatformTransactionManager manager,
            ProtectiveBackupPortableFile portable,ProtectiveBackupArtifactCodec codec,
            ProtectiveBackupImportInspector inspector) {
        this.db=db;this.tx=new TransactionTemplate(manager);
        this.portable=portable;this.codec=codec;this.inspector=inspector;
    }

    Result apply(byte[] file,long source,long target,SecretKey key) {
        if(source<=0 || target<=0 || source==target)
            throw new IllegalArgumentException("DISPOSABLE_DISTINCT_FAMILIES_REQUIRED");
        var proof=inspector.inspect(file,source,key);
        if(!proof.availableCategories().equals(List.of("MEDIA_BINDINGS","MEMBER_GROUPS")))
            throw new IllegalArgumentException("DISPOSABLE_CATEGORY_SCOPE_UNSUPPORTED");
        var envelope=portable.decode(file);
        byte[] plain=codec.open(envelope,envelope.manifest().snapshotId(),source,key);
        final JsonNode categories;
        try {categories=json.readTree(plain).path("categories");}
        catch(Exception ex){throw new IllegalArgumentException("INVALID_PORTABLE_ARCHIVE",ex);}
        finally {Arrays.fill(plain,(byte)0);}
        // Preflight every binding before opening a write transaction.
        var groupIds=new HashSet<Long>();
        for(JsonNode group:categories.path("MEMBER_GROUPS")) {
            if(group.path("family_id").asLong()!=source ||
                    !groupIds.add(group.path("id").asLong()))
                throw new SecurityException("INVALID_ARCHIVED_GROUP_OWNER");
        }
        for(JsonNode binding:categories.path("MEDIA_BINDINGS")) {
            if(binding.path("workspace_id").asLong()!=source ||
                    !"MEMBER_GROUP".equals(binding.path("target_type").asText()) ||
                    !groupIds.contains(binding.path("target_id").asLong()) ||
                    binding.hasNonNull("asset_id") ||
                    !binding.path("catalog_key").isTextual())
                throw new IllegalArgumentException("UNSAFE_ARCHIVED_BINDING");
        }
        return tx.execute(status->{
            // Transactional, target-scoped, conflict-safe. This is not used by
            // any main-source production service or HTTP controller.
            Long targetExists=db.queryForObject("SELECT count(*) FROM families WHERE id=?",
                    Long.class,target);
            if(targetExists==null || targetExists!=1)throw new IllegalArgumentException("TARGET_MISSING");
            Long targetGroups=db.queryForObject("SELECT count(*) FROM member_groups WHERE family_id=?",
                    Long.class,target);
            Long targetBindings=db.queryForObject("SELECT count(*) FROM media_bindings WHERE workspace_id=?",
                    Long.class,target);
            if(targetGroups==null || targetGroups>0 || targetBindings==null || targetBindings>0)
                throw new IllegalStateException("TARGET_NOT_EMPTY");
            var remap=new TreeMap<Long,Long>();
            for(JsonNode group:categories.path("MEMBER_GROUPS")) {
                Long next=db.queryForObject("""
                        INSERT INTO member_groups(family_id,name,active,show_in_navigation)
                        VALUES (?,?,?,?) RETURNING id
                        """,Long.class,target,group.path("name").asText(),
                        group.path("active").asBoolean(),
                        group.path("show_in_navigation").asBoolean());
                if(next==null || next==group.path("id").asLong())
                    throw new IllegalStateException("GROUP_ID_REMAP_FAILED");
                remap.put(group.path("id").asLong(),next);
            }
            int bindings=0;
            for(JsonNode binding:categories.path("MEDIA_BINDINGS")) {
                db.update("""
                        INSERT INTO media_bindings(workspace_id,target_type,target_id,slot,catalog_key)
                        VALUES (?,'MEMBER_GROUP',?,?,?)
                        """,target,remap.get(binding.path("target_id").asLong()),
                        binding.path("slot").asText(),binding.path("catalog_key").asText());
                bindings++;
            }
            Long groupsAfter=db.queryForObject("SELECT count(*) FROM member_groups WHERE family_id=?",
                    Long.class,target);
            Long bindingsAfter=db.queryForObject("SELECT count(*) FROM media_bindings WHERE workspace_id=?",
                    Long.class,target);
            if(groupsAfter==null || groupsAfter.longValue()!=proof.counts().get("MEMBER_GROUPS")
                    || bindingsAfter==null || bindingsAfter.longValue()!=proof.counts().get("MEDIA_BINDINGS"))
                throw new IllegalStateException("NATIVE_RESTORE_COUNT_MISMATCH");
            return new Result(Map.copyOf(remap),bindings,proof.snapshotId());
        });
    }
}
