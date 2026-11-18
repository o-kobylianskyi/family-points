package com.olehkobylianskyi.familypoints;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;

/**
 * TEST ONLY: replays an explicitly selected source GROUP + two-level WORK_NODE
 * graph between independent disposable PostgreSQL 18 databases. Source DB
 * must still exist. Not a V50 portable archive importer, no owner grant and
 * no production Restore API.
 */
final class DisposableTwoDatabaseRestoreGraphExecutor {
    record Result(long groupId,Map<Long,Long> workNodeIds) {}
    private record SourceNode(long id,Long parentId,String title,int sortOrder) {}

    private final JdbcTemplate source;
    private final JdbcTemplate target;
    private final TransactionTemplate tx;

    DisposableTwoDatabaseRestoreGraphExecutor(JdbcTemplate source,JdbcTemplate target,
                                               PlatformTransactionManager manager) {
        this.source=Objects.requireNonNull(source);
        this.target=Objects.requireNonNull(target);
        this.tx=new TransactionTemplate(Objects.requireNonNull(manager));
    }

    Result apply(long sourceFamily,long targetFamily,long sourceGroupId,
                 List<Long> selectedSourceNodeIds,Map<Long,Long> exactMembers,
                 int failAfterStep) {
        if(sourceFamily<=0 || targetFamily<=0 || sourceGroupId<=0
                || selectedSourceNodeIds==null || selectedSourceNodeIds.isEmpty()
                || exactMembers==null || exactMembers.isEmpty())
            throw new IllegalArgumentException("RESTORE_GRAPH_BAD_REQUEST");
        // This source-side proof works only while both original and target DBs
        // are online. It MUST NOT replace signed original member provenance.
        new DisposableMemberMappingPreflight().verify(
                source,target,sourceFamily,targetFamily,exactMembers);
        String groupName=source.queryForObject("""
                SELECT name FROM member_groups WHERE id=? AND family_id=?
                """,String.class,sourceGroupId,sourceFamily);
        if(groupName==null)
            throw new SecurityException("RESTORE_GROUP_FOREIGN_SOURCE");
        if(selectedSourceNodeIds.size()!=new HashSet<>(selectedSourceNodeIds).size())
            throw new SecurityException("RESTORE_NODE_DUPLICATE_SELECTION");
        var nodes=new TreeMap<Long,SourceNode>();
        for(Long oldId:selectedSourceNodeIds) {
            if(oldId==null || oldId<=0)throw new SecurityException("RESTORE_NODE_INVALID_ID");
            var rows=source.query("""
                    SELECT id,parent_node_id,title,sort_order
                    FROM work_nodes WHERE id=? AND family_id=? AND type='GROUP'
                    """,(rs,n)->new SourceNode(rs.getLong("id"),
                            rs.getObject("parent_node_id",Long.class),
                            rs.getString("title"),rs.getInt("sort_order")),
                    oldId,sourceFamily);
            if(rows.size()!=1 || nodes.putIfAbsent(oldId,rows.get(0))!=null)
                throw new SecurityException("RESTORE_NODE_FOREIGN_SOURCE");
        }
        for(SourceNode node:nodes.values()) {
            if(node.parentId()!=null && !nodes.containsKey(node.parentId()))
                throw new SecurityException("RESTORE_NODE_PARENT_NOT_SELECTED");
        }
        // Reject cycles and determine exact parent-first insertion order.
        var pending=new TreeMap<>(nodes);
        var ordered=new ArrayList<SourceNode>();
        var scheduled=new HashSet<Long>();
        while(!pending.isEmpty()) {
            boolean advanced=false;
            for(var it=pending.entrySet().iterator();it.hasNext();) {
                var item=it.next();
                if(item.getValue().parentId()==null
                        || scheduled.contains(item.getValue().parentId())) {
                    ordered.add(item.getValue());
                    scheduled.add(item.getKey());
                    it.remove();
                    advanced=true;
                }
            }
            if(!advanced)throw new SecurityException("RESTORE_NODE_SOURCE_CYCLE");
        }
        return tx.execute(status->{
            Long epoch=target.queryForObject("""
                    SELECT data_epoch FROM families WHERE id=? FOR UPDATE
                    """,Long.class,targetFamily);
            if(epoch==null || epoch!=0L)
                throw new SecurityException("RESTORE_TARGET_EPOCH_INVALID");
            Long existingGroups=target.queryForObject(
                    "SELECT count(*) FROM member_groups WHERE family_id=?",
                    Long.class,targetFamily);
            Long existingNodes=target.queryForObject(
                    "SELECT count(*) FROM work_nodes WHERE family_id=?",
                    Long.class,targetFamily);
            if(!Objects.equals(0L,existingGroups)||!Objects.equals(0L,existingNodes))
                throw new SecurityException("RESTORE_TARGET_NOT_EMPTY");
            // Recheck the current target-side roster while the target family
            // epoch row is locked; source identity approval remains missing.
            new DisposableMemberMappingPreflight().verify(
                    source,target,sourceFamily,targetFamily,exactMembers);
            Long group=target.queryForObject("""
                    INSERT INTO member_groups(family_id,name,active,show_in_navigation)
                    VALUES (?,?,true,false) RETURNING id
                    """,Long.class,targetFamily,groupName);
            if(failAfterStep==1)throw new IllegalStateException("QA_FAIL_AFTER_GROUP");
            var inserted=new TreeMap<Long,Long>();
            for(SourceNode node:ordered) {
                Long parent=node.parentId()==null?null:inserted.get(node.parentId());
                Long id=target.queryForObject("""
                        INSERT INTO work_nodes(
                            family_id,parent_node_id,type,title,sort_order,active)
                        VALUES (?,?,'GROUP',?,?,true) RETURNING id
                        """,Long.class,targetFamily,parent,node.title(),node.sortOrder());
                if(id==null || id<=0)throw new IllegalStateException("QA_NODE_INSERT_FAILED");
                inserted.put(node.id(),id);
                if(failAfterStep==2 && inserted.size()==1)
                    throw new IllegalStateException("QA_FAIL_AFTER_PARENT_NODE");
            }
            if(failAfterStep==3)throw new IllegalStateException("QA_FAIL_AFTER_CHILD_NODE");
            return new Result(group,Map.copyOf(inserted));
        });
    }
}
