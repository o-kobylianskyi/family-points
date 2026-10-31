package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.service.WorkspaceSetupService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Initial family wizard metadata; no catalog import or activation side effects. */
@RestController
@RequestMapping("/workspaces/{workspaceId}/setup")
@PreAuthorize("@workspaceSecurity.canManageWorkspace(#workspaceId)")
public class WorkspaceSetupController {
    private final WorkspaceSetupService service;
    public WorkspaceSetupController(WorkspaceSetupService service) { this.service=service; }

    @GetMapping
    public ResponseEntity<WorkspaceSetupService.SetupState> current(@PathVariable Long workspaceId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.current(workspaceId));
    }

    @PatchMapping("/steps/{stepCode}")
    public ResponseEntity<WorkspaceSetupService.SetupState> setStep(
            @PathVariable Long workspaceId, @PathVariable String stepCode,
            @Valid @RequestBody WorkspaceSetupService.StepRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(service.setStep(workspaceId,stepCode,request));
    }

    public record CompleteRequest(Long expectedRevision) {}
    @PostMapping("/complete")
    public ResponseEntity<WorkspaceSetupService.SetupState> complete(
            @PathVariable Long workspaceId, @RequestBody CompleteRequest request) {
        if(request == null || request.expectedRevision()==null)
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,"SETUP_REVISION_REQUIRED");
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(service.complete(workspaceId,request.expectedRevision()));
    }
}
