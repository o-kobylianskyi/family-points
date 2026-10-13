package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.service.WorkspaceInvitationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workspace-invitations")
public class WorkspaceInvitationController {
    private final WorkspaceInvitationService invitations;

    public WorkspaceInvitationController(WorkspaceInvitationService invitations) {
        this.invitations = invitations;
    }

    @PostMapping("/workspaces/{workspaceId}")
    @PreAuthorize("@workspaceSecurity.canManageMembers(#workspaceId)")
    public WorkspaceInvitationService.Invitation issue(@PathVariable Long workspaceId) {
        return invitations.issue(workspaceId);
    }

    // Authenticated preview for DEV; anonymous landing requires separate abuse protection.
    public record PreviewRequest(String token) {}

    @PostMapping("/preview")
    @PreAuthorize("isAuthenticated()")
    public WorkspaceInvitationService.Preview preview(@RequestBody PreviewRequest request) {
        return invitations.preview(request.token());
    }

    @DeleteMapping("/workspaces/{workspaceId}/{invitationId}")
    @PreAuthorize("@workspaceSecurity.canManageMembers(#workspaceId)")
    public void revoke(@PathVariable Long workspaceId, @PathVariable Long invitationId) {
        invitations.revoke(workspaceId, invitationId);
    }
}
