package com.olehkobylianskyi.familypoints.dto;

import com.olehkobylianskyi.familypoints.entity.*;
import java.util.List;

public record RoleSetResponse(Long id, String name, String description, List<Role> roles) {
    public record Role(Long id, Long groupId, String groupName, String name, String description) {}

    public static RoleSetResponse from(RoleSet set, List<GroupRole> roles) {
        return new RoleSetResponse(set.getId(), set.getName(), set.getDescription(),
                roles.stream().map(r -> new Role(r.getId(), r.getMemberGroup().getId(),
                        r.getMemberGroup().getName(), r.getName(), r.getDescription())).toList());
    }
}
