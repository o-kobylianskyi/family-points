package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.entity.WorkspacePermission;
import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import com.olehkobylianskyi.familypoints.security.WorkspaceMembershipResolver;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Read-only preview for an account's verified membership in a workspace. */
@RestController
@RequestMapping("/api/me/workspaces")
public class WorkspaceMembershipContextController {
    private final CurrentUserService currentUser;
    private final WorkspaceMembershipResolver resolver;

    public WorkspaceMembershipContextController(CurrentUserService currentUser,
                                                WorkspaceMembershipResolver resolver) {
        this.currentUser = currentUser;
        this.resolver = resolver;
    }

    public record MembershipContext(Long workspaceId, String workspaceName,
                                    Long memberId, String memberName,
                                    String roleCode, List<String> permissions) {}

    @GetMapping("/{workspaceId}/context")
    @Transactional(readOnly = true)
    public MembershipContext context(@PathVariable Long workspaceId) {
        var account = currentUser.getCurrentAccount();
        var member = resolver.requireActiveMember(account, workspaceId);
        var role = member.getWorkspaceRole();
        if (role == null || !workspaceId.equals(role.getWorkspace().getId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Workspace access denied");
        }
        var permissions = role.getPermissions().stream()
                .map(WorkspacePermission::name)
                .sorted()
                .toList();
        return new MembershipContext(workspaceId, member.getWorkspace().getName(),
                member.getId(), member.getName(), role.getCode(), permissions);
    }
}
