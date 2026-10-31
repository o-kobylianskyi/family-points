package com.olehkobylianskyi.familypoints.dto;

import com.olehkobylianskyi.familypoints.entity.RoleDefinition;
import com.olehkobylianskyi.familypoints.entity.RoleContextType;
import com.olehkobylianskyi.familypoints.entity.RoleVisibility;

public record RoleDefinitionResponse(
        Long id,
        Long roleSetId,
        String systemCode,
        String name,
        String description,
        RoleVisibility visibility,
        RoleContextType ownerContextType,
        Long ownerContextId,
        boolean systemDefault,
        boolean active
) {
    public static RoleDefinitionResponse from(RoleDefinition role) {
        return new RoleDefinitionResponse(
                role.getId(),
                role.getRoleSet() == null ? null : role.getRoleSet().getId(),
                role.getSystemCode(),
                role.getName(),
                role.getDescription(),
                role.getVisibility(),
                role.getOwnerContextType(),
                role.getOwnerContextId(),
                role.isSystemDefault(),
                role.isActive()
        );
    }
}
