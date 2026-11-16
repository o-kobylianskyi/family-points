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
    public record SelectionRequest(java.util.List<String> categories) {}

    @PostMapping("/dependency-audit/selection")
    public ResponseEntity<WorkspaceResetDependencyAuditService.SelectionAudit> inspectSelection(
            @PathVariable Long workspaceId, @RequestBody SelectionRequest request) {
        if(request==null)
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,"Categories required");
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(audit.inspectSelection(workspaceId,request.categories()));
    }

    /** Read-only implementation gap and FK closure matrix, never an execute grant. */
    @GetMapping("/restore-readiness")
    public ResponseEntity<WorkspaceResetDependencyAuditService.RestoreReadiness> restoreReadiness(
            @PathVariable Long workspaceId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(audit.restoreReadiness(workspaceId));
    }

    @GetMapping("/dependency-audit")
    public ResponseEntity<WorkspaceResetDependencyAuditService.Audit> dependencyAudit(
            @PathVariable Long workspaceId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(audit.audit(workspaceId));
    }
}
