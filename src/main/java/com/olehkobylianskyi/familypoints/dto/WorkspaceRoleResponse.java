package com.olehkobylianskyi.familypoints.dto;

import com.olehkobylianskyi.familypoints.entity.WorkspacePermission;
import com.olehkobylianskyi.familypoints.entity.WorkspaceRole;

import java.util.Set;

public record WorkspaceRoleResponse(
        Long id,
        String code,
        String name,
        boolean systemRole,
        Set<WorkspacePermission> permissions
) {
    public static WorkspaceRoleResponse from(WorkspaceRole role) {
        return new WorkspaceRoleResponse(
                role.getId(),
                role.getCode(),
                role.getName(),
                role.isSystemRole(),
                Set.copyOf(role.getPermissions())
        );
    }
}