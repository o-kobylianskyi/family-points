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
 * Disposable database TEST ONLY. Replays a bounded reward category and link
 * subgraph into a fresh target family whose reward definitions were independently
 * reconstructed. No Spring bean, HTTP endpoint, VERIFIED or reset integration.
 */
final class DisposableRewardCategoryRestoreExecutor {
    record Result(Map<Long,Long> categoryIds,int links,UUID snapshotId) {}

    private final JdbcTemplate db;
    private final TransactionTemplate tx;
    private final ProtectiveBackupImportInspector inspector;
    private final ProtectiveBackupPortableFile portable;
    private final ProtectiveBackupArtifactCodec codec;
    private final ObjectMapper json=new ObjectMapper();

    DisposableRewardCategoryRestoreExecutor(JdbcTemplate db,PlatformTransactionManager manager,
            ProtectiveBackupImportInspector inspector,ProtectiveBackupPortableFile portable,
            ProtectiveBackupArtifactCodec codec) {
        this.db=db;
        this.tx=new TransactionTemplate(manager);
        this.inspector=inspector;
        this.portable=portable;
        this.codec=codec;
    }

    Result apply(byte[] file,long source,long target,
            Map<Long,Long> definitionIds,SecretKey key) {
        if(file==null || source<=0 || target<=0 || source==target
                || definitionIds==null || key==null)
            throw new IllegalArgumentException("INVALID_CATEGORY_REPLAY_REQUEST");
        var inspection=inspector.inspect(file,source,key);
        if(!"V50".equals(inspection.schemaVersion())
                || !inspection.availableCategories().equals(List.of(
                    "REWARD_CATEGORIES","REWARD_DEFINITIONS",
                    "REWARD_DEFINITION_CATEGORIES")))
            throw new IllegalArgumentException("CATEGORY_REPLAY_SCOPE_UNSUPPORTED");

        var envelope=portable.decode(file);
        byte[] raw=codec.open(envelope,inspection.snapshotId(),source,key);
        final JsonNode categories,links,definitions;
        try {
            var root=json.readTree(raw).path("categories");
            categories=root.path("REWARD_CATEGORIES");
            links=root.path("REWARD_DEFINITION_CATEGORIES");
            definitions=root.path("REWARD_DEFINITIONS");
        }catch(Exception ex) {
            throw new IllegalArgumentException("CATEGORY_REPLAY_INVALID_ARCHIVE",ex);
        }finally {Arrays.fill(raw,(byte)0);}

        var sourceCategoryIds=new HashSet<Long>();
        var sourceDefinitionIds=new HashSet<Long>();
        for(JsonNode category:categories) {
            long id=category.path("id").asLong();
            if(id<=0 || category.path("family_id").asLong()!=source
                    || !sourceCategoryIds.add(id)
                    || !category.path("name").isTextual()
                    || category.path("name").asText().isBlank()
                    || category.path("name").asText().length()>100)
                throw new SecurityException("CATEGORY_SOURCE_INVALID");
        }
        for(JsonNode definition:definitions) {
            long id=definition.path("id").asLong();
            if(id<=0 || definition.path("family_id").asLong()!=source
                    || !sourceDefinitionIds.add(id))
                throw new SecurityException("CATEGORY_DEFINITION_SOURCE_INVALID");
        }
        if(!definitionIds.keySet().equals(sourceDefinitionIds)
                || definitionIds.containsValue(null)
                || new HashSet<>(definitionIds.values()).size()!=definitionIds.size())
            throw new SecurityException("CATEGORY_DEFINITION_REMAP_INCOMPLETE");
        var linkKeys=new HashSet<String>();
        for(JsonNode link:links) {
            long oldDefinition=link.path("reward_definition_id").asLong();
            long oldCategory=link.path("category_id").asLong();
            if(!sourceDefinitionIds.contains(oldDefinition)
                    || !sourceCategoryIds.contains(oldCategory)
                    || !linkKeys.add(oldDefinition+":"+oldCategory))
                throw new SecurityException("CATEGORY_LINK_SOURCE_INVALID");
        }

        return tx.execute(status->{
            Long locked=db.queryForObject("SELECT id FROM families WHERE id=? FOR UPDATE",
                    Long.class,target);
            if(locked==null)throw new SecurityException("CATEGORY_TARGET_MISSING");
            Long epoch=db.queryForObject("SELECT data_epoch FROM families WHERE id=?",
                    Long.class,target);
            if(!Objects.equals(0L,epoch))
                throw new IllegalStateException("CATEGORY_TARGET_EPOCH_NOT_FRESH");
            Long existing=db.queryForObject("SELECT count(*) FROM reward_categories WHERE family_id=?",
                    Long.class,target);
            if(!Objects.equals(0L,existing))
                throw new IllegalStateException("CATEGORY_TARGET_NOT_EMPTY");
            for(long id:definitionIds.values()) {
                Long belongs=db.queryForObject(
                        "SELECT count(*) FROM reward_definitions WHERE id=? AND family_id=?",
                        Long.class,id,target);
                if(!Objects.equals(1L,belongs))
                    throw new SecurityException("CATEGORY_DEFINITION_TARGET_FOREIGN");
                Long oldLinks=db.queryForObject("""
                        SELECT count(*) FROM reward_definition_categories
                        WHERE reward_definition_id=?
                        """,Long.class,id);
                if(!Objects.equals(0L,oldLinks))
                    throw new IllegalStateException("CATEGORY_DEFINITION_LINKS_NOT_EMPTY");
            }
            var remapped=new TreeMap<Long,Long>();
            for(JsonNode category:categories) {
                long oldId=category.path("id").asLong();
                Long newId=db.queryForObject("""
                        INSERT INTO reward_categories(family_id,name,active,sort_order)
                        VALUES (?,?,?,?) RETURNING id
                        """,Long.class,target,category.path("name").asText(),
                        category.path("active").asBoolean(true),
                        category.path("sort_order").asInt(0));
                if(newId==null || newId.equals(oldId))
                    throw new IllegalStateException("CATEGORY_PK_REMAP_FAILED");
                remapped.put(oldId,newId);
            }
            for(JsonNode link:links) {
                db.update("""
                        INSERT INTO reward_definition_categories(reward_definition_id,category_id)
                        VALUES (?,?)
                        """,definitionIds.get(link.path("reward_definition_id").asLong()),
                        remapped.get(link.path("category_id").asLong()));
            }
            Long restored=db.queryForObject("""
                    SELECT count(*) FROM reward_definition_categories l
                    JOIN reward_definitions d ON d.id=l.reward_definition_id
                    WHERE d.family_id=?
                    """,Long.class,target);
            if(restored==null || restored.intValue()!=links.size())
                throw new IllegalStateException("CATEGORY_LINK_COUNT_MISMATCH");
            return new Result(Map.copyOf(remapped),links.size(),inspection.snapshotId());
        });
    }
}
