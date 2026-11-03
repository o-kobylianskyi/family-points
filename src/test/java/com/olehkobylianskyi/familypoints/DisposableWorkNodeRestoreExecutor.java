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
 * Test-only restoration of hierarchical work folders and TASK nodes into
 * disposable PostgreSQL. Does not reconstruct TaskDefinition: linked TASK
 * nodes require a later, separately approved task FK reconstruction.
 */
final class DisposableWorkNodeRestoreExecutor {
    record Result(Map<Long,Long> nodeIds,int restored) {}
    private final JdbcTemplate db;
    private final TransactionTemplate tx;
    private final ProtectiveBackupPortableFile portable;
    private final ProtectiveBackupArtifactCodec codec;
    private final ProtectiveBackupImportInspector inspector;
    private final ObjectMapper json=new ObjectMapper();

    DisposableWorkNodeRestoreExecutor(JdbcTemplate db,PlatformTransactionManager manager,
            ProtectiveBackupPortableFile portable,ProtectiveBackupArtifactCodec codec,
            ProtectiveBackupImportInspector inspector) {
        this.db=db;this.tx=new TransactionTemplate(manager);
        this.portable=portable;this.codec=codec;this.inspector=inspector;
    }

    Result apply(byte[] file,long source,long target,SecretKey key) {
        if(source<=0 || target<=0 || source==target)
            throw new IllegalArgumentException("DISPOSABLE_DISTINCT_FAMILIES_REQUIRED");
        var inspection=inspector.inspect(file,source,key);
        if(!inspection.availableCategories().equals(List.of("WORK_NODES"))
                || !"V50".equals(inspection.schemaVersion()))
            throw new IllegalArgumentException("WORK_NODE_SCOPE_UNSUPPORTED");
        var envelope=portable.decode(file);
        byte[] clear=codec.open(envelope,envelope.manifest().snapshotId(),source,key);
        final JsonNode rows;
        try {rows=json.readTree(clear).path("categories").path("WORK_NODES");}
        catch(Exception ex){throw new IllegalArgumentException("INVALID_WORK_NODE_ARCHIVE",ex);}
        finally {Arrays.fill(clear,(byte)0);}
        Map<Long,JsonNode> nodes=new TreeMap<>();
        for(JsonNode row:rows) {
            if(!row.path("id").isIntegralNumber() || row.path("id").asLong()<=0
                    || !row.path("family_id").isIntegralNumber() || row.path("family_id").asLong()!=source
                    || !row.path("title").isTextual() || row.path("title").asText().isBlank()
                    || row.path("title").asText().length()>150
                    || !row.path("type").isTextual()
                    || !Set.of("GROUP","TASK").contains(row.path("type").asText())
                    || row.path("sort_order").isMissingNode()
                    || !row.path("sort_order").isIntegralNumber()
                    || !row.path("active").isBoolean()
                    || nodes.putIfAbsent(row.path("id").asLong(),row)!=null)
                throw new IllegalArgumentException("INVALID_ARCHIVED_WORK_NODE");
        }
        // A TASK WorkNode without its TaskDefinition is not a complete
        // recoverable unit. Never insert an orphan and call that FULL restore.
        for(JsonNode row:rows) {
            if("TASK".equals(row.path("type").asText()))
                throw new IllegalArgumentException("WORK_NODE_TASK_DEFINITION_REQUIRED");
            var parent=row.path("parent_node_id");
            if(!parent.isNull() && !parent.isMissingNode()
                    && (!parent.isIntegralNumber() || !nodes.containsKey(parent.asLong())))
                throw new IllegalArgumentException("WORK_NODE_PARENT_OUTSIDE_ARCHIVE");
        }
        // Enforce a strict acyclic forest; no silently broken ancestry.
        for(Long start:nodes.keySet()) {
            var visited=new HashSet<Long>();
            Long current=start;
            while(current!=null) {
                if(!visited.add(current))
                    throw new IllegalArgumentException("WORK_NODE_CYCLE");
                var parent=nodes.get(current).path("parent_node_id");
                current=parent.isNull() || parent.isMissingNode()?null:parent.asLong();
            }
        }
        return tx.execute(status->{
            Long workspace=db.queryForObject("SELECT id FROM families WHERE id=? FOR UPDATE",
                    Long.class,target);
            if(workspace==null)throw new IllegalArgumentException("TARGET_MISSING");
            Long epoch=db.queryForObject("SELECT data_epoch FROM families WHERE id=?",Long.class,target);
            Long existing=db.queryForObject("SELECT count(*) FROM work_nodes WHERE family_id=?",
                    Long.class,target);
            if(epoch==null || epoch!=0L || existing==null || existing!=0L)
                throw new IllegalStateException("TARGET_WORK_NODE_CONFLICT");
            // Insert parentless nodes first, then populate parent IDs in
            // topological order. No source PK or foreign owner is imported.
            var mapping=new TreeMap<Long,Long>();
            var pending=new TreeMap<>(nodes);
            while(!pending.isEmpty()) {
                int advanced=0;
                var iter=pending.entrySet().iterator();
                while(iter.hasNext()) {
                    var pair=iter.next();var row=pair.getValue();
                    var parent=row.path("parent_node_id");
                    Long parentId=parent.isNull() || parent.isMissingNode()?null:parent.asLong();
                    if(parentId!=null && !mapping.containsKey(parentId))continue;
                    Long newId=db.queryForObject("""
                            INSERT INTO work_nodes(family_id,parent_node_id,type,title,description,sort_order,active)
                            VALUES (?,?,?,?,?,?,?) RETURNING id
                            """,Long.class,target,parentId==null?null:mapping.get(parentId),
                            row.path("type").asText(),row.path("title").asText(),
                            row.path("description").isNull()?null:row.path("description").asText(null),
                            row.path("sort_order").asInt(),row.path("active").asBoolean());
                    if(newId==null || newId.equals(pair.getKey()))
                        throw new IllegalStateException("WORK_NODE_ID_REMAP_FAILED");
                    mapping.put(pair.getKey(),newId);
                    iter.remove();advanced++;
                }
                if(advanced==0)throw new IllegalStateException("WORK_NODE_UNRESOLVED_ANCESTRY");
            }
            Long after=db.queryForObject("SELECT count(*) FROM work_nodes WHERE family_id=?",
                    Long.class,target);
            if(after==null || after!=rows.size())
                throw new IllegalStateException("WORK_NODE_COUNT_MISMATCH");
            return new Result(Map.copyOf(mapping),rows.size());
        });
    }
}
