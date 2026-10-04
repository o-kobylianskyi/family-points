package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.*;
import com.olehkobylianskyi.familypoints.repository.RoleAssignmentRepository;
import com.olehkobylianskyi.familypoints.repository.RoleDefinitionRepository;
import com.olehkobylianskyi.familypoints.repository.RolePermissionGrantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * Temporary compatibility bridge while GroupEditor/API still use legacy GroupRole.
 *
 * The new role catalog is the target model, but legacy role tables remain active until
 * services/frontend are switched and smoke-tested. This service keeps both models in sync.
 */
@Service
public class LegacyGroupRoleBridgeService {

    private final RoleDefinitionRepository roleDefinitions;
    private final RoleAssignmentRepository roleAssignments;
    private final RolePermissionGrantRepository rolePermissionGrants;

    public LegacyGroupRoleBridgeService(
            RoleDefinitionRepository roleDefinitions,
            RoleAssignmentRepository roleAssignments,
            RolePermissionGrantRepository rolePermissionGrants
    ) {
        this.roleDefinitions = roleDefinitions;
        this.roleAssignments = roleAssignments;
        this.rolePermissionGrants = rolePermissionGrants;
    }

    @Transactional
    public RoleDefinition syncRole(GroupRole legacyRole) {
        MemberGroup group = legacyRole.getMemberGroup();
        Workspace workspace = group.getWorkspace();

        RoleDefinition role = roleDefinitions.findByLegacyGroupRoleId(legacyRole.getId())
                .orElseGet(() -> {
                    RoleDefinition created = new RoleDefinition(
                            workspace,
                            legacyRole.getRoleSet(),
                            legacyRole.getName(),
                            legacyRole.getDescription(),
                            RoleVisibility.PRIVATE,
                            RoleContextType.MEMBER_GROUP,
                            group.getId()
                    );
                    created.setLegacyGroupRoleId(legacyRole.getId());
                    return created;
                });

        role.setRoleSet(legacyRole.getRoleSet());
        role.setName(legacyRole.getName());
        role.setDescription(legacyRole.getDescription());
        role.setVisibility(RoleVisibility.PRIVATE);
        role.setActive(true);

        return roleDefinitions.save(role);
    }

    @Transactional
    public RolePermissionGrant syncPermissionGrant(GroupPermissionGrant legacyGrant) {
        RoleDefinition role = syncRole(legacyGrant.getGroupRole());

        RolePermissionGrant grant = rolePermissionGrants
                .findByLegacyGroupPermissionGrantId(legacyGrant.getId())
                .orElseGet(() -> {
                    RolePermissionGrant created = new RolePermissionGrant(
                            role,
                            legacyGrant.getPermission(),
                            toRoleScope(legacyGrant.getScope())
                    );
                    created.setLegacyGroupPermissionGrantId(legacyGrant.getId());
                    return created;
                });

        grant.setActive(true);
        return rolePermissionGrants.save(grant);
    }

    @Transactional
    public void removePermissionGrant(GroupPermissionGrant legacyGrant) {
        rolePermissionGrants.findByLegacyGroupPermissionGrantId(legacyGrant.getId())
                .ifPresent(rolePermissionGrants::delete);
    }

    @Transactional
    public void removeRoleBridge(GroupRole legacyRole) {
        roleDefinitions.findByLegacyGroupRoleId(legacyRole.getId()).ifPresent(role -> {
            rolePermissionGrants.deleteAll(rolePermissionGrants.findByRoleDefinitionId(role.getId()));
            roleAssignments.deleteAll(roleAssignments.findByRoleDefinitionId(role.getId()));
            roleDefinitions.delete(role);
        });
    }

    private RolePermissionScope toRoleScope(GroupPermissionScope scope) {
        return scope == GroupPermissionScope.GROUP_SUBTREE
                ? RolePermissionScope.SUBTREE
                : RolePermissionScope.CURRENT;
    }

    @Transactional
    public void syncMembership(GroupMembership membership) {
        MemberGroup group = membership.getMemberGroup();
        Workspace workspace = group.getWorkspace();
        WorkspaceMember member = membership.getMember();

        Set<Long> desiredRoleDefinitionIds = new HashSet<>();

        if (membership.isActive()) {
            for (GroupRole legacyRole : membership.getRoles()) {
                RoleDefinition role = syncRole(legacyRole);
                desiredRoleDefinitionIds.add(role.getId());

                RoleAssignment assignment = roleAssignments
                        .findByWorkspaceIdAndRoleDefinitionIdAndActorTypeAndActorIdAndContextTypeAndContextId(
                                workspace.getId(),
                                role.getId(),
                                ActorType.MEMBER,
                                member.getId(),
                                RoleContextType.MEMBER_GROUP,
                                group.getId()
                        )
                        .orElseGet(() -> new RoleAssignment(
                                workspace,
                                role,
                                ActorType.MEMBER,
                                member.getId(),
                                RoleContextType.MEMBER_GROUP,
                                group.getId()
                        ));

                assignment.setActive(true);
                roleAssignments.save(assignment);
            }
        }

        for (RoleAssignment assignment : roleAssignments
                .findByWorkspaceIdAndActorTypeAndActorIdAndActiveTrue(
                        workspace.getId(),
                        ActorType.MEMBER,
                        member.getId()
                )) {
            if (assignment.getContextType() != RoleContextType.MEMBER_GROUP
                    || !group.getId().equals(assignment.getContextId())) {
                continue;
            }

            RoleDefinition role = assignment.getRoleDefinition();
            if (role.getLegacyGroupRoleId() == null) {
                continue;
            }

            if (!desiredRoleDefinitionIds.contains(role.getId())) {
                assignment.setActive(false);
                roleAssignments.save(assignment);
            }
        }
    }
}
