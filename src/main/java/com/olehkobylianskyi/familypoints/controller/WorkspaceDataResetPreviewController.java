package com.olehkobylianskyi.familypoints.controller;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;

/**
 * BKL-002 phase 1: read-only reset inventory. This controller intentionally
 * has NO deletion or execution endpoint. The category list is a partial
 * inventory and must not be treated as an executable deletion plan.
 */
@RestController
@RequestMapping("/workspaces/{workspaceId}/data-reset")
@PreAuthorize("@workspaceSecurity.canManageWorkspace(#workspaceId)")
public class WorkspaceDataResetPreviewController {
    private final JdbcTemplate db;

    public WorkspaceDataResetPreviewController(JdbcTemplate db) {
        this.db = db;
    }

    public record ResetCategory(String code, long recordCount, boolean executable,
                                String warning) {}
    public record ResetPreview(Long workspaceId, boolean executionEnabled,
                               boolean completeDependencyPlan,
                               List<String> alwaysPreserved,
                               Map<String, ResetCategory> categories) {}

    @GetMapping("/preview")
    public ResetPreview preview(@PathVariable Long workspaceId) {
        Integer exists = db.queryForObject(
                "SELECT count(*) FROM families WHERE id=?", Integer.class, workspaceId);
        if (exists == null || exists != 1)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found");
        Map<String, ResetCategory> categories = new LinkedHashMap<>();
        categories.put("PRIVATE_TEMPLATES", new ResetCategory("PRIVATE_TEMPLATES",
                count("workspace_private_templates", workspaceId), false,
                "Activation request references need coordinated review"));
        categories.put("TASK_DEFINITIONS", new ResetCategory("TASK_DEFINITIONS",
                count("task_definitions", workspaceId), false,
                "Executions, schedules, approvals, history and ledger dependencies not yet resolved"));
        categories.put("REWARD_DEFINITIONS", new ResetCategory("REWARD_DEFINITIONS",
                countFamily("reward_definitions", workspaceId), false,
                "Purchases, requests, time sessions and point ledger dependencies not yet resolved"));
        categories.put("TASK_EXECUTIONS", new ResetCategory("TASK_EXECUTIONS",
                db.queryForObject("""
                    SELECT count(*) FROM task_instances i
                    JOIN family_members m ON m.id=i.member_id
                    WHERE m.family_id=?
                    """, Long.class, workspaceId), false,
                "Task audit, participants and rewards must be accounted for"));
        categories.put("REWARD_REQUESTS", new ResetCategory("REWARD_REQUESTS",
                countFamily("reward_requests", workspaceId), false,
                "Approvals, obligations and purchases must be reviewed"));
        categories.put("REWARD_PURCHASES", new ResetCategory("REWARD_PURCHASES",
                db.queryForObject("""
                    SELECT count(*) FROM reward_purchases p
                    JOIN family_members m ON m.id=p.member_id
                    WHERE m.family_id=?
                    """, Long.class, workspaceId), false,
                "Historical spending and obligation dependencies are protected"));
        categories.put("POINT_LEDGER", new ResetCategory("POINT_LEDGER",
                db.queryForObject("""
                    SELECT count(*) FROM point_transactions t
                    JOIN family_members m ON m.id=t.member_id
                    WHERE m.family_id=?
                    """, Long.class, workspaceId), false,
                "Immutable point transaction history is protected"));
        categories.put("REPUTATION_LEDGER", new ResetCategory("REPUTATION_LEDGER",
                db.queryForObject("""
                    SELECT count(*) FROM reputation_transactions t
                    JOIN family_members m ON m.id=t.member_id
                    WHERE m.family_id=?
                    """, Long.class, workspaceId), false,
                "Immutable reputation transaction history is protected"));
        categories.put("MEMBER_GROUPS", new ResetCategory("MEMBER_GROUPS",
                countFamily("member_groups", workspaceId), false,
                "Memberships, nested groups, permissions and group balances require coordinated cleanup"));
        categories.put("WORK_NODES", new ResetCategory("WORK_NODES",
                countFamily("work_nodes", workspaceId), false,
                "Task hierarchy and descendant references must be preserved until dependency plan approved"));
        categories.put("TASK_SCHEDULE_SLOTS", new ResetCategory("TASK_SCHEDULE_SLOTS",
                db.queryForObject("""
                    SELECT count(*) FROM task_schedule_slots s
                    JOIN task_definitions d ON d.id=s.task_definition_id
                    WHERE d.family_id=?
                    """, Long.class, workspaceId), false,
                "Scheduled occurrences and task history depend on these slots"));
        categories.put("MEDIA_ASSETS", new ResetCategory("MEDIA_ASSETS",
                count("media_assets", workspaceId), false,
                "Media blobs and bindings require coordinated removal; global catalog images unaffected"));
        categories.put("MEDIA_BINDINGS", new ResetCategory("MEDIA_BINDINGS",
                count("media_bindings", workspaceId), false,
                "Member/group/task image references must be reconciled"));
        return new ResetPreview(workspaceId, false, false,
                List.of("Global account and authentication", "Workspace identity and members",
                        "Other workspaces", "Global bundled template catalog",
                        "Existing financial balances and ledger"), categories);
    }

    /**
     * Read-only FK dependency map used to design a safe reset plan.
     * PostgreSQL catalog introspection only; no table contents are returned.
     * All constraints are included to expose dependencies outside the currently
     * counted categories before any destructive operation can be considered.
     */
    public record ForeignKeyDependency(String dependentTable, String referencedTable,
                                       String constraintName, String deleteAction) {}

    @GetMapping("/dependencies")
    public List<ForeignKeyDependency> dependencies(@PathVariable Long workspaceId) {
        Integer exists = db.queryForObject(
                "SELECT count(*) FROM families WHERE id=?", Integer.class, workspaceId);
        if (exists == null || exists != 1)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found");
        return db.query("""
                SELECT dependent.relname, referenced.relname, c.conname,
                       CASE c.confdeltype
                         WHEN 'c' THEN 'CASCADE'
                         WHEN 'n' THEN 'SET_NULL'
                         WHEN 'd' THEN 'SET_DEFAULT'
                         WHEN 'r' THEN 'RESTRICT'
                         ELSE 'NO_ACTION' END
                FROM pg_catalog.pg_constraint c
                JOIN pg_catalog.pg_class dependent ON dependent.oid=c.conrelid
                JOIN pg_catalog.pg_class referenced ON referenced.oid=c.confrelid
                JOIN pg_catalog.pg_namespace dn ON dn.oid=dependent.relnamespace
                JOIN pg_catalog.pg_namespace rn ON rn.oid=referenced.relnamespace
                WHERE c.contype='f' AND dn.nspname='public' AND rn.nspname='public'
                ORDER BY referenced.relname, dependent.relname, c.conname
                """, (rs,i) -> new ForeignKeyDependency(
                    rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4)));
    }

    /**
     * Selection validator for the future reset wizard. Deliberately NOT a
     * deletion authorization: even a valid plan is never executable until
     * full FK, audit, generation fencing and transactional design is approved.
     */
    public record ResetPlanRequest(Set<String> categories) {}
    public record ResetPlan(Long workspaceId, Set<String> selectedCategories,
                            boolean executable, List<String> blockers,
                            List<String> preserved) {}

    private static final Set<String> PLANNABLE = Set.of(
            "PRIVATE_TEMPLATES", "TASK_DEFINITIONS", "TASK_EXECUTIONS",
            "REWARD_DEFINITIONS", "REWARD_REQUESTS", "REWARD_PURCHASES",
            "POINT_LEDGER", "REPUTATION_LEDGER", "MEMBER_GROUPS",
            "WORK_NODES", "TASK_SCHEDULE_SLOTS", "MEDIA_ASSETS", "MEDIA_BINDINGS");

    @PostMapping("/plan")
    public ResetPlan plan(@PathVariable Long workspaceId, @RequestBody ResetPlanRequest request) {
        Integer exists = db.queryForObject(
                "SELECT count(*) FROM families WHERE id=?", Integer.class, workspaceId);
        if (exists == null || exists != 1)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found");
        if (request == null || request.categories() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Categories required");
        Set<String> selected = new LinkedHashSet<>(request.categories());
        if (selected.contains(null) || !PLANNABLE.containsAll(selected))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown reset category");

        List<String> blockers = new java.util.ArrayList<>();
        if (selected.contains("TASK_DEFINITIONS") &&
                !selected.containsAll(Set.of("TASK_EXECUTIONS", "TASK_SCHEDULE_SLOTS")))
            blockers.add("TASK_DEFINITIONS requires TASK_EXECUTIONS and TASK_SCHEDULE_SLOTS");
        if (selected.contains("REWARD_DEFINITIONS") &&
                !selected.containsAll(Set.of("REWARD_REQUESTS", "REWARD_PURCHASES")))
            blockers.add("REWARD_DEFINITIONS requires REWARD_REQUESTS and REWARD_PURCHASES");
        if (selected.contains("MEDIA_ASSETS") && !selected.contains("MEDIA_BINDINGS"))
            blockers.add("MEDIA_ASSETS requires MEDIA_BINDINGS");
        if (selected.contains("MEMBER_GROUPS") && !selected.contains("WORK_NODES"))
            blockers.add("Group and work hierarchy references must be reviewed before reset");
        blockers.add("Execution is disabled until dependent FK graph and tenant-safe deletion order are approved");
        blockers.add("Audit, confirmation and idempotency records are not implemented");
        blockers.add("Offline operation generation fencing and ledger reconciliation are not implemented");
        return new ResetPlan(workspaceId, Set.copyOf(selected), false, List.copyOf(blockers),
                List.of("Account authentication", "Workspace and member identities",
                        "Other workspaces", "Global catalog"));
    }

    private long countFamily(String trustedTable, Long workspaceId) {
        // Only hardcoded names from this controller may reach this method.
        Long value = db.queryForObject("SELECT count(*) FROM " + trustedTable +
                " WHERE family_id=?", Long.class, workspaceId);
        return value == null ? 0L : value;
    }

    private long count(String trustedTable, Long workspaceId) {
        // Invoked exclusively with hardcoded internal table identifiers;
        // workspaceId remains a prepared-statement parameter.
        Long count = db.queryForObject(
                "SELECT count(*) FROM " + trustedTable + " WHERE workspace_id=?",
                Long.class, workspaceId);
        return count == null ? 0L : count;
    }
}
