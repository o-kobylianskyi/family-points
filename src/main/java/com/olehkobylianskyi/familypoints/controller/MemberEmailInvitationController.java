package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.service.MemberEmailInvitationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class MemberEmailInvitationController {
    private final MemberEmailInvitationService invitations;
    public MemberEmailInvitationController(MemberEmailInvitationService invitations) {
        this.invitations = invitations;
    }

    public record IssueRequest(@NotBlank @Email String email) {}
    public record AcceptRequest(@NotBlank String token,
                                @NotBlank String username,
                                @NotBlank @Size(min = 8, max = 128) String password) {}

    @PostMapping("/workspaces/{workspaceId}/members/{memberId}/email-invitation")
    @PreAuthorize("hasAuthority('MANAGE_MEMBERS') && @workspaceSecurity.canAccessWorkspace(#workspaceId)")
    public void issue(@PathVariable Long workspaceId, @PathVariable Long memberId,
                      @Valid @RequestBody IssueRequest request) {
        invitations.issue(workspaceId, memberId, request.email());
    }

    public record AcceptGoogleRequest(@NotBlank String token, @NotBlank String idToken) {}

    public record CheckRequest(@NotBlank String token) {}
    public record CheckResponse(String status) {}

    @PostMapping("/auth/member-invitations/check")
    public CheckResponse check(@Valid @RequestBody CheckRequest request) {
        return new CheckResponse(invitations.invitationStatus(request.token()));
    }

    @PostMapping("/auth/member-invitations/accept-google")
    public void acceptGoogle(@Valid @RequestBody AcceptGoogleRequest request) {
        invitations.acceptWithGoogle(request.token(), request.idToken());
    }

    @PostMapping("/auth/member-invitations/accept")
    public void accept(@Valid @RequestBody AcceptRequest request) {
        invitations.accept(request.token(), request.username(), request.password());
    }
}
