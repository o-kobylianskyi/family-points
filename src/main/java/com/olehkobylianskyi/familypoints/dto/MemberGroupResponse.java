package com.olehkobylianskyi.familypoints.dto;

import com.olehkobylianskyi.familypoints.entity.*;
import java.util.*;

public record MemberGroupResponse(
        Long id,
        String name,
        String description,
        boolean active,
        boolean showInNavigation,
        List<Role> roles,
        List<Member> members,
        List<ChildGroup> childGroups,
        List<Long> parentGroupIds,
        List<Balance> balances
) {
    public record Role(
            Long id,
            String systemCode,
            String name,
            String description,
            Long roleSetId,
            RoleVisibility visibility,
            boolean systemDefault
    ) {}

    public record Member(
            Long membershipId,
            Long memberId,
            String memberName,
            boolean active,
            List<Long> roleIds
    ) {}

    public record ChildGroup(Long compositionId, Long groupId, String groupName) {}
    public record Balance(Long pointTypeId, String code, String name, long amount) {}

    public static MemberGroupResponse fromCatalog(
            MemberGroup group,
            List<RoleDefinition> availableRoles,
            List<GroupMembership> memberships,
            List<RoleAssignment> assignments,
            List<GroupComposition> children,
            List<GroupComposition> parents,
            List<Balance> balances
    ) {
        Map<Long, List<Long>> rolesByMember = new HashMap<>();

        for (RoleAssignment assignment : assignments) {
            if (!assignment.isActive() || assignment.getActorType() != ActorType.MEMBER) continue;
            rolesByMember
                    .computeIfAbsent(assignment.getActorId(), ignored -> new ArrayList<>())
                    .add(assignment.getRoleDefinition().getId());
        }

        rolesByMember.values().forEach(list -> list.sort(Long::compareTo));

        return new MemberGroupResponse(
                group.getId(),
                group.getName(),
                group.getDescription(),
                group.isActive(),
                group.isShowInNavigation(),
                availableRoles.stream()
                        .map(role -> new Role(
                                role.getId(),
                                role.getSystemCode(),
                                role.getName(),
                                role.getDescription(),
                                role.getRoleSet() == null ? null : role.getRoleSet().getId(),
                                role.getVisibility(),
                                role.isSystemDefault()
                        ))
                        .toList(),
                memberships.stream()
                        .map(membership -> new Member(
                                membership.getId(),
                                membership.getMember().getId(),
                                membership.getMember().getName(),
                                membership.isActive(),
                                rolesByMember.getOrDefault(membership.getMember().getId(), List.of())
                        ))
                        .toList(),
                children.stream()
                        .map(c -> new ChildGroup(
                                c.getId(),
                                c.getChildGroup().getId(),
                                c.getChildGroup().getName()
                        ))
                        .toList(),
                parents.stream()
                        .map(c -> c.getParentGroup().getId())
                        .toList(),
                balances == null ? List.of() : balances
        );
    }
}
