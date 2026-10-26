package com.olehkobylianskyi.familypoints.controller;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
