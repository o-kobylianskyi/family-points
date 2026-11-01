package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.service.WorkspaceResetDependencyAuditService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** BKL-002 diagnostic only; no execute and no mutation paths. */
@RestController
@RequestMapping("/workspaces/{workspaceId}/data-reset")
@PreAuthorize("@workspaceSecurity.canManageWorkspace(#workspaceId)")
public class WorkspaceResetDependencyAuditController {
    private final WorkspaceResetDependencyAuditService audit;
    public WorkspaceResetDependencyAuditController(WorkspaceResetDependencyAuditService audit) {
        this.audit=audit;
    }
    @GetMapping("/dependency-audit")
    public ResponseEntity<WorkspaceResetDependencyAuditService.Audit> dependencyAudit(
            @PathVariable Long workspaceId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(audit.audit(workspaceId));
    }
}
