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

    public record IssueRequest(Long roleId) {}
    public record AcceptRequest(String handle, String memberName) {}

    @PostMapping("/accept")
    @PreAuthorize("isAuthenticated()")
    public java.util.Map<String, Long> accept(@RequestBody AcceptRequest body) {
        return java.util.Map.of("workspaceId", invitations.accept(body.handle(), body.memberName()));
    }

    @PostMapping("/workspaces/{workspaceId}")
    @PreAuthorize("@workspaceSecurity.canManageMembers(#workspaceId)")
    public WorkspaceInvitationService.Invitation issue(@PathVariable Long workspaceId, @RequestBody IssueRequest body) {
        return invitations.issue(workspaceId, body.roleId());
    }

    // Authenticated preview for DEV; anonymous landing requires separate abuse protection.
    public record PreviewRequest(String token) {}

    @PostMapping("/preview")
    @PreAuthorize("isAuthenticated()")
    public WorkspaceInvitationService.Preview preview(@RequestBody PreviewRequest request) {
        return invitations.preview(request.token());
    }

    public record ContextRequest(String value) {}

    @PostMapping("/context")
    @PreAuthorize("isAuthenticated()")
    public WorkspaceInvitationService.PendingContext begin(@RequestBody ContextRequest request) {
        return invitations.begin(request.value());
    }

    @PostMapping("/context/resume")
    @PreAuthorize("isAuthenticated()")
    public WorkspaceInvitationService.Preview resume(@RequestBody ContextRequest request) {
        return invitations.resume(request.value());
    }

    @DeleteMapping("/workspaces/{workspaceId}/{invitationId}")
    @PreAuthorize("@workspaceSecurity.canManageMembers(#workspaceId)")
    public void revoke(@PathVariable Long workspaceId, @PathVariable Long invitationId) {
        invitations.revoke(workspaceId, invitationId);
    }
}
