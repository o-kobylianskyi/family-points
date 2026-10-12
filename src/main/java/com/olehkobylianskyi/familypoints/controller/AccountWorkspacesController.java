package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.repository.AccountWorkspaceMembershipRepository;
import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/me/workspaces")
public class AccountWorkspacesController {
    private final CurrentUserService currentUser;
    private final AccountWorkspaceMembershipRepository memberships;

    public AccountWorkspacesController(CurrentUserService currentUser,
                                       AccountWorkspaceMembershipRepository memberships) {
        this.currentUser = currentUser;
        this.memberships = memberships;
    }

    public record WorkspaceChoice(Long workspaceId, String workspaceName,
                                  Long memberId, String memberName, boolean active) {}

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<WorkspaceChoice>> list() {
        var account = currentUser.getCurrentAccount();
        var activeWorkspaceId = currentUser.getCurrentWorkspaceId();
        var choices = memberships.findAllByAccountIdOrderByIdAsc(account.getId())
                .stream()
                .filter(link -> link.getWorkspaceMember().isActive())
                .map(link -> {
                    var member = link.getWorkspaceMember();
                    var workspace = member.getWorkspace();
                    return new WorkspaceChoice(workspace.getId(), workspace.getName(),
                            member.getId(), member.getName(), workspace.getId().equals(activeWorkspaceId));
                })
                .toList();
        return ResponseEntity.ok(choices);
    }
}
