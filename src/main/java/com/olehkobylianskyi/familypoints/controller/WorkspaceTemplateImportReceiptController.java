package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.service.WorkspaceTemplateImportReceiptService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

/**
 * Additive API. Existing /imports and /imports/preview contracts remain intact.
 * Never activates a task/reward or posts points.
 */
@RestController
@RequestMapping("/workspaces/{workspaceId}/templates/imports")
@PreAuthorize("@workspaceSecurity.canManageWorkspace(#workspaceId)")
public class WorkspaceTemplateImportReceiptController {
    private final WorkspaceTemplateImportReceiptService imports;

    public WorkspaceTemplateImportReceiptController(WorkspaceTemplateImportReceiptService imports) {
        this.imports=imports;
    }

    @PostMapping("/idempotent")
    public ResponseEntity<WorkspaceTemplateImportReceiptService.Result> importOnce(
            @PathVariable Long workspaceId,
            @RequestHeader("Idempotency-Key") UUID operationId,
            @Valid @RequestBody WorkspaceTemplateController.ImportRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(imports.importOnce(workspaceId,operationId,request));
    }

    @GetMapping("/receipts/{operationId}")
    public ResponseEntity<WorkspaceTemplateImportReceiptService.Result> receipt(
            @PathVariable Long workspaceId,@PathVariable UUID operationId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(imports.lookup(workspaceId,operationId));
    }
}
