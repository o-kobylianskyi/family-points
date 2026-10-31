package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.entity.UserAccount;
import com.olehkobylianskyi.familypoints.repository.AccountWorkspaceMembershipRepository;
import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import com.olehkobylianskyi.familypoints.security.JwtService;
import com.olehkobylianskyi.familypoints.security.WorkspaceSessionService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** Explicit workspace selection: JWT is issued only for verified active membership. */
@RestController
@RequestMapping("/auth/workspaces")
public class WorkspaceSessionController {
    private final CurrentUserService currentUser;
    private final AccountWorkspaceMembershipRepository memberships;
    private final WorkspaceSessionService sessions;
    private final JwtService tokens;

    public WorkspaceSessionController(CurrentUserService currentUser,
            AccountWorkspaceMembershipRepository memberships,
            WorkspaceSessionService sessions, JwtService tokens) {
        this.currentUser = currentUser;
        this.memberships = memberships;
        this.sessions = sessions;
        this.tokens = tokens;
    }

    public record WorkspaceOption(Long workspaceId, Long memberId, String name) {}
    public record SelectedWorkspace(String accessToken, String tokenType,
                                    long expiresInSeconds, Long workspaceId, Long memberId) {}

    @GetMapping
    @Transactional(readOnly = true)
    public List<WorkspaceOption> mine() {
        UserAccount account = currentUser.getCurrentAccount();
        return memberships.findAllByAccountIdOrderByIdAsc(account.getId()).stream()
                .filter(link -> link.getWorkspaceMember().isActive()
                        && link.getWorkspaceMember().getDeletedAt() == null)
                .map(link -> new WorkspaceOption(link.getWorkspace().getId(),
                        link.getWorkspaceMember().getId(), link.getWorkspace().getName()))
                .toList();
    }

    @PostMapping("/{workspaceId}/select")
    public SelectedWorkspace select(@PathVariable Long workspaceId, Authentication authentication) {
        UserAccount account = currentUser.getCurrentAccount();
        var session = sessions.requireSession(account, workspaceId);
        UserDetails principal = (UserDetails) authentication.getPrincipal();
        return new SelectedWorkspace(tokens.generateWorkspaceAccessToken(principal, workspaceId),
                "Bearer", tokens.getAccessTokenExpirationSeconds(), workspaceId, session.memberId());
    }
}
