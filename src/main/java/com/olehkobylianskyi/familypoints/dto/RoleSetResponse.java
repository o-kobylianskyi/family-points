package com.olehkobylianskyi.familypoints.dto;

import com.olehkobylianskyi.familypoints.entity.*;
import java.util.List;

public record RoleSetResponse(
        Long id,
        String systemCode,
        String name,
        String description,
        RoleVisibility visibility,
        RoleContextType ownerContextType,
        Long ownerContextId,
        boolean systemDefault,
        boolean active,
        List<Role> roles
) {
    public record Role(Long id, Long groupId, String groupName, String name, String description) {}

    public static RoleSetResponse from(RoleSet set, List<GroupRole> roles) {
        return new RoleSetResponse(
                set.getId(),
                set.getSystemCode(),
                set.getName(),
                set.getDescription(),
                set.getVisibility(),
                set.getOwnerContextType(),
                set.getOwnerContextId(),
                set.isSystemDefault(),
                set.isActive(),
                roles.stream().map(r -> new Role(
                        r.getId(),
                        r.getMemberGroup().getId(),
                        r.getMemberGroup().getName(),
                        r.getName(),
                        r.getDescription()
                )).toList()
        );
    }
}
