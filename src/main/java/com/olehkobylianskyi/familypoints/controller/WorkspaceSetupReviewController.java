package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.service.WorkspaceSetupService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.List;

/**
 * Read-only onboarding review. Does not create a group, private template, task,
 * reward, purchase, or ledger transaction. The reviewed selection is NOT a commit token.
 */
@RestController
@RequestMapping("/workspaces/{workspaceId}/setup")
@PreAuthorize("@workspaceSecurity.canManageWorkspace(#workspaceId)")
public class WorkspaceSetupReviewController {
    private final WorkspaceSetupService setup;
    private final WorkspaceTemplateController templates;

    public WorkspaceSetupReviewController(WorkspaceSetupService setup,
                                          WorkspaceTemplateController templates) {
        this.setup = setup;
        this.templates = templates;
    }

    public record ReviewRequest(@NotNull Long expectedRevision,
                                @NotNull List<String> selectedCatalogCodes) {}
    public record ReviewResult(Long workspaceId, long dataEpoch, long revision,
                               String setupStatus,
                               int selectedCount, int alreadyImportedCount, int toImportCount,
                               List<String> selectedCodes, List<String> alreadyImported,
                               List<String> toImport, boolean requiresExplicitConfirmation,
                               boolean createsActiveDefinitions) {}

    @PostMapping("/review")
    public ResponseEntity<ReviewResult> review(@PathVariable Long workspaceId,
                                               @Valid @RequestBody ReviewRequest request) {
        if (request == null || request.expectedRevision() == null
                || request.selectedCatalogCodes() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SETUP_REVIEW_INVALID");
        var state = setup.current(workspaceId);
        if (state.revision() != request.expectedRevision())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "SETUP_REVISION_STALE");
        if ("COMPLETED".equals(state.status()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "SETUP_ALREADY_COMPLETED");
        // Delegate validation to the same source used by actual import. In
        // particular, unknown/duplicate codes cannot pass this dry run.
        var preview = templates.preview(workspaceId,
                new WorkspaceTemplateController.ImportRequest(request.selectedCatalogCodes()));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new ReviewResult(
                workspaceId, state.dataEpoch(), state.revision(), state.status(),
                preview.selectedCodes().size(), preview.alreadyImported().size(),
                preview.toImport().size(), preview.selectedCodes(), preview.alreadyImported(),
                preview.toImport(), true, false));
    }
}
