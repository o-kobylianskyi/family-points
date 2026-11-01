package com.olehkobylianskyi.familypoints.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * BKL-002 preflight. Schema introspection only: NEVER authorizes execution,
 * guesses a delete order or modifies family records.
 */
@Service
public class WorkspaceResetDependencyAuditService {
    private final JdbcTemplate db;

    private static final Set<String> PROTECTED = Set.of(
            "families", "family_members", "user_accounts", "workspace_roles",
            "workspace_setup_states", "point_transactions", "reputation_transactions",
            "point_types", "account_external_identities", "flyway_schema_history");

    public WorkspaceResetDependencyAuditService(JdbcTemplate db) {this.db=db;}

    public record ForeignKey(String dependentTable, String dependentColumn,
                             String referencedTable, String referencedColumn,
                             String constraintName, String deleteAction) {}
    public record Audit(long workspaceId, boolean executionEnabled,
                        boolean dependenciesApproved, List<String> protectedTables,
                        List<ForeignKey> foreignKeys, List<String> blockers) {}

    @Transactional(readOnly=true)
    public Audit audit(long workspaceId) {
        Integer exists=db.queryForObject("SELECT count(*) FROM families WHERE id=?",
                Integer.class,workspaceId);
        if(exists==null || exists!=1)
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND,"Workspace not found");

        List<ForeignKey> edges=db.query("""
                SELECT child.relname, child_attr.attname, parent.relname, parent_attr.attname,
                       fk.conname,
                       CASE fk.confdeltype WHEN 'c' THEN 'CASCADE'
                           WHEN 'n' THEN 'SET_NULL' WHEN 'd' THEN 'SET_DEFAULT'
                           WHEN 'r' THEN 'RESTRICT' ELSE 'NO_ACTION' END
                FROM pg_catalog.pg_constraint fk
                JOIN pg_catalog.pg_class child ON child.oid=fk.conrelid
                JOIN pg_catalog.pg_namespace child_ns ON child_ns.oid=child.relnamespace
                JOIN pg_catalog.pg_class parent ON parent.oid=fk.confrelid
                JOIN pg_catalog.pg_namespace parent_ns ON parent_ns.oid=parent.relnamespace
                JOIN LATERAL unnest(fk.conkey) WITH ORDINALITY src(attnum, ordinal) ON true
                JOIN LATERAL unnest(fk.confkey) WITH ORDINALITY dst(attnum, ordinal)
                  ON dst.ordinal=src.ordinal
                JOIN pg_catalog.pg_attribute child_attr
                  ON child_attr.attrelid=child.oid AND child_attr.attnum=src.attnum
                JOIN pg_catalog.pg_attribute parent_attr
                  ON parent_attr.attrelid=parent.oid AND parent_attr.attnum=dst.attnum
                WHERE fk.contype='f' AND child_ns.nspname='public'
                  AND parent_ns.nspname='public'
                ORDER BY parent.relname,child.relname,fk.conname,src.ordinal
                """,(rs,i)->new ForeignKey(rs.getString(1),rs.getString(2),
                        rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6)));
        // A complete catalog is not a signed closure proof. Protected targets can
        // have CASCADE inbound edges, so we expose this risk without modifying data.
        var blockers=new TreeSet<String>();
        blockers.add("RESET_EXECUTION_DISABLED");
        blockers.add("TENANT_SCOPED_DELETE_CLOSURE_NOT_APPROVED");
        blockers.add("FINANCIAL_LEDGER_AND_BALANCE_POLICY_NOT_APPROVED");
        blockers.add("CHALLENGE_CONSUMPTION_AND_REAUTH_NOT_COMPLETE");
        blockers.add("CROSS_FAMILY_FK_AND_CONCURRENCY_PROOF_NOT_COMPLETE");
        for(var edge:edges) {
            if(PROTECTED.contains(edge.referencedTable()) &&
                    "CASCADE".equals(edge.deleteAction()))
                blockers.add("REVIEW_PROTECTED_CASCADE:"+edge.constraintName());
        }
        return new Audit(workspaceId,false,false,
                PROTECTED.stream().sorted().toList(),edges,List.copyOf(blockers));
    }
}
