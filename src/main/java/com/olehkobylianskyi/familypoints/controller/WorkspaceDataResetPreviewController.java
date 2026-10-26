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
                count("reward_definitions", workspaceId), false,
                "Purchases, requests, time sessions and point ledger dependencies not yet resolved"));
        return new ResetPreview(workspaceId, false, false,
                List.of("Global account and authentication", "Workspace identity and members",
                        "Other workspaces", "Global bundled template catalog",
                        "Existing financial balances and ledger"), categories);
    }

    private long count(String trustedTable, Long workspaceId) {
        // Invoked exclusively with three hardcoded internal table identifiers;
        // workspaceId remains a prepared-statement parameter.
        Long count = db.queryForObject(
                "SELECT count(*) FROM " + trustedTable + " WHERE workspace_id=?",
                Long.class, workspaceId);
        return count == null ? 0L : count;
    }
}
