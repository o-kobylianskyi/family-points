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

    /**
     * Conservative category-scoped preflight. A report is NEVER an execution
     * authorization: unknown and protected categories fail closed.
     */
    public record SelectionAudit(long workspaceId, java.util.List<String> selectedCategories,
            boolean executable, java.util.List<String> blockers,
            java.util.List<ForeignKey> relevantForeignKeys) {}

    private static final java.util.Map<String,java.util.Set<String>> CATEGORY_TABLES=java.util.Map.ofEntries(
            java.util.Map.entry("PRIVATE_TEMPLATES",java.util.Set.of("workspace_private_templates")),
            java.util.Map.entry("TASK_DEFINITIONS",java.util.Set.of("task_definitions")),
            java.util.Map.entry("TASK_EXECUTIONS",java.util.Set.of("task_instances")),
            java.util.Map.entry("TASK_SCHEDULE_SLOTS",java.util.Set.of("task_schedule_slots")),
            java.util.Map.entry("REWARD_DEFINITIONS",java.util.Set.of("reward_definitions")),
            java.util.Map.entry("REWARD_REQUESTS",java.util.Set.of("reward_requests")),
            java.util.Map.entry("REWARD_PURCHASES",java.util.Set.of("reward_purchases")),
            java.util.Map.entry("MEMBER_GROUPS",java.util.Set.of("member_groups")),
            java.util.Map.entry("WORK_NODES",java.util.Set.of("work_nodes")),
            java.util.Map.entry("MEDIA_ASSETS",java.util.Set.of("media_assets")),
            java.util.Map.entry("MEDIA_BINDINGS",java.util.Set.of("media_bindings")),
            java.util.Map.entry("POINT_LEDGER",java.util.Set.of("point_transactions")),
            java.util.Map.entry("REPUTATION_LEDGER",java.util.Set.of("reputation_transactions")));

    @Transactional(readOnly=true)
    public SelectionAudit inspectSelection(long workspaceId, java.util.Collection<String> categories) {
        if(categories==null || categories.isEmpty() || categories.stream().anyMatch(java.util.Objects::isNull)
                || !CATEGORY_TABLES.keySet().containsAll(categories))
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,"Invalid category selection");
        var selected=categories.stream().distinct().sorted().toList();
        var audit=audit(workspaceId);
        var tables=new java.util.TreeSet<String>();
        selected.forEach(category->tables.addAll(CATEGORY_TABLES.get(category)));
        var blockers=new java.util.TreeSet<String>(audit.blockers());
        for(var category:selected) {
            if(category.endsWith("_LEDGER"))blockers.add("PROTECTED_LEDGER:"+category);
        }
        var edges=new java.util.ArrayList<ForeignKey>();
        for(var fk:audit.foreignKeys()) {
            boolean incoming=tables.contains(fk.referencedTable());
            boolean outgoing=tables.contains(fk.dependentTable());
            if(!incoming && !outgoing)continue;
            edges.add(fk);
            if(incoming && !outgoing)
                blockers.add("EXTERNAL_FK_DEPENDENCY:"+fk.dependentTable()+"."+fk.constraintName());
            if(incoming && PROTECTED.contains(fk.dependentTable()))
                blockers.add("PROTECTED_DEPENDENT:"+fk.dependentTable()+"."+fk.constraintName());
            if(outgoing && PROTECTED.contains(fk.referencedTable()))
                blockers.add("PROTECTED_PARENT_REFERENCE:"+fk.referencedTable()+"."+fk.constraintName());
            if(incoming && !outgoing && "CASCADE".equals(fk.deleteAction()))
                blockers.add("UNSELECTED_CASCADE_DEPENDENT:"+fk.dependentTable()+"."+fk.constraintName());
        }
        // The category map alone cannot prove row-level family ownership:
        // every FK crossing the selection boundary must be reviewed, even where
        // the referenced table is globally shared or the FK has ON DELETE CASCADE.
        if(tables.stream().anyMatch(PROTECTED::contains))
            blockers.add("SELECTION_INCLUDES_PROTECTED_TABLE");
        if(selected.contains("TASK_DEFINITIONS") &&
                (!selected.contains("TASK_EXECUTIONS") || !selected.contains("TASK_SCHEDULE_SLOTS")))
            blockers.add("TASK_DEFINITIONS_REQUIRES_EXECUTIONS_AND_SLOTS");
        if(selected.contains("REWARD_DEFINITIONS") &&
                (!selected.contains("REWARD_REQUESTS") || !selected.contains("REWARD_PURCHASES")))
            blockers.add("REWARD_DEFINITIONS_REQUIRES_REQUESTS_AND_PURCHASES");
        if(selected.contains("MEDIA_ASSETS")&&!selected.contains("MEDIA_BINDINGS"))
            blockers.add("MEDIA_ASSETS_REQUIRES_BINDINGS");
        return new SelectionAudit(workspaceId,selected,false,java.util.List.copyOf(blockers),java.util.List.copyOf(edges));
    }

    /**
     * Production read-only support matrix for BKL-002. This is a capability
     * inventory, NOT a claim that a category can be safely reset or restored.
     * FK edges are real live PostgreSQL constraints, never a static guessed
     * deletion order. Protected identity, finance and audit stay fail-closed.
     */
    public record CategoryReadiness(String category, String table, boolean supportedForExecute,
            java.util.List<String> blockers, java.util.List<ForeignKey> incomingForeignKeys,
            java.util.List<ForeignKey> outgoingForeignKeys) {}
    public record RestoreReadiness(long workspaceId, boolean fullSupported,
            boolean selectiveExecuteEnabled, java.util.List<CategoryReadiness> categories,
            java.util.List<String> globalBlockers) {}

    @Transactional(readOnly=true)
    public RestoreReadiness restoreReadiness(long workspaceId) {
        var report=audit(workspaceId);
        var results=new java.util.ArrayList<CategoryReadiness>();
        for(var category:new java.util.TreeSet<>(CATEGORY_TABLES.keySet())) {
            String table=CATEGORY_TABLES.get(category).iterator().next();
            var incoming=report.foreignKeys().stream()
                    .filter(fk->table.equals(fk.referencedTable())).toList();
            var outgoing=report.foreignKeys().stream()
                    .filter(fk->table.equals(fk.dependentTable())).toList();
            var blockers=new java.util.TreeSet<String>();
            blockers.add("PRODUCTION_RESTORE_EXECUTOR_NOT_VERIFIED");
            blockers.add("OWNER_REAUTH_PROTECTIVE_BACKUP_NOT_VERIFIED");
            blockers.add("TRANSACTIONAL_REPLACE_RECEIPT_NOT_VERIFIED");
            if(category.endsWith("_LEDGER"))
                blockers.add("FINANCIAL_INTEGRITY_AND_HISTORY_POLICY_REQUIRED");
            if(!incoming.isEmpty())
                blockers.add("INBOUND_FK_DEPENDENCY_CLOSURE_UNVERIFIED");
            if(!outgoing.isEmpty())
                blockers.add("OUTBOUND_FK_IDENTITY_REMAP_UNVERIFIED");
            results.add(new CategoryReadiness(category,table,false,
                    java.util.List.copyOf(blockers),incoming,outgoing));
        }
        var global=new java.util.TreeSet<>(report.blockers());
        global.add("FULL_RESTORE_UNSUPPORTED");
        global.add("SELECTIVE_RESTORE_EXECUTE_DISABLED");
        global.add("PORTABLE_FILE_HTTP_NOT_VERIFIED");
        global.add("SERVER_LOCAL_VERIFIED_BACKUP_RETENTION_NOT_VERIFIED");
        return new RestoreReadiness(workspaceId,false,false,
                java.util.List.copyOf(results),java.util.List.copyOf(global));
    }

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
