package com.olehkobylianskyi.familypoints;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.olehkobylianskyi.familypoints.service.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.crypto.SecretKey;
import java.util.*;

/** TEST ONLY: disposable target PG, never exposed by production Spring or HTTP. */
final class DisposableRewardRequirementRestoreExecutor {
    record Result(Map<Long,Long> ids, UUID snapshotId) {}
    private final JdbcTemplate db;
    private final TransactionTemplate tx;
    private final ProtectiveBackupImportInspector inspector;
    private final ProtectiveBackupArtifactCodec codec;
    private final ProtectiveBackupPortableFile portable;
    private final ObjectMapper mapper=new ObjectMapper();

    DisposableRewardRequirementRestoreExecutor(JdbcTemplate db,PlatformTransactionManager manager,
            ProtectiveBackupImportInspector inspector,ProtectiveBackupArtifactCodec codec,
            ProtectiveBackupPortableFile portable) {
        this.db=db;this.tx=new TransactionTemplate(manager);this.inspector=inspector;
        this.codec=codec;this.portable=portable;
    }
    Result apply(byte[] archive,long source,long target,Map<Long,Long> definitions,
            Map<Long,Long> requests,Map<Long,Long> tasks,SecretKey key) {
        if(archive==null || source<=0 || target<=0 || source==target
                || definitions==null || requests==null || tasks==null || key==null)
            throw new IllegalArgumentException("REQUIREMENT_REPLAY_INVALID_REQUEST");
        var inspection=inspector.inspect(archive,source,key);
        byte[] clear=codec.open(portable.decode(archive),inspection.snapshotId(),source,key);
        final JsonNode root,rows;
        try {
            root=mapper.readTree(clear).path("categories");
            rows=root.path("REWARD_REQUIREMENTS");
            if(!rows.isArray() || inspection.availableCategories().stream().anyMatch(category->
                    !Set.of("REWARD_REQUIREMENTS","REWARD_DEFINITIONS",
                            "REWARD_REQUESTS","TASK_DEFINITIONS").contains(category)))
                throw new IllegalArgumentException("REQUIREMENT_REPLAY_UNSUPPORTED_SCOPE");
        }catch(java.io.IOException ex) {
            throw new IllegalArgumentException("REQUIREMENT_REPLAY_INVALID_ARCHIVE",ex);
        }finally {Arrays.fill(clear,(byte)0);}
        var used=new HashSet<Long>();
        var defs=new HashSet<Long>();var reqs=new HashSet<Long>();var taskIds=new HashSet<Long>();
        for(JsonNode row:rows) {
            long id=row.path("id").asLong();
            boolean d=row.hasNonNull("reward_definition_id"),q=row.hasNonNull("reward_request_id");
            if(id<=0 || !used.add(id) || row.path("family_id").asLong()!=source
                    || d==q || !row.path("phase").isTextual()
                    || !row.path("requirement_type").isTextual())
                throw new SecurityException("REQUIREMENT_REPLAY_INVALID_SOURCE");
            if(d)defs.add(row.path("reward_definition_id").asLong());
            if(q)reqs.add(row.path("reward_request_id").asLong());
            if(row.hasNonNull("task_definition_id"))taskIds.add(row.path("task_definition_id").asLong());
        }
        validateMapping(definitions,defs,"DEFINITION");
        validateMapping(requests,reqs,"REQUEST");
        validateMapping(tasks,taskIds,"TASK");
        checkArchivedParents(root,"REWARD_DEFINITIONS",defs);
        checkArchivedParents(root,"REWARD_REQUESTS",reqs);
        checkArchivedParents(root,"TASK_DEFINITIONS",taskIds);
        return tx.execute(status->{
            Long locked=db.queryForObject("SELECT id FROM families WHERE id=? FOR UPDATE",
                    Long.class,target);
            if(locked==null)throw new SecurityException("REQUIREMENT_TARGET_NOT_FOUND");
            Long epoch=db.queryForObject("SELECT data_epoch FROM families WHERE id=?",
                    Long.class,target);
            if(!Objects.equals(0L,epoch))
                throw new IllegalStateException("REQUIREMENT_TARGET_EPOCH_STALE");
            Long existing=db.queryForObject("SELECT count(*) FROM reward_requirements WHERE family_id=?",
                    Long.class,target);
            if(!Objects.equals(0L,existing))
                throw new IllegalStateException("REQUIREMENT_TARGET_NOT_EMPTY");
            validateTarget(definitions,"reward_definitions",target);
            validateTarget(requests,"reward_requests",target);
            validateTarget(tasks,"task_definitions",target);
            var result=new TreeMap<Long,Long>();
            for(JsonNode row:rows) {
                Long sourceDef=nullableId(row,"reward_definition_id");
                Long sourceReq=nullableId(row,"reward_request_id");
                Long sourceTask=nullableId(row,"task_definition_id");
                Long newId=db.queryForObject("""
                        INSERT INTO reward_requirements
                          (family_id,reward_definition_id,reward_request_id,phase,
                           requirement_type,task_definition_id,time_scope,
                           window_value,description,required,sort_order)
                        VALUES (?,?,?,?,?,?,?,?,?,?,?) RETURNING id
                        """,Long.class,target,
                        sourceDef==null?null:definitions.get(sourceDef),
                        sourceReq==null?null:requests.get(sourceReq),
                        row.path("phase").asText(),row.path("requirement_type").asText(),
                        sourceTask==null?null:tasks.get(sourceTask),
                        row.hasNonNull("time_scope")?row.path("time_scope").asText():null,
                        row.hasNonNull("window_value")?row.path("window_value").asInt():null,
                        row.hasNonNull("description")?row.path("description").asText():null,
                        row.path("required").asBoolean(true),row.path("sort_order").asInt(0));
                if(newId==null || newId.equals(row.path("id").asLong()))
                    throw new IllegalStateException("REQUIREMENT_PK_REMAP_INVALID");
                result.put(row.path("id").asLong(),newId);
            }
            Long count=db.queryForObject("SELECT count(*) FROM reward_requirements WHERE family_id=?",
                    Long.class,target);
            if(count==null || count.intValue()!=rows.size())
                throw new IllegalStateException("REQUIREMENT_REPLAY_COUNT_MISMATCH");
            return new Result(Map.copyOf(result),inspection.snapshotId());
        });
    }
    private static Long nullableId(JsonNode row,String key) {
        return row.hasNonNull(key)?row.path(key).asLong():null;
    }
    private static void validateMapping(Map<Long,Long> mapping,Set<Long> needed,String label) {
        if(!mapping.keySet().equals(needed)
                || mapping.values().stream().anyMatch(v->v==null || v<=0)
                || new HashSet<>(mapping.values()).size()!=mapping.size())
            throw new SecurityException("REQUIREMENT_"+label+"_REMAP_INVALID");
    }
    private static void checkArchivedParents(JsonNode root,String category,Set<Long> needed) {
        if(needed.isEmpty())return;
        if(!root.has(category))
            throw new SecurityException("REQUIREMENT_SOURCE_PARENT_MISSING:"+category);
        var ids=new HashSet<Long>();
        for(JsonNode row:root.path(category))ids.add(row.path("id").asLong());
        if(!ids.containsAll(needed))
            throw new SecurityException("REQUIREMENT_SOURCE_PARENT_DANGLING:"+category);
    }
    private void validateTarget(Map<Long,Long> mapping,String table,long family) {
        for(Long id:mapping.values()) {
            Long count=db.queryForObject(
                    "SELECT count(*) FROM "+table+" WHERE id=? AND family_id=?",
                    Long.class,id,family);
            if(!Objects.equals(1L,count))
                throw new SecurityException("REQUIREMENT_TARGET_FOREIGN_PARENT:"+table);
        }
    }
}
