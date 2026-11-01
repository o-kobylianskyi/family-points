package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.service.WorkspaceSetupFinalizeService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

/** Explicit finalization: only private catalog import + durable setup completion. */
@RestController
@RequestMapping("/workspaces/{workspaceId}/setup")
@PreAuthorize("@workspaceSecurity.canManageWorkspace(#workspaceId)")
public class WorkspaceSetupFinalizeController {
    private final WorkspaceSetupFinalizeService service;
    public WorkspaceSetupFinalizeController(WorkspaceSetupFinalizeService service){this.service=service;}

    @PostMapping("/finalize-import")
    public ResponseEntity<WorkspaceSetupFinalizeService.Result> finalizeImport(
            @PathVariable Long workspaceId,
            @RequestHeader("Idempotency-Key") UUID operationId,
            @Valid @RequestBody WorkspaceSetupFinalizeService.Request request){
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(service.finalizeWizard(workspaceId,operationId,request));
    }

    @GetMapping("/finalize-import/receipts/{operationId}")
    public ResponseEntity<WorkspaceSetupFinalizeService.Result> receipt(
            @PathVariable Long workspaceId,@PathVariable UUID operationId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(service.receipt(workspaceId,operationId));
    }
}
