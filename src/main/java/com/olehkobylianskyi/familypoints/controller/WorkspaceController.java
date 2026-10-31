package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.dto.WorkspaceResponse;
import com.olehkobylianskyi.familypoints.dto.WorkspaceUpdateRequest;
import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import com.olehkobylianskyi.familypoints.service.WorkspaceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/workspaces")
public class WorkspaceController {
    private final WorkspaceService workspaceService;
    private final CurrentUserService currentUser;

    public WorkspaceController(WorkspaceService workspaceService, CurrentUserService currentUser) {
        this.workspaceService = workspaceService;
        this.currentUser = currentUser;
    }

    /**
     * Creation must go through the transactional, invitation-gated family
     * registration flow (not yet implemented). Never expose an arbitrary
     * workspace-creation endpoint to any signed-in child/parent.
     */
    @PostMapping
    public WorkspaceResponse createWorkspace() {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Use family registration to create a workspace");
    }

    /** Return only the current user's workspace; never enumerate other families. */
    @GetMapping
    public List<WorkspaceResponse> getAllWorkspaces() {
        Long id = currentUser.getCurrentWorkspaceId();
        return workspaceService.getWorkspaceById(id)
                .map(WorkspaceResponse::from)
                .map(List::of)
                .orElseGet(List::of);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@workspaceSecurity.canAccessWorkspace(#id)")
    public ResponseEntity<WorkspaceResponse> getWorkspace(@PathVariable Long id) {
        return workspaceService.getWorkspaceById(id)
                .map(WorkspaceResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("@workspaceSecurity.canManageWorkspace(#id)")
    public ResponseEntity<WorkspaceResponse> updateWorkspace(
            @PathVariable Long id,
            @Valid @RequestBody WorkspaceUpdateRequest request
    ) {
        return workspaceService.updateWorkspace(id, request.getName())
                .map(WorkspaceResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Destructive workspace deletion is disabled until a dedicated, audited
     * and reversible account/tenant deletion workflow has been implemented.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorkspace(@PathVariable Long id) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Workspace deletion is disabled");
    }
}
