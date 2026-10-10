package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.dto.GroupPermissionGrantResponse;
import com.olehkobylianskyi.familypoints.dto.MemberGroupResponse;
import com.olehkobylianskyi.familypoints.entity.*;
import com.olehkobylianskyi.familypoints.exception.ResourceNotFoundException;
import com.olehkobylianskyi.familypoints.exception.InsufficientPointsException;
import com.olehkobylianskyi.familypoints.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class MemberGroupService {

    private final WorkspaceRepository workspaces;
    private final WorkspaceMemberRepository members;
    private final MemberGroupRepository groups;
    private final GroupMembershipRepository memberships;
    private final GroupCompositionRepository compositions;
    private final PointTypeRepository pointTypes;
    private final GroupPointTransactionRepository groupLedger;
    private final RoleSetRepository roleSets;
    private final RoleDefinitionRepository roleDefinitions;
    private final RoleAssignmentRepository roleAssignments;
    private final RolePermissionGrantRepository rolePermissionGrants;

    public MemberGroupService(
            WorkspaceRepository workspaces,
            WorkspaceMemberRepository members,
            MemberGroupRepository groups,
            GroupMembershipRepository memberships,
            GroupCompositionRepository compositions,
            PointTypeRepository pointTypes,
            GroupPointTransactionRepository groupLedger,
            RoleSetRepository roleSets,
            RoleDefinitionRepository roleDefinitions,
            RoleAssignmentRepository roleAssignments,
            RolePermissionGrantRepository rolePermissionGrants
    ) {
        this.workspaces = workspaces;
        this.members = members;
        this.groups = groups;
        this.memberships = memberships;
        this.compositions = compositions;
        this.pointTypes = pointTypes;
        this.groupLedger = groupLedger;
        this.roleSets = roleSets;
        this.roleDefinitions = roleDefinitions;
        this.roleAssignments = roleAssignments;
        this.rolePermissionGrants = rolePermissionGrants;
    }

    @Transactional
    public MemberGroupResponse createGroup(Long workspaceId, String name, String description) {
        Workspace workspace = workspace(workspaceId);
        return response(groups.save(new MemberGroup(workspace, name.trim(), blank(description))));
    }

    @Transactional(readOnly = true)
    public List<MemberGroupResponse> getGroups(Long workspaceId) {
        workspace(workspaceId);
        return groups.findByWorkspaceIdAndActiveTrueOrderByNameAsc(workspaceId)
                .stream()
                .map(this::response)
                .toList();
    }

    @Transactional
    public MemberGroupResponse updateGroup(
            Long workspaceId,
            Long groupId,
            String name,
            String description,
            boolean showInNavigation
    ) {
        MemberGroup group = group(workspaceId, groupId);
        group.setName(name.trim());
        group.setDescription(blank(description));
        group.setShowInNavigation(showInNavigation);
        return response(groups.save(group));
    }

    @Transactional
    public void deactivateGroup(Long workspaceId, Long groupId) {
        MemberGroup group = group(workspaceId, groupId);
        group.setActive(false);
        groups.save(group);

        roleAssignments
                .findByWorkspaceIdAndContextTypeAndContextIdAndActiveTrue(
                        workspaceId, RoleContextType.MEMBER_GROUP, groupId)
                .forEach(assignment -> assignment.setActive(false));
    }

    /**
     * Creates a genuinely group-local role. Shared/system roles are never copied
     * into a group; they are returned automatically by availableRoles().
     */
    @Transactional
    public MemberGroupResponse addRole(
            Long workspaceId,
            Long groupId,
            String name,
            String description,
            Long roleSetId
    ) {
        MemberGroup group = group(workspaceId, groupId);
        String normalizedName = name.trim();

        boolean duplicate = localRoles(group).stream()
                .anyMatch(role -> role.getName().equalsIgnoreCase(normalizedName));
        if (duplicate) {
            throw new IllegalArgumentException("Local role already exists: " + normalizedName);
        }

        RoleSet roleSet = roleSetId == null
                ? null
                : roleSets.findByIdAndWorkspaceId(roleSetId, workspaceId)
                        .orElseThrow(() -> new ResourceNotFoundException("Role set not found: " + roleSetId));

        RoleDefinition role = new RoleDefinition(
                group.getWorkspace(),
                roleSet,
                normalizedName,
                blank(description),
                RoleVisibility.PRIVATE,
                RoleContextType.MEMBER_GROUP,
                groupId
        );

        roleDefinitions.save(role);
        return response(group);
    }

    @Transactional
    public MemberGroupResponse updateRole(
            Long workspaceId,
            Long groupId,
            Long roleId,
            String name,
            String description,
            Long roleSetId
    ) {
        MemberGroup group = group(workspaceId, groupId);
        RoleDefinition role = localRole(workspaceId, groupId, roleId);

        String normalizedName = name.trim();
        boolean duplicate = localRoles(group).stream()
                .anyMatch(other -> !other.getId().equals(roleId)
                        && other.getName().equalsIgnoreCase(normalizedName));
        if (duplicate) {
            throw new IllegalArgumentException("Local role already exists: " + normalizedName);
        }

        role.setName(normalizedName);
        role.setDescription(blank(description));
        role.setRoleSet(roleSetId == null
                ? null
                : roleSets.findByIdAndWorkspaceId(roleSetId, workspaceId)
                        .orElseThrow(() -> new ResourceNotFoundException("Role set not found: " + roleSetId)));

        roleDefinitions.save(role);
        return response(group);
    }

    @Transactional
    public MemberGroupResponse deleteRole(Long workspaceId, Long groupId, Long roleId) {
        MemberGroup group = group(workspaceId, groupId);
        RoleDefinition role = localRole(workspaceId, groupId, roleId);

        roleAssignments.findByRoleDefinitionId(roleId).forEach(assignment -> assignment.setActive(false));
        rolePermissionGrants.findByRoleDefinitionId(roleId).forEach(grant -> grant.setActive(false));
        role.setActive(false);
        roleDefinitions.save(role);

        return response(group);
    }

    @Transactional
    public MemberGroupResponse updateMemberRoles(
            Long workspaceId,
            Long groupId,
            Long memberId,
            Set<Long> roleIds
    ) {
        return addMember(workspaceId, groupId, memberId, roleIds);
    }

    @Transactional
    public MemberGroupResponse addMember(
            Long workspaceId,
            Long groupId,
            Long memberId,
            Set<Long> roleIds
    ) {
        MemberGroup group = group(workspaceId, groupId);
        WorkspaceMember member = members.findByIdAndWorkspaceId(memberId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace member not found: " + memberId));

        GroupMembership membership = memberships
                .findByMemberGroupIdAndMemberId(groupId, memberId)
                .orElseGet(() -> new GroupMembership(group, member));

        membership.setActive(true);
        // Legacy role links are intentionally cleared. RoleAssignment is now the source of truth.
        membership.getRoles().clear();
        memberships.save(membership);

        replaceMemberRoleAssignments(workspaceId, groupId, memberId, roleIds == null ? Set.of() : roleIds);
        return response(group);
    }

    @Transactional
    public MemberGroupResponse removeMember(Long workspaceId, Long groupId, Long memberId) {
        MemberGroup group = group(workspaceId, groupId);
        GroupMembership membership = memberships
                .findByMemberGroupIdAndMemberId(groupId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found"));

        membership.setActive(false);
        memberships.save(membership);

        roleAssignments
                .findByWorkspaceIdAndActorTypeAndActorIdAndContextTypeAndContextIdAndActiveTrue(
                        workspaceId,
                        ActorType.MEMBER,
                        memberId,
                        RoleContextType.MEMBER_GROUP,
                        groupId
                )
                .forEach(assignment -> assignment.setActive(false));

        return response(group);
    }

    @Transactional
    public MemberGroupResponse addChildGroup(Long workspaceId, Long parentId, Long childId) {
        if (parentId.equals(childId)) {
            throw new IllegalArgumentException("A group cannot contain itself");
        }

        MemberGroup parent = group(workspaceId, parentId);
        MemberGroup child = group(workspaceId, childId);

        if (reaches(childId, parentId, new HashSet<>())) {
            throw new IllegalArgumentException("Group cycle is not allowed");
        }

        GroupComposition composition = compositions
                .findByParentGroupIdAndChildGroupId(parentId, childId)
                .orElseGet(() -> new GroupComposition(parent, child));

        composition.setActive(true);
        compositions.save(composition);
        return response(parent);
    }

    @Transactional
    public MemberGroupResponse removeChildGroup(Long workspaceId, Long parentId, Long childId) {
        MemberGroup parent = group(workspaceId, parentId);
        GroupComposition composition = compositions
                .findByParentGroupIdAndChildGroupId(parentId, childId)
                .orElseThrow(() -> new ResourceNotFoundException("Group composition not found"));

        composition.setActive(false);
        compositions.save(composition);
        return response(parent);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> resolveActors(Long workspaceId, Long groupId) {
        group(workspaceId, groupId);

        LinkedHashSet<Long> groupIds = new LinkedHashSet<>();
        LinkedHashSet<Long> memberIds = new LinkedHashSet<>();
        collect(groupId, groupIds, memberIds, new HashSet<>());

        return Map.of(
                "rootGroupId", groupId,
                "groupIds", groupIds,
                "memberIds", memberIds
        );
    }

    @Transactional(readOnly = true)
    public List<GroupPermissionGrantResponse> getPermissionGrants(Long workspaceId, Long groupId) {
        MemberGroup group = group(workspaceId, groupId);
        List<GroupPermissionGrantResponse> result = new ArrayList<>();

        for (RoleDefinition role : availableRoles(group)) {
            rolePermissionGrants.findByRoleDefinitionIdAndActiveTrueOrderByIdAsc(role.getId())
                    .stream()
                    .map(GroupPermissionGrantResponse::from)
                    .forEach(result::add);
        }

        return result;
    }

    @Transactional
    public GroupPermissionGrantResponse addPermissionGrant(
            Long workspaceId,
            Long groupId,
            Long roleId,
            GroupPermission permission,
            GroupPermissionScope scope
    ) {
        group(workspaceId, groupId);
        RoleDefinition role = localRole(workspaceId, groupId, roleId);

        RolePermissionScope newScope = scope == GroupPermissionScope.GROUP_SUBTREE
                ? RolePermissionScope.SUBTREE
                : RolePermissionScope.CURRENT;

        boolean exists = rolePermissionGrants
                .findByRoleDefinitionIdAndPermissionAndActiveTrue(roleId, permission)
                .stream()
                .anyMatch(grant -> grant.getScope() == newScope);

        if (exists) {
            throw new IllegalArgumentException("Permission grant already exists");
        }

        RolePermissionGrant saved = rolePermissionGrants.save(
                new RolePermissionGrant(role, permission, newScope)
        );

        return GroupPermissionGrantResponse.from(saved);
    }

    @Transactional
    public void removePermissionGrant(Long workspaceId, Long groupId, Long grantId) {
        group(workspaceId, groupId);

        RolePermissionGrant grant = rolePermissionGrants.findById(grantId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission grant not found: " + grantId));

        localRole(workspaceId, groupId, grant.getRoleDefinition().getId());
        grant.setActive(false);
        rolePermissionGrants.save(grant);
    }

    @Transactional
    public MemberGroupResponse addPoints(
            Long workspaceId,
            Long groupId,
            Long pointTypeId,
            int amount,
            PointTransactionType type,
            String description
    ) {
        // Serialize balance changes for this group before reading the ledger.
        MemberGroup group = groups.findWithLockByIdAndWorkspaceId(groupId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + groupId));
        if (!group.isActive()) {
            throw new IllegalArgumentException("Group is not active");
        }

        PointType pointType = pointTypes.findByIdAndWorkspaceId(pointTypeId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Point type not found: " + pointTypeId));

        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (type != PointTransactionType.EARN && type != PointTransactionType.SPEND) {
            throw new IllegalArgumentException("Only EARN and SPEND are supported for manual group operations");
        }

        long signed = type == PointTransactionType.SPEND ? -(long) amount : (long) amount;
        long available = groupLedger.getBalance(groupId, pointTypeId);
        if (signed < 0 && available < -signed) {
            throw new InsufficientPointsException(-signed, available);
        }

        groupLedger.save(new GroupPointTransaction(
                group, pointType, Math.toIntExact(signed), type, blank(description)
        ));
        return response(group);
    }

    private void replaceMemberRoleAssignments(
            Long workspaceId,
            Long groupId,
            Long memberId,
            Set<Long> desiredRoleIds
    ) {
        Map<Long, RoleDefinition> available = new LinkedHashMap<>();
        for (RoleDefinition role : availableRoles(group(workspaceId, groupId))) {
            available.put(role.getId(), role);
        }

        for (Long roleId : desiredRoleIds) {
            if (!available.containsKey(roleId)) {
                throw new ResourceNotFoundException("Role definition not available in group: " + roleId);
            }
        }

        List<RoleAssignment> active = roleAssignments
                .findByWorkspaceIdAndActorTypeAndActorIdAndContextTypeAndContextIdAndActiveTrue(
                        workspaceId,
                        ActorType.MEMBER,
                        memberId,
                        RoleContextType.MEMBER_GROUP,
                        groupId
                );

        Map<Long, RoleAssignment> activeByRole = new HashMap<>();
        for (RoleAssignment assignment : active) {
            activeByRole.put(assignment.getRoleDefinition().getId(), assignment);
            if (!desiredRoleIds.contains(assignment.getRoleDefinition().getId())) {
                assignment.setActive(false);
            }
        }

        Workspace workspace = workspace(workspaceId);

        for (Long roleId : desiredRoleIds) {
            if (activeByRole.containsKey(roleId)) continue;

            RoleAssignment assignment = roleAssignments
                    .findByWorkspaceIdAndRoleDefinitionIdAndActorTypeAndActorIdAndContextTypeAndContextId(
                            workspaceId,
                            roleId,
                            ActorType.MEMBER,
                            memberId,
                            RoleContextType.MEMBER_GROUP,
                            groupId
                    )
                    .orElse(null);

            if (assignment == null) {
                assignment = new RoleAssignment(
                        workspace,
                        available.get(roleId),
                        ActorType.MEMBER,
                        memberId,
                        RoleContextType.MEMBER_GROUP,
                        groupId
                );
            } else {
                assignment.setActive(true);
            }

            roleAssignments.save(assignment);
        }
    }

    private List<RoleDefinition> availableRoles(MemberGroup group) {
        Long workspaceId = group.getWorkspace().getId();
        Long groupId = group.getId();

        return roleDefinitions.findByWorkspaceIdAndActiveTrueOrderByNameAsc(workspaceId)
                .stream()
                .filter(role ->
                        role.getVisibility() == RoleVisibility.SHARED
                                || (
                                role.getVisibility() == RoleVisibility.PRIVATE
                                        && role.getOwnerContextType() == RoleContextType.MEMBER_GROUP
                                        && Objects.equals(role.getOwnerContextId(), groupId)
                        )
                )
                .toList();
    }

    private List<RoleDefinition> localRoles(MemberGroup group) {
        return availableRoles(group).stream()
                .filter(role -> role.getVisibility() == RoleVisibility.PRIVATE)
                .filter(role -> role.getOwnerContextType() == RoleContextType.MEMBER_GROUP)
                .filter(role -> Objects.equals(role.getOwnerContextId(), group.getId()))
                .toList();
    }

    private RoleDefinition localRole(Long workspaceId, Long groupId, Long roleId) {
        RoleDefinition role = roleDefinitions.findByIdAndWorkspaceId(roleId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Role definition not found: " + roleId));

        if (role.isSystemDefault()
                || role.getVisibility() != RoleVisibility.PRIVATE
                || role.getOwnerContextType() != RoleContextType.MEMBER_GROUP
                || !Objects.equals(role.getOwnerContextId(), groupId)) {
            throw new IllegalArgumentException("Only a private role owned by this group can be modified");
        }

        return role;
    }

    private void collect(Long id, Set<Long> groupIds, Set<Long> memberIds, Set<Long> seen) {
        if (!seen.add(id)) return;

        for (GroupMembership membership : memberships.findByMemberGroupIdAndActiveTrueOrderByIdAsc(id)) {
            memberIds.add(membership.getMember().getId());
        }

        for (GroupComposition composition : compositions.findByParentGroupIdAndActiveTrueOrderByIdAsc(id)) {
            Long childId = composition.getChildGroup().getId();
            groupIds.add(childId);
            collect(childId, groupIds, memberIds, seen);
        }
    }

    private boolean reaches(Long from, Long target, Set<Long> seen) {
        if (from.equals(target)) return true;
        if (!seen.add(from)) return false;

        for (GroupComposition composition : compositions.findByParentGroupIdAndActiveTrueOrderByIdAsc(from)) {
            if (reaches(composition.getChildGroup().getId(), target, seen)) {
                return true;
            }
        }

        return false;
    }

    private MemberGroupResponse response(MemberGroup group) {
        Long workspaceId = group.getWorkspace().getId();

        List<MemberGroupResponse.Balance> balances = pointTypes
                .findByWorkspaceIdAndActiveTrueOrderBySortOrderAsc(workspaceId)
                .stream()
                .map(pointType -> new MemberGroupResponse.Balance(
                        pointType.getId(),
                        pointType.getCode(),
                        pointType.getName(),
                        groupLedger.getBalance(group.getId(), pointType.getId())
                ))
                .toList();

        List<RoleAssignment> assignments = roleAssignments
                .findByWorkspaceIdAndContextTypeAndContextIdAndActiveTrue(
                        workspaceId,
                        RoleContextType.MEMBER_GROUP,
                        group.getId()
                );

        return MemberGroupResponse.fromCatalog(
                group,
                availableRoles(group),
                memberships.findByMemberGroupIdAndActiveTrueOrderByIdAsc(group.getId()),
                assignments,
                compositions.findByParentGroupIdAndActiveTrueOrderByIdAsc(group.getId()),
                compositions.findByChildGroupIdAndActiveTrueOrderByIdAsc(group.getId()),
                balances
        );
    }

    private MemberGroup group(Long workspaceId, Long id) {
        return groups.findByIdAndWorkspaceId(id, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Member group not found: " + id));
    }

    private Workspace workspace(Long id) {
        return workspaces.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found: " + id));
    }

    private String blank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
