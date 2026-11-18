package com.olehkobylianskyi.familypoints.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Live PostgreSQL original-member FK inventory. An unknown FK never inherits
 * permission from a coincidentally numbered member. This is a source EXPORT
 * safety boundary, not permission to insert anything into a target family.
 *
 * Every actual FK column is classified; sensitive account/auth rows remain
 * EXCLUDED, and unsupported live business dependents BLOCK full history export.
 */
@Component
public final class WorkspacePortableMemberFkClosure {
    public enum Disposition { INCLUDE, DEPENDENCY, EXCLUDE, BLOCK }
    public record Edge(String table,String column,Disposition disposition,long sourceRows) {}
    private final JdbcTemplate db;
    public WorkspacePortableMemberFkClosure(JdbcTemplate db) {this.db=db;}

    private static final Map<String,Disposition> KNOWN = Map.ofEntries(
        Map.entry("user_accounts.family_member_id",Disposition.EXCLUDE),
        Map.entry("account_workspace_memberships.family_member_id",Disposition.EXCLUDE),
        Map.entry("task_definitions.assigned_member_id",Disposition.INCLUDE),
        Map.entry("task_definitions.created_by_member_id",Disposition.INCLUDE),
        Map.entry("task_definitions.preferred_member_id",Disposition.INCLUDE),
        Map.entry("task_definitions.responsible_member_id",Disposition.INCLUDE),
        Map.entry("task_instances.member_id",Disposition.INCLUDE),
        Map.entry("task_instances.original_assigned_member_id",Disposition.INCLUDE),
        Map.entry("reward_purchases.member_id",Disposition.INCLUDE),
        Map.entry("point_transactions.member_id",Disposition.INCLUDE),
        Map.entry("reputation_transactions.member_id",Disposition.BLOCK),
        Map.entry("reward_requests.requested_by_member_id",Disposition.BLOCK),
        Map.entry("reward_obligations.member_id",Disposition.BLOCK),
        Map.entry("task_delegations.delegated_by_member_id",Disposition.BLOCK),
        Map.entry("task_delegations.from_member_id",Disposition.BLOCK),
        Map.entry("task_delegations.to_member_id",Disposition.BLOCK),
        Map.entry("point_exchange_requests.member_id",Disposition.BLOCK),
        Map.entry("point_exchange_requests.processed_by_member_id",Disposition.BLOCK),
        Map.entry("group_memberships.member_id",Disposition.BLOCK),
        Map.entry("member_exception_periods.member_id",Disposition.BLOCK),
        Map.entry("task_reward_requests.requested_by_member_id",Disposition.BLOCK),
        Map.entry("task_suggestions.requested_by_member_id",Disposition.BLOCK),
        Map.entry("task_suggestions.reviewed_by_member_id",Disposition.BLOCK)
    );

    public static Map<String,Disposition> documentedDispositions() {
        return KNOWN;
    }

    public List<Edge> inspect(long workspaceId) {
        if(workspaceId<=0)throw new SecurityException("PORTABLE_MEMBER_FK_SCOPE_INVALID");
        // pg_constraint rather than a stale handwritten list, because later
        // Flyway migrations may add new original-member references.
        var constraints=db.queryForList("""
            SELECT child.relname AS child_table, attr.attname AS child_column
              FROM pg_constraint fk
              JOIN pg_class child ON child.oid=fk.conrelid
              JOIN pg_namespace ns ON ns.oid=child.relnamespace
              JOIN pg_attribute attr ON attr.attrelid=fk.conrelid
                   AND attr.attnum=fk.conkey[1]
             WHERE fk.contype='f'
               AND fk.confrelid='public.family_members'::regclass
               AND cardinality(fk.conkey)=1
               AND ns.nspname='public'
             ORDER BY child.relname,attr.attname
            """);
        if(constraints.isEmpty())
            throw new SecurityException("PORTABLE_MEMBER_FK_CATALOG_UNAVAILABLE");
        List<Edge> result=new ArrayList<>();
        var visited=new HashSet<String>();
        for(Map<String,Object> entry:constraints) {
            String table=Objects.toString(entry.get("child_table"),"");
            String column=Objects.toString(entry.get("child_column"),"");
            // Identifiers are sourced solely from PostgreSQL's catalog and
            // validated before embedding in server-owned SQL.
            if(!table.matches("[a-z][a-z0-9_]*")
                    ||!column.matches("[a-z][a-z0-9_]*"))
                throw new SecurityException("PORTABLE_MEMBER_FK_IDENTIFIER_INVALID");
            String id=table+"."+column;
            if(!visited.add(id))throw new SecurityException("PORTABLE_MEMBER_FK_DUPLICATE");
            Disposition disposition=KNOWN.getOrDefault(id,Disposition.BLOCK);
            Long count=db.queryForObject(
                "SELECT count(*) FROM public."+table+" t "+
                "JOIN public.family_members m ON m.id=t."+column+" "+
                "WHERE m.family_id=?",Long.class,workspaceId);
            if(count==null||count<0)
                throw new SecurityException("PORTABLE_MEMBER_FK_COUNT_UNAVAILABLE");
            result.add(new Edge(table,column,disposition,count));
        }
        return List.copyOf(result);
    }

    /**
     * An eight-category export is permitted only if it contains every
     * INCLUDED original-member dependent and no live BLOCK edge. EXCLUDE is
     * only for explicitly named credentials/account linkage, never an
     * authorization to recreate identity from an archive member ID.
     */
    public void requireHistoryClosure(long workspaceId) {
        var edges=inspect(workspaceId);
        for(Edge e:edges) {
            if(e.disposition()==Disposition.BLOCK && e.sourceRows()>0)
                throw new SecurityException("PORTABLE_V2_UNSUPPORTED_MEMBER_FK:"
                        +e.table()+"."+e.column());
        }
    }
}
