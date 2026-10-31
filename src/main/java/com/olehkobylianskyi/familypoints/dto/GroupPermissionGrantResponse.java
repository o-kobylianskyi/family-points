package com.olehkobylianskyi.familypoints.dto;

import com.olehkobylianskyi.familypoints.entity.*;

public record GroupPermissionGrantResponse(
        Long id,
        Long roleId,
        GroupPermission permission,
        GroupPermissionScope scope
) {
    public static GroupPermissionGrantResponse from(GroupPermissionGrant grant) {
        return new GroupPermissionGrantResponse(
                grant.getId(),
                grant.getGroupRole().getId(),
                grant.getPermission(),
                grant.getScope()
        );
    }

    public static GroupPermissionGrantResponse from(RolePermissionGrant grant) {
        return new GroupPermissionGrantResponse(
                grant.getId(),
                grant.getRoleDefinition().getId(),
                grant.getPermission(),
                grant.getScope() == RolePermissionScope.SUBTREE
                        ? GroupPermissionScope.GROUP_SUBTREE
                        : GroupPermissionScope.GROUP
        );
    }
}
