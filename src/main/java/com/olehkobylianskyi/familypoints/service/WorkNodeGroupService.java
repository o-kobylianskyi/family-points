package com.olehkobylianskyi.familypoints.service;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

/** Task-folders only; never creates or changes MemberGroups, tasks or ledger entries. */
@Service
public class WorkNodeGroupService {
    private final JdbcTemplate db;
    private final WorkspaceEpochTransactionGuard epoch;
    public WorkNodeGroupService(JdbcTemplate db, WorkspaceEpochTransactionGuard epoch) {
        this.db=db; this.epoch=epoch;
    }
    public record Group(Long id, Long parentGroupId, String title, String description,
                        int sortOrder, boolean active) {}
    public record Input(String title, String description, Long parentGroupId, Integer sortOrder) {}
    private final org.springframework.jdbc.core.RowMapper<Group> mapper=(r,i)->
            new Group(r.getLong("id"),(Long)r.getObject("parent_node_id"),
                      r.getString("title"),r.getString("description"),
                      r.getInt("sort_order"),r.getBoolean("active"));

    @Transactional(readOnly=true)
    public List<Group> list(long family) {
        return db.query("""
                SELECT id,parent_node_id,title,description,sort_order,active
                  FROM work_nodes WHERE family_id=? AND type='GROUP' AND active=true
                  ORDER BY parent_node_id NULLS FIRST,sort_order,id
                """,mapper,family);
    }

    @Transactional
    public Group create(long family,Input input) {
        epoch.requireCurrentRequestEpoch(family);
        String name=validTitle(input);
        long parent=validatedParent(family,input.parentGroupId(),null);
        var existing=db.query("""
                SELECT id,parent_node_id,title,description,sort_order,active FROM work_nodes
                WHERE family_id=? AND type='GROUP' AND active=true
                  AND parent_node_id IS NOT DISTINCT FROM ? AND lower(title)=lower(?)
                """,mapper,family,parent==0L?null:parent,name);
        if(!existing.isEmpty())
            throw new ResponseStatusException(HttpStatus.CONFLICT,"WORK_NODE_GROUP_DUPLICATE");
        Long id=db.queryForObject("""
                INSERT INTO work_nodes(family_id,parent_node_id,type,title,description,sort_order,active)
                VALUES (?,?,'GROUP',?,?,?,true) RETURNING id
                """,Long.class,family,parent==0L?null:parent,name,description(input),sort(input));
        return get(family,id);
    }

    @Transactional
    public Group update(long family,long id,Input input) {
        epoch.requireCurrentRequestEpoch(family);
        Group existing=get(family,id);
        String name=validTitle(input);
        long parent=validatedParent(family,input.parentGroupId(),id);
        // Ancestor traversal catches cycles at any depth.
        Long ancestor=parent==0?null:parent;
        while(ancestor!=null) {
            if(ancestor==id)
                throw new ResponseStatusException(HttpStatus.CONFLICT,"WORK_NODE_GROUP_CYCLE");
            var rows=db.queryForList("""
                    SELECT parent_node_id FROM work_nodes
                    WHERE id=? AND family_id=? AND type='GROUP' AND active=true
                    """,Long.class,ancestor,family);
            if(rows.isEmpty())
                throw new ResponseStatusException(HttpStatus.CONFLICT,"WORK_NODE_GROUP_PARENT_MISSING");
            ancestor=rows.get(0);
        }
        var collisions=db.queryForList("""
                SELECT id FROM work_nodes WHERE family_id=? AND type='GROUP'
                AND active=true AND id<>? AND parent_node_id IS NOT DISTINCT FROM ?
                AND lower(title)=lower(?)
                """,Long.class,family,id,parent==0?null:parent,name);
        if(!collisions.isEmpty())
            throw new ResponseStatusException(HttpStatus.CONFLICT,"WORK_NODE_GROUP_DUPLICATE");
        db.update("""
                UPDATE work_nodes SET parent_node_id=?,title=?,description=?,sort_order=?
                WHERE id=? AND family_id=? AND type='GROUP' AND active=true
                """,parent==0?null:parent,name,description(input),sort(input),id,family);
        return get(family,id);
    }

    @Transactional
    public void deactivate(long family,long id) {
        epoch.requireCurrentRequestEpoch(family);
        get(family,id);
        Long count=db.queryForObject("""
                SELECT count(*) FROM work_nodes WHERE parent_node_id=? AND family_id=? AND active=true
                """,Long.class,id,family);
        if(count!=null && count>0)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"WORK_NODE_GROUP_NOT_EMPTY");
        db.update("UPDATE work_nodes SET active=false WHERE id=? AND family_id=? AND type='GROUP'",id,family);
    }

    private Group get(long family,long id) {
        var rows=db.query("""
                SELECT id,parent_node_id,title,description,sort_order,active
                FROM work_nodes WHERE id=? AND family_id=? AND type='GROUP' AND active=true
                """,mapper,id,family);
        if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"WORK_NODE_GROUP_NOT_FOUND");
        return rows.get(0);
    }
    private long validatedParent(long family,Long parent,Long self) {
        if(parent==null)return 0L;
        if(self!=null && self.equals(parent))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"WORK_NODE_GROUP_CYCLE");
        get(family,parent);
        return parent;
    }
    private static String validTitle(Input i) {
        if(i==null||i.title()==null||i.title().isBlank()||i.title().trim().length()>150)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"WORK_NODE_GROUP_TITLE_INVALID");
        return i.title().trim();
    }
    private static String description(Input i) {
        if(i.description()!=null && i.description().length()>1000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"WORK_NODE_GROUP_DESCRIPTION_INVALID");
        return i.description();
    }
    private static int sort(Input i) { return i.sortOrder()==null?0:i.sortOrder(); }
}
