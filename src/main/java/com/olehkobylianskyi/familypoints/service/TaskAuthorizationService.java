package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.*;
import com.olehkobylianskyi.familypoints.repository.GroupCompositionRepository;
import com.olehkobylianskyi.familypoints.repository.GroupMembershipRepository;
import com.olehkobylianskyi.familypoints.repository.GroupPermissionGrantRepository;
import com.olehkobylianskyi.familypoints.repository.TaskParticipantRepository;
import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class TaskAuthorizationService {

    private final CurrentUserService currentUserService;
    private final TaskParticipantRepository taskParticipantRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final GroupCompositionRepository groupCompositionRepository;
    private final GroupPermissionGrantRepository groupPermissionGrantRepository;

    public TaskAuthorizationService(
            CurrentUserService currentUserService,
            TaskParticipantRepository taskParticipantRepository,
            GroupMembershipRepository groupMembershipRepository,
            GroupCompositionRepository groupCompositionRepository,
            GroupPermissionGrantRepository groupPermissionGrantRepository
    ) {
        this.currentUserService = currentUserService;
        this.taskParticipantRepository = taskParticipantRepository;
        this.groupMembershipRepository = groupMembershipRepository;
        this.groupCompositionRepository = groupCompositionRepository;
        this.groupPermissionGrantRepository = groupPermissionGrantRepository;
    }

    public WorkspaceMember currentMember() {
        return currentUserService.getCurrentAccount().getWorkspaceMember();
    }

    public void requireWorkspace(Long workspaceId) {
        if (!currentUserService.belongsToWorkspace(workspaceId)) {
            throw new AccessDeniedException("Current user cannot access this workspace");
        }
    }

    public void requireCreate(Long workspaceId) {
        requireWorkspace(workspaceId);
        WorkspaceMember me = currentMember();
        if (!me.hasPermission(WorkspacePermission.CREATE_TASKS)
                && !me.hasPermission(WorkspacePermission.ADMIN_OVERRIDE)) {
            throw new AccessDeniedException("Current user cannot create tasks");
        }
    }

    public boolean canRead(TaskDefinition definition) {
        WorkspaceMember me = currentMember();
        if (!sameWorkspace(definition, me)) return false;
        if (me.hasPermission(WorkspacePermission.VIEW_ALL_TASKS)
                || me.hasPermission(WorkspacePermission.ADMIN_OVERRIDE)) return true;
        if (isAuthor(definition, me)) return true;
        if (matchesParticipant(definition.getId(), TaskParticipantRole.ADMIN, me.getId())) return true;
        if (matchesParticipant(definition.getId(), TaskParticipantRole.OBSERVER, me.getId())) return true;
        if (matchesParticipant(definition.getId(), TaskParticipantRole.EXECUTOR, me.getId())) return true;
        if (hasScopedTaskPermission(definition, me, GroupPermission.TASK_VIEW)) return true;
        return isEligible(definition, me);
    }

    public void requireRead(TaskDefinition definition) {
        if (!canRead(definition)) throw new AccessDeniedException("Current user cannot view this task");
    }

    public boolean canManage(TaskDefinition definition) {
        WorkspaceMember me = currentMember();
        if (!sameWorkspace(definition, me)) return false;
        if (me.hasPermission(WorkspacePermission.MANAGE_TASKS)
                || me.hasPermission(WorkspacePermission.ADMIN_OVERRIDE)) return true;
        return isAuthor(definition, me)
                || matchesParticipant(definition.getId(), TaskParticipantRole.ADMIN, me.getId())
                || hasScopedTaskPermission(definition, me, GroupPermission.TASK_MANAGE);
    }

    public void requireManage(TaskDefinition definition) {
        if (!canManage(definition)) {
            throw new AccessDeniedException("Only the task author, task administrator, or workspace task manager can manage this task");
        }
    }

    public boolean canExecute(TaskInstance instance) {
        WorkspaceMember me = currentMember();
        TaskDefinition definition = instance.getTaskDefinition();
        if (!sameWorkspace(definition, me)) return false;

        // Execution rights belong only to the current executor of this concrete
        // TaskInstance. Being the author/admin or merely an EXECUTOR participant
        // does not mean that a member may execute an instance delegated to
        // somebody else. Administrative lifecycle actions are authorized
        // separately through requireManage/requireAdministrativeAction.
        return instance.getMember() != null
                && instance.getMember().getId().equals(me.getId());
    }

    public void requireExecute(TaskInstance instance) {
        if (!canExecute(instance)) throw new AccessDeniedException("Current user cannot execute this task");
    }

    public void requireAdministrativeAction(TaskInstance instance) {
        requireManage(instance.getTaskDefinition());
    }

    public void requireClaimForSelf(TaskDefinition definition, WorkspaceMember member) {
        WorkspaceMember me = currentMember();
        if (!me.getId().equals(member.getId())) {
            throw new AccessDeniedException("An open task can only be claimed for the current member");
        }
        if (!isEligible(definition, me)) {
            throw new AccessDeniedException("Current member is not eligible for this task");
        }
    }

    public void requireDelegate(TaskInstance instance) {
        TaskDefinition definition = instance.getTaskDefinition();
        if (!definition.isDelegationAllowed()) {
            throw new AccessDeniedException("Delegation is not allowed for this task");
        }
        if (canManage(definition) || canExecute(instance)) return;
        throw new AccessDeniedException("Current user cannot delegate this task");
    }

    public boolean isEligible(TaskDefinition definition, WorkspaceMember member) {
        if (!sameWorkspace(definition, member)) return false;
        AssignmentPolicy policy = definition.getAssignmentPolicy();
        if (policy == AssignmentPolicy.OPEN_WORKSPACE || policy == AssignmentPolicy.PREFERRED_MEMBER) {
            if (definition.getTargetGroup() == null) return true;
        }
        if (policy == AssignmentPolicy.OPEN_GROUP || definition.getTargetGroup() != null) {
            return memberMatchesGroup(
                    definition.getTargetGroup().getId(), member.getId(),
                    definition.getRequiredGroupRoles(), definition.getRoleMatchMode(), new HashSet<>()
            );
        }
        return policy == AssignmentPolicy.SINGLE_MEMBER
                && definition.getAssignedMember() != null
                && definition.getAssignedMember().getId().equals(member.getId());
    }

    public boolean matchesParticipant(Long definitionId, TaskParticipantRole role, Long memberId) {
        for (TaskParticipant participant : taskParticipantRepository.findByTaskDefinitionIdAndRoleOrderByIdAsc(definitionId, role)) {
            if (participant.getActorType() == ActorType.MEMBER && participant.getActorId().equals(memberId)) return true;
            if (participant.getActorType() == ActorType.GROUP
                    && memberMatchesGroup(participant.getActorId(), memberId, Set.of(), RoleMatchMode.ANY, new HashSet<>())) return true;
        }
        return false;
    }

    private boolean hasScopedTaskPermission(TaskDefinition definition, WorkspaceMember member, GroupPermission permission) {
        MemberGroup target = definition.getTargetGroup();
        if (target == null || !target.isEffectiveOn(java.time.LocalDate.now())) return false;

        for (GroupMembership membership : groupMembershipRepository.findByMemberIdAndActiveTrue(member.getId())) {
            MemberGroup authorityGroup = membership.getMemberGroup();
            if (!authorityGroup.isEffectiveOn(java.time.LocalDate.now())) continue;

            for (GroupRole role : membership.getRoles()) {
                for (GroupPermissionGrant grant : groupPermissionGrantRepository.findByGroupRoleIdAndPermission(role.getId(), permission)) {
                    if (!grant.getMemberGroup().getId().equals(authorityGroup.getId())) continue;
                    if (grant.getScope() == GroupPermissionScope.GROUP
                            && authorityGroup.getId().equals(target.getId())) return true;
                    if (grant.getScope() == GroupPermissionScope.GROUP_SUBTREE
                            && groupContains(authorityGroup.getId(), target.getId(), new HashSet<>())) return true;
                }
            }
        }
        return false;
    }

    private boolean groupContains(Long rootGroupId, Long targetGroupId, Set<Long> visited) {
        if (rootGroupId.equals(targetGroupId)) return true;
        if (!visited.add(rootGroupId)) return false;
        for (GroupComposition composition : groupCompositionRepository.findByParentGroupIdAndActiveTrueOrderByIdAsc(rootGroupId)) {
            if (groupContains(composition.getChildGroup().getId(), targetGroupId, visited)) return true;
        }
        return false;
    }

    private boolean isAuthor(TaskDefinition definition, WorkspaceMember member) {
        return definition.getCreatedBy() != null && definition.getCreatedBy().getId().equals(member.getId());
    }

    private boolean sameWorkspace(TaskDefinition definition, WorkspaceMember member) {
        return definition.getWorkspace().getId().equals(member.getWorkspace().getId());
    }

    private boolean memberMatchesGroup(
            Long groupId, Long memberId, Set<GroupRole> requiredRoles,
            RoleMatchMode mode, Set<Long> visited
    ) {
        if (!visited.add(groupId)) return false;
        var membership = groupMembershipRepository.findByMemberGroupIdAndMemberId(groupId, memberId).orElse(null);
        if (membership != null && membership.isActive()) {
            if (requiredRoles == null || requiredRoles.isEmpty()) return true;
            long matched = requiredRoles.stream().filter(membership.getRoles()::contains).count();
            if ((mode == null || mode == RoleMatchMode.ANY) && matched > 0) return true;
            if (mode == RoleMatchMode.ALL && matched == requiredRoles.size()) return true;
        }
        for (GroupComposition composition : groupCompositionRepository.findByParentGroupIdAndActiveTrueOrderByIdAsc(groupId)) {
            if (memberMatchesGroup(composition.getChildGroup().getId(), memberId, requiredRoles, mode, visited)) return true;
        }
        return false;
    }
}
