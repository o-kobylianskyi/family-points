package com.olehkobylianskyi.familypoints.security;

import org.springframework.stereotype.Component;

@Component("workspaceSecurity")
public class WorkspaceSecurity {

    private final CurrentUserService currentUserService;

    public WorkspaceSecurity(
            CurrentUserService currentUserService
    ) {
        this.currentUserService = currentUserService;
    }

    public boolean canAccessWorkspace(Long workspaceId) {
        return currentUserService.belongsToWorkspace(workspaceId);
    }
}