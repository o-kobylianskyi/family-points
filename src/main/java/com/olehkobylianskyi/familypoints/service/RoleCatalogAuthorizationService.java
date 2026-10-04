package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.WorkspaceMember;
import com.olehkobylianskyi.familypoints.entity.WorkspacePermission;
import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class RoleCatalogAuthorizationService {

    private final CurrentUserService currentUserService;

    public RoleCatalogAuthorizationService(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    public void requireWorkspace(Long workspaceId) {
        if (!currentUserService.belongsToWorkspace(workspaceId)) {
            throw new AccessDeniedException("Current user cannot access this workspace");
        }
    }

    public void requireManage(Long workspaceId) {
        requireWorkspace(workspaceId);
        WorkspaceMember member = currentUserService.getCurrentAccount().getWorkspaceMember();
        if (!member.hasPermission(WorkspacePermission.MANAGE_ROLES)
                && !member.hasPermission(WorkspacePermission.ADMIN_OVERRIDE)) {
            throw new AccessDeniedException("Current user cannot manage the shared role catalog");
        }
    }
}
