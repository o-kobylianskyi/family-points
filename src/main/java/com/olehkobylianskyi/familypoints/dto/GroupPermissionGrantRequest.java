package com.olehkobylianskyi.familypoints.dto;

import com.olehkobylianskyi.familypoints.entity.GroupPermission;
import com.olehkobylianskyi.familypoints.entity.GroupPermissionScope;
import jakarta.validation.constraints.NotNull;

public class GroupPermissionGrantRequest {
    @NotNull private GroupPermission permission;
    @NotNull private GroupPermissionScope scope;

    public GroupPermission getPermission() { return permission; }
    public void setPermission(GroupPermission permission) { this.permission = permission; }
    public GroupPermissionScope getScope() { return scope; }
    public void setScope(GroupPermissionScope scope) { this.scope = scope; }
}
