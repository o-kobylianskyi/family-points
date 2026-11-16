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
 * Test-only disposable PostgreSQL replay of reward obligations and their
 * blocked category/reward links. Required parents were restored separately.
 * Never wire into Spring, HTTP, production VERIFIED or Reset Execute.
 */
final class DisposableRewardObligationRestoreExecutor {
    record Result(Map<Long,Long> obligationIds,int blockedCategories,int blockedRewards,
                  UUID snapshotId) {}
    private final JdbcTemplate db;
    private final TransactionTemplate tx;
    private final ProtectiveBackupArtifactCodec codec;
    private final ProtectiveBackupPortableFile portable;
    private final ProtectiveBackupImportInspector inspector;
    private final ObjectMapper mapper=new ObjectMapper();

    DisposableRewardObligationRestoreExecutor(JdbcTemplate db,
            PlatformTransactionManager manager,ProtectiveBackupArtifactCodec codec,
            ProtectiveBackupPortableFile portable,ProtectiveBackupImportInspector inspector) {
        this.db=db;this.tx=new TransactionTemplate(manager);
        this.codec=codec;this.portable=portable;this.inspector=inspector;
    }

    Result apply(byte[] archive,long source,long target,
            Map<Long,Long> members,Map<Long,Long> purchases,
            Map<Long,Long> requirements,Map<Long,Long> categories,
            Map<Long,Long> rewards,SecretKey key) {
        if(archive==null || source<=0 || target<=0 || source==target || key==null
                || members==null || purchases==null || requirements==null
                || categories==null || rewards==null)
            throw new IllegalArgumentException("OBLIGATION_REPLAY_INVALID_REQUEST");
        var inspection=inspector.inspect(archive,source,key);
        if(!"V50".equals(inspection.schemaVersion()))
            throw new IllegalArgumentException("OBLIGATION_REPLAY_SCHEMA_UNSUPPORTED");
        byte[] bytes=codec.open(portable.decode(archive),inspection.snapshotId(),source,key);
        final JsonNode root;
        try {root=mapper.readTree(bytes).path("categories");}
        catch(java.io.IOException ex) {
            throw new IllegalArgumentException("OBLIGATION_REPLAY_INVALID_ARCHIVE",ex);
        }finally {Arrays.fill(bytes,(byte)0);}
        var supported=Set.of("REWARD_OBLIGATIONS",
                "REWARD_OBLIGATION_BLOCKED_CATEGORIES","REWARD_OBLIGATION_BLOCKED_REWARDS",
                "REWARD_PURCHASES","REWARD_REQUIREMENTS","REWARD_CATEGORIES",
                "REWARD_DEFINITIONS");
        if(!root.has("REWARD_OBLIGATIONS") ||
                inspection.availableCategories().stream().anyMatch(x->!supported.contains(x)))
            throw new IllegalArgumentException("OBLIGATION_REPLAY_SCOPE_UNSUPPORTED");
        var obligations=root.path("REWARD_OBLIGATIONS");
        var blockedCategories=root.path("REWARD_OBLIGATION_BLOCKED_CATEGORIES");
        var blockedRewards=root.path("REWARD_OBLIGATION_BLOCKED_REWARDS");
        var oldIds=new HashSet<Long>();
        var oldMembers=new HashSet<Long>();
        var oldPurchases=new HashSet<Long>();
        var oldRequirements=new HashSet<Long>();
        var oldCategories=new HashSet<Long>();
        var oldRewards=new HashSet<Long>();
        for(JsonNode row:obligations) {
            if(!row.path("id").isIntegralNumber() || !row.path("member_id").isIntegralNumber()
                    || !row.path("reward_purchase_id").isIntegralNumber()
                    || !row.path("family_id").isIntegralNumber()
                    || row.path("family_id").asLong()!=source
                    || !row.path("title").isTextual() || !row.path("status").isTextual()
                    || !row.path("blocking_mode").isTextual()
                    || row.path("id").asLong()<=0 || !oldIds.add(row.path("id").asLong()))
                throw new SecurityException("OBLIGATION_SOURCE_ROW_INVALID");
            oldMembers.add(row.path("member_id").asLong());
            oldPurchases.add(row.path("reward_purchase_id").asLong());
            if(row.hasNonNull("requirement_id")) {
                if(!row.path("requirement_id").isIntegralNumber()
                        || row.path("requirement_id").asLong()<=0)
                    throw new SecurityException("OBLIGATION_REQUIREMENT_ID_INVALID");
                oldRequirements.add(row.path("requirement_id").asLong());
            }
        }
        for(JsonNode row:blockedCategories) {
            long id=row.path("obligation_id").asLong(),category=row.path("category_id").asLong();
            if(!oldIds.contains(id) || category<=0)
                throw new SecurityException("OBLIGATION_BLOCK_CATEGORY_SOURCE_INVALID");
            oldCategories.add(category);
        }
        for(JsonNode row:blockedRewards) {
            long id=row.path("obligation_id").asLong(),reward=row.path("reward_definition_id").asLong();
            if(!oldIds.contains(id) || reward<=0)
                throw new SecurityException("OBLIGATION_BLOCK_REWARD_SOURCE_INVALID");
            oldRewards.add(reward);
        }
        requireExactMap(members,oldMembers,"MEMBERS");
        requireExactMap(purchases,oldPurchases,"PURCHASES");
        requireExactMap(requirements,oldRequirements,"REQUIREMENTS");
        requireExactMap(categories,oldCategories,"CATEGORIES");
        requireExactMap(rewards,oldRewards,"REWARDS");
        requireParents(root,"REWARD_PURCHASES",oldPurchases);
        requireParents(root,"REWARD_REQUIREMENTS",oldRequirements);
        requireParents(root,"REWARD_CATEGORIES",oldCategories);
        requireParents(root,"REWARD_DEFINITIONS",oldRewards);

        return tx.execute(status->{
            Long locked=db.queryForObject("SELECT id FROM families WHERE id=? FOR UPDATE",
                    Long.class,target);
            if(locked==null)throw new SecurityException("OBLIGATION_TARGET_NOT_FOUND");
            Long epoch=db.queryForObject("SELECT data_epoch FROM families WHERE id=?",
                    Long.class,target);
            if(!Objects.equals(0L,epoch))
                throw new SecurityException("OBLIGATION_TARGET_EPOCH_STALE");
            Long existing=db.queryForObject("SELECT count(*) FROM reward_obligations WHERE family_id=?",
                    Long.class,target);
            if(!Objects.equals(0L,existing))
                throw new IllegalStateException("OBLIGATION_TARGET_NOT_EMPTY");
            for(Long member:members.values()) {
                Long count=db.queryForObject(
                        "SELECT count(*) FROM family_members WHERE id=? AND family_id=?",
                        Long.class,member,target);
                if(!Objects.equals(1L,count))
                    throw new SecurityException("OBLIGATION_TARGET_FOREIGN_MEMBER");
            }
            for(Long purchase:purchases.values()) {
                Long count=db.queryForObject("""
                        SELECT count(*) FROM reward_purchases p
                        JOIN family_members m ON m.id=p.member_id
                        WHERE p.id=? AND m.family_id=?
                        """,Long.class,purchase,target);
                if(!Objects.equals(1L,count))
                    throw new SecurityException("OBLIGATION_TARGET_FOREIGN_PURCHASE");
            }
            validateParents(requirements,"reward_requirements",target);
            validateParents(categories,"reward_categories",target);
            validateParents(rewards,"reward_definitions",target);
            var remapped=new TreeMap<Long,Long>();
            for(JsonNode row:obligations) {
                Long requirementId=row.hasNonNull("requirement_id")
                        ?requirements.get(row.path("requirement_id").asLong()):null;
                Long newId=db.queryForObject("""
                        INSERT INTO reward_obligations(
                          family_id,member_id,reward_purchase_id,requirement_id,
                          title,status,blocking_mode,due_at,created_at,resolved_at)
                        VALUES (?,?,?,?,?,?,?,CAST(? AS timestamp),
                                CAST(? AS timestamp),CAST(? AS timestamp)) RETURNING id
                        """,Long.class,target,members.get(row.path("member_id").asLong()),
                        purchases.get(row.path("reward_purchase_id").asLong()),requirementId,
                        row.path("title").asText(),row.path("status").asText(),
                        row.path("blocking_mode").asText(),
                        timestamp(row,"due_at"),timestamp(row,"created_at"),
                        timestamp(row,"resolved_at"));
                if(newId==null || newId<=0)throw new IllegalStateException("OBLIGATION_PK_MISSING");
                remapped.put(row.path("id").asLong(),newId);
            }
            for(JsonNode link:blockedCategories) {
                db.update("""
                        INSERT INTO reward_obligation_blocked_categories(obligation_id,category_id)
                        VALUES (?,?)
                        """,remapped.get(link.path("obligation_id").asLong()),
                        categories.get(link.path("category_id").asLong()));
            }
            for(JsonNode link:blockedRewards) {
                db.update("""
                        INSERT INTO reward_obligation_blocked_rewards(obligation_id,reward_definition_id)
                        VALUES (?,?)
                        """,remapped.get(link.path("obligation_id").asLong()),
                        rewards.get(link.path("reward_definition_id").asLong()));
            }
            Long count=db.queryForObject("SELECT count(*) FROM reward_obligations WHERE family_id=?",
                    Long.class,target);
            Long catCount=db.queryForObject("""
                    SELECT count(*) FROM reward_obligation_blocked_categories b
                    JOIN reward_obligations o ON o.id=b.obligation_id WHERE o.family_id=?
                    """,Long.class,target);
            Long rewardCount=db.queryForObject("""
                    SELECT count(*) FROM reward_obligation_blocked_rewards b
                    JOIN reward_obligations o ON o.id=b.obligation_id WHERE o.family_id=?
                    """,Long.class,target);
            if(count==null || count.intValue()!=obligations.size()
                    || catCount==null || catCount.intValue()!=blockedCategories.size()
                    || rewardCount==null || rewardCount.intValue()!=blockedRewards.size())
                throw new IllegalStateException("OBLIGATION_GRAPH_REPLAY_COUNT_MISMATCH");
            return new Result(Map.copyOf(remapped),blockedCategories.size(),
                    blockedRewards.size(),inspection.snapshotId());
        });
    }

    private static String timestamp(JsonNode row,String field) {
        if(!row.hasNonNull(field))return null;
        if(!row.path(field).isTextual())throw new SecurityException("OBLIGATION_BAD_TIMESTAMP");
        return row.path(field).asText();
    }
    private static void requireExactMap(Map<Long,Long> map,Set<Long> old,String name) {
        if(!map.keySet().equals(old) || map.values().stream().anyMatch(x->x==null || x<=0)
                || new HashSet<>(map.values()).size()!=map.size())
            throw new SecurityException("OBLIGATION_"+name+"_MAP_INVALID");
    }
    private static void requireParents(JsonNode root,String category,Set<Long> ids) {
        if(ids.isEmpty())return;
        if(!root.path(category).isArray())
            throw new SecurityException("OBLIGATION_PARENT_CATEGORY_MISSING:"+category);
        var present=new HashSet<Long>();
        for(JsonNode row:root.path(category)) {
            if(!row.path("id").isIntegralNumber() || row.path("id").asLong()<=0)
                throw new SecurityException("OBLIGATION_PARENT_ID_INVALID");
            present.add(row.path("id").asLong());
        }
        if(!present.containsAll(ids))
            throw new SecurityException("OBLIGATION_PARENT_FK_DANGLING:"+category);
    }
    private void validateParents(Map<Long,Long> map,String table,long target) {
        for(Long id:map.values()) {
            Long count=db.queryForObject(
                    "SELECT count(*) FROM "+table+" WHERE id=? AND family_id=?",
                    Long.class,id,target);
            if(!Objects.equals(1L,count))
                throw new SecurityException("OBLIGATION_TARGET_FOREIGN_PARENT:"+table);
        }
    }
}
