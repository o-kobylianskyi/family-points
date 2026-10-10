package com.olehkobylianskyi.familypoints.security;

import org.springframework.stereotype.Component;
import com.olehkobylianskyi.familypoints.entity.WorkspacePermission;

@Component("workspaceSecurity")
public class WorkspaceSecurity {

    private final CurrentUserService currentUserService;

    public WorkspaceSecurity(
            CurrentUserService currentUserService
    ) {
        this.currentUserService = currentUserService;
    }

    public boolean canManagePoints(Long workspaceId) {
        if (!currentUserService.belongsToWorkspace(workspaceId)) return false;
        var member = currentUserService.getCurrentAccount().getWorkspaceMember();
        return member.hasPermission(WorkspacePermission.MANAGE_POINTS)
                || member.hasPermission(WorkspacePermission.ADMIN_OVERRIDE);
    }

    public boolean canAccessWorkspace(Long workspaceId) {
        return currentUserService.belongsToWorkspace(workspaceId);
    }
}