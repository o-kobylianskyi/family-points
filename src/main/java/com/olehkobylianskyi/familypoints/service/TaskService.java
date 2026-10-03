package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.*;
import com.olehkobylianskyi.familypoints.dto.TaskDefinitionCreateRequest;
import com.olehkobylianskyi.familypoints.exception.InvalidTaskStateException;
import com.olehkobylianskyi.familypoints.exception.ResourceNotFoundException;
import com.olehkobylianskyi.familypoints.repository.*;
import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.olehkobylianskyi.familypoints.repository.MemberExceptionPeriodRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

@Service
public class TaskService {

    private final TaskDefinitionRepository taskDefinitionRepository;
    private final TaskInstanceRepository taskInstanceRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final PointTypeRepository pointTypeRepository;
    private final PointService pointService;
    private final MemberExceptionPeriodRepository memberExceptionPeriodRepository;
    private final WorkNodeRepository workNodeRepository;
    private final CurrentUserService currentUserService;
    private final TaskGenerationService taskGenerationService;
    private final MemberGroupRepository memberGroupRepository;
    private final GroupRoleRepository groupRoleRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final GroupCompositionRepository groupCompositionRepository;
    private final TaskDelegationRepository taskDelegationRepository;
    private final TaskParticipantRepository taskParticipantRepository;
    private final TaskAuthorizationService taskAuthorizationService;
    private final TaskAuditService taskAuditService;

    public TaskService(
            TaskDefinitionRepository taskDefinitionRepository,
            TaskInstanceRepository taskInstanceRepository,
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            PointTypeRepository pointTypeRepository,
            PointService pointService,
            MemberExceptionPeriodRepository memberExceptionPeriodRepository,
            WorkNodeRepository workNodeRepository,
            CurrentUserService currentUserService,
            TaskGenerationService taskGenerationService,
            MemberGroupRepository memberGroupRepository,
            GroupRoleRepository groupRoleRepository,
            GroupMembershipRepository groupMembershipRepository,
            GroupCompositionRepository groupCompositionRepository,
            TaskDelegationRepository taskDelegationRepository,
            TaskParticipantRepository taskParticipantRepository,
            TaskAuthorizationService taskAuthorizationService,
            TaskAuditService taskAuditService
    ) {
        this.taskDefinitionRepository = taskDefinitionRepository;
        this.taskInstanceRepository = taskInstanceRepository;
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.pointTypeRepository = pointTypeRepository;
        this.pointService = pointService;
        this.memberExceptionPeriodRepository = memberExceptionPeriodRepository;
        this.workNodeRepository = workNodeRepository;
        this.currentUserService = currentUserService;
        this.taskGenerationService = taskGenerationService;
        this.memberGroupRepository = memberGroupRepository;
        this.groupRoleRepository = groupRoleRepository;
        this.groupMembershipRepository = groupMembershipRepository;
        this.groupCompositionRepository = groupCompositionRepository;
        this.taskDelegationRepository = taskDelegationRepository;
        this.taskParticipantRepository = taskParticipantRepository;
        this.taskAuthorizationService = taskAuthorizationService;
        this.taskAuditService = taskAuditService;
    }

    @Transactional
    public TaskDefinition createDefinition(
            Long workspaceId,
            AssignmentPolicy assignmentPolicy,
            Long assignedMemberId,
            Long targetGroupId,
            Long preferredMemberId,
            Long responsibleMemberId,
            Long parentTaskDefinitionId,
            boolean delegationAllowed,
            RoleMatchMode roleMatchMode,
            Set<Long> requiredGroupRoleIds,
            java.util.List<com.olehkobylianskyi.familypoints.dto.TaskActorRef> administrators,
            java.util.List<com.olehkobylianskyi.familypoints.dto.TaskActorRef> observers,
            java.util.List<com.olehkobylianskyi.familypoints.dto.TaskActorRef> executors,
            String title,
            String description,
            boolean mandatory,
            TaskRecurrenceType recurrenceType,
            LocalDate startDate,
            LocalDate endDate,
            Integer recurrenceDayOfWeek,
            Integer recurrenceDayOfMonth,
            Long rewardPointTypeId,
            Integer rewardAmount,
            Long penaltyPointTypeId,
            Integer penaltyAmount,
            LocalTime dueTime
    ) {
        taskAuthorizationService.requireCreate(workspaceId);
        Workspace workspace = getWorkspaceOrThrow(workspaceId);
        AssignmentPolicy policy = assignmentPolicy == null ? AssignmentPolicy.SINGLE_MEMBER : assignmentPolicy;

        WorkspaceMember assignedMember = assignedMemberId == null ? null : getMemberOrThrow(workspaceId, assignedMemberId);
        WorkspaceMember preferredMember = preferredMemberId == null ? null : getMemberOrThrow(workspaceId, preferredMemberId);
        MemberGroup targetGroup = targetGroupId == null ? null : memberGroupRepository.findByIdAndWorkspaceId(targetGroupId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Member group not found: " + targetGroupId));

        validateAssignment(policy, assignedMember, targetGroup, preferredMember);

        if (startDate == null) throw new IllegalArgumentException("Task start date must be specified");
        if (endDate != null && endDate.isBefore(startDate))
            throw new IllegalArgumentException("Task end date cannot be before start date");
        validateRecurrence(recurrenceType, recurrenceDayOfWeek, recurrenceDayOfMonth);

        PointType rewardPointType = getOptionalPointType(workspaceId, rewardPointTypeId);
        PointType penaltyPointType = getOptionalPointType(workspaceId, penaltyPointTypeId);
        validateAmountAndPointType(rewardPointTypeId, rewardAmount, "Reward");
        validateAmountAndPointType(penaltyPointTypeId, penaltyAmount, "Penalty");

        // Legacy assignedMember remains populated only for SINGLE_MEMBER until shared/open TaskInstance lifecycle is enabled.
        TaskDefinition definition = new TaskDefinition(
                workspace, policy == AssignmentPolicy.SINGLE_MEMBER ? assignedMember : null, title.trim(), description, mandatory,
                recurrenceType, startDate, endDate, recurrenceDayOfWeek, recurrenceDayOfMonth,
                rewardPointType, rewardAmount, penaltyPointType, penaltyAmount, dueTime);

        WorkspaceMember creator = currentUserService.getCurrentAccount().getWorkspaceMember();
        if (!creator.getWorkspace().getId().equals(workspaceId))
            throw new IllegalArgumentException("Creator does not belong to task workspace");

        WorkspaceMember responsible = responsibleMemberId == null ? creator : getMemberOrThrow(workspaceId, responsibleMemberId);
        definition.setCreatedBy(creator);
        definition.setResponsibleMember(responsible);
        definition.setAssignmentPolicy(policy);
        definition.setTargetGroup(targetGroup);
        definition.setPreferredMember(preferredMember);
        definition.setDelegationAllowed(delegationAllowed);
        definition.setRoleMatchMode(roleMatchMode == null ? RoleMatchMode.ANY : roleMatchMode);

        if (requiredGroupRoleIds != null && !requiredGroupRoleIds.isEmpty()) {
            if (targetGroup == null) throw new IllegalArgumentException("Group roles require targetGroupId");
            for (Long roleId : requiredGroupRoleIds) {
                GroupRole role = groupRoleRepository.findByIdAndMemberGroupId(roleId, targetGroup.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Group role not found: " + roleId));
                definition.getRequiredGroupRoles().add(role);
            }
        }

        WorkNode parentNode = null;
        if (parentTaskDefinitionId != null) {
            TaskDefinition parentDefinition = getDefinitionOrThrow(workspaceId, parentTaskDefinitionId);
            taskAuthorizationService.requireRead(parentDefinition);
            if (parentDefinition.getWorkNode() == null)
                throw new InvalidTaskStateException("Parent task has no work node");
            parentNode = parentDefinition.getWorkNode();
        }
        WorkNode workNode = workNodeRepository.save(new WorkNode(workspace, parentNode, WorkNodeType.TASK, title.trim(), description));
        definition.setWorkNode(workNode);
        TaskDefinition savedDefinition = taskDefinitionRepository.save(definition);

        addParticipant(savedDefinition, TaskParticipantRole.ADMIN, ActorType.MEMBER, creator.getId());
        saveParticipants(savedDefinition, TaskParticipantRole.ADMIN, administrators);
        saveParticipants(savedDefinition, TaskParticipantRole.OBSERVER, observers);
        saveParticipants(savedDefinition, TaskParticipantRole.EXECUTOR, executors);
        if (policy == AssignmentPolicy.SINGLE_MEMBER && assignedMember != null)
            addParticipant(savedDefinition, TaskParticipantRole.EXECUTOR, ActorType.MEMBER, assignedMember.getId());
        if ((policy == AssignmentPolicy.GROUP_SHARED || policy == AssignmentPolicy.OPEN_GROUP) && targetGroup != null)
            addParticipant(savedDefinition, TaskParticipantRole.EXECUTOR, ActorType.GROUP, targetGroup.getId());

        // Only SINGLE_MEMBER has a concrete member instance in v1. Open/shared policies are resolved by claim/participants next.
        taskAuditService.recordUserEvent(
                savedDefinition, null, TaskAuditEventType.DEFINITION_CREATED, creator,
                null, null, null, null, null
        );

        if (policy == AssignmentPolicy.SINGLE_MEMBER)
            taskGenerationService.generateForDefinition(savedDefinition, LocalDate.now());

        return savedDefinition;
    }

    @Transactional
    public TaskDefinition updateDefinition(Long workspaceId, Long definitionId, TaskDefinitionCreateRequest request) {
        TaskDefinition definition = getDefinitionOrThrow(workspaceId, definitionId);
        requireDefinitionManagement(definition);

        AssignmentPolicy policy = request.getAssignmentPolicy() == null ? AssignmentPolicy.SINGLE_MEMBER : request.getAssignmentPolicy();
        WorkspaceMember assignedMember = request.getAssignedMemberId() == null ? null : getMemberOrThrow(workspaceId, request.getAssignedMemberId());
        WorkspaceMember preferredMember = request.getPreferredMemberId() == null ? null : getMemberOrThrow(workspaceId, request.getPreferredMemberId());
        MemberGroup targetGroup = request.getTargetGroupId() == null ? null : memberGroupRepository.findByIdAndWorkspaceId(request.getTargetGroupId(), workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Member group not found: " + request.getTargetGroupId()));
        WorkspaceMember responsible = request.getResponsibleMemberId() == null
                ? definition.getCreatedBy()
                : getMemberOrThrow(workspaceId, request.getResponsibleMemberId());

        validateAssignment(policy, assignedMember, targetGroup, preferredMember);
        if (request.getStartDate() == null) throw new IllegalArgumentException("Task start date must be specified");
        if (request.getEndDate() != null && request.getEndDate().isBefore(request.getStartDate()))
            throw new IllegalArgumentException("Task end date cannot be before start date");
        validateRecurrence(request.getRecurrenceType(), request.getRecurrenceDayOfWeek(), request.getRecurrenceDayOfMonth());
        validateAmountAndPointType(request.getRewardPointTypeId(), request.getRewardAmount(), "Reward");
        validateAmountAndPointType(request.getPenaltyPointTypeId(), request.getPenaltyAmount(), "Penalty");

        definition.setAssignmentPolicy(policy);
        definition.setAssignedMember(policy == AssignmentPolicy.SINGLE_MEMBER ? assignedMember : null);
        definition.setTargetGroup(targetGroup);
        definition.setPreferredMember(preferredMember);
        definition.setResponsibleMember(responsible);
        definition.setDelegationAllowed(request.isDelegationAllowed());
        definition.setRoleMatchMode(request.getRoleMatchMode() == null ? RoleMatchMode.ANY : request.getRoleMatchMode());
        definition.getRequiredGroupRoles().clear();
        if (request.getRequiredGroupRoleIds() != null && !request.getRequiredGroupRoleIds().isEmpty()) {
            if (targetGroup == null) throw new IllegalArgumentException("Group roles require targetGroupId");
            for (Long roleId : request.getRequiredGroupRoleIds()) {
                GroupRole role = groupRoleRepository.findByIdAndMemberGroupId(roleId, targetGroup.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Group role not found: " + roleId));
                definition.getRequiredGroupRoles().add(role);
            }
        }

        definition.setTitle(request.getTitle().trim());
        definition.setDescription(request.getDescription());
        definition.setMandatory(request.isMandatory());
        definition.setRecurrenceType(request.getRecurrenceType());
        definition.setStartDate(request.getStartDate());
        definition.setEndDate(request.getEndDate());
        definition.setRecurrenceDayOfWeek(request.getRecurrenceDayOfWeek());
        definition.setRecurrenceDayOfMonth(request.getRecurrenceDayOfMonth());
        definition.setDueTime(request.getDueTime());
        definition.setRewardPointType(getOptionalPointType(workspaceId, request.getRewardPointTypeId()));
        definition.setRewardAmount(request.getRewardAmount());
        definition.setPenaltyPointType(getOptionalPointType(workspaceId, request.getPenaltyPointTypeId()));
        definition.setPenaltyAmount(request.getPenaltyAmount());

        if (definition.getWorkNode() != null) {
            definition.getWorkNode().setTitle(definition.getTitle());
            definition.getWorkNode().setDescription(definition.getDescription());
        }

        taskParticipantRepository.deleteByTaskDefinitionId(definitionId);
        if (definition.getCreatedBy() != null)
            addParticipant(definition, TaskParticipantRole.ADMIN, ActorType.MEMBER, definition.getCreatedBy().getId());
        saveParticipants(definition, TaskParticipantRole.ADMIN, request.getAdministrators());
        saveParticipants(definition, TaskParticipantRole.OBSERVER, request.getObservers());
        saveParticipants(definition, TaskParticipantRole.EXECUTOR, request.getExecutors());
        if (policy == AssignmentPolicy.SINGLE_MEMBER && assignedMember != null)
            addParticipant(definition, TaskParticipantRole.EXECUTOR, ActorType.MEMBER, assignedMember.getId());
        if ((policy == AssignmentPolicy.GROUP_SHARED || policy == AssignmentPolicy.OPEN_GROUP) && targetGroup != null)
            addParticipant(definition, TaskParticipantRole.EXECUTOR, ActorType.GROUP, targetGroup.getId());

        TaskDefinition saved = taskDefinitionRepository.save(definition);
        taskAuditService.recordUserEvent(
                saved, null, TaskAuditEventType.DEFINITION_UPDATED,
                currentUserService.getCurrentAccount().getWorkspaceMember(),
                null, null, null, null, null
        );
        if (policy == AssignmentPolicy.SINGLE_MEMBER && saved.isActive())
            taskGenerationService.generateForDefinition(saved, LocalDate.now());
        return saved;
    }

    @Transactional
    public TaskDefinition setDefinitionActive(Long workspaceId, Long definitionId, boolean active) {
        TaskDefinition definition = getDefinitionOrThrow(workspaceId, definitionId);
        requireDefinitionManagement(definition);
        boolean changed = definition.isActive() != active;
        definition.setActive(active);
        if (definition.getWorkNode() != null) definition.getWorkNode().setActive(active);
        TaskDefinition saved = taskDefinitionRepository.save(definition);
        if (changed) {
            WorkspaceMember performedBy = currentUserService.getCurrentAccount().getWorkspaceMember();
            taskAuditService.recordUserEvent(
                    saved, null,
                    active ? TaskAuditEventType.DEFINITION_ACTIVATED : TaskAuditEventType.DEFINITION_DEACTIVATED,
                    performedBy,
                    null, null, null, null, null
            );

            if (!active) {
                for (TaskInstance instance : taskInstanceRepository.findByTaskDefinitionIdAndStatus(
                        definitionId, TaskInstanceStatus.IN_PROGRESS)) {
                    instance.pause();
                    taskAuditService.recordSystemEvent(
                            saved, instance, TaskAuditEventType.INSTANCE_PAUSED,
                            ActorType.MEMBER, instance.getMember().getId(),
                            ActorType.MEMBER, instance.getMember().getId(),
                            "Paused automatically because the task definition was deactivated"
                    );
                }
            }
        }
        return saved;
    }

    private void requireDefinitionManagement(TaskDefinition definition) {
        taskAuthorizationService.requireManage(definition);
    }

    private void saveParticipants(TaskDefinition definition, TaskParticipantRole role, java.util.List<com.olehkobylianskyi.familypoints.dto.TaskActorRef> actors) {
        if (actors == null) return;
        for (var actor : actors) {
            if (actor == null || actor.getActorType() == null || actor.getActorId() == null) continue;
            validateActor(definition.getWorkspace().getId(), actor.getActorType(), actor.getActorId());
            addParticipant(definition, role, actor.getActorType(), actor.getActorId());
        }
    }

    private void addParticipant(TaskDefinition definition, TaskParticipantRole role, ActorType type, Long actorId) {
        if (!taskParticipantRepository.existsByTaskDefinitionIdAndRoleAndActorTypeAndActorId(definition.getId(), role, type, actorId))
            taskParticipantRepository.save(new TaskParticipant(definition, role, type, actorId));
    }

    private void validateActor(Long workspaceId, ActorType type, Long actorId) {
        if (type == ActorType.MEMBER) getMemberOrThrow(workspaceId, actorId);
        else memberGroupRepository.findByIdAndWorkspaceId(actorId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Member group not found: " + actorId));
    }

    @Transactional(readOnly = true)
    public java.util.List<TaskParticipant> getParticipants(Long workspaceId, Long definitionId) {
        TaskDefinition definition = getDefinitionOrThrow(workspaceId, definitionId);
        taskAuthorizationService.requireRead(definition);
        return taskParticipantRepository.findByTaskDefinitionIdOrderByIdAsc(definitionId);
    }

    @Transactional(readOnly = true)
    public String getActorName(Long workspaceId, TaskParticipant p) {
        if (p.getActorType() == ActorType.MEMBER) return getMemberOrThrow(workspaceId, p.getActorId()).getName();
        return memberGroupRepository.findByIdAndWorkspaceId(p.getActorId(), workspaceId)
                .map(MemberGroup::getName).orElse("#" + p.getActorId());
    }

    @Transactional(readOnly = true)
    public java.util.List<TaskDefinition> getDefinitionsByParticipation(Long workspaceId, String view) {
        getWorkspaceOrThrow(workspaceId);
        WorkspaceMember me = currentUserService.getCurrentAccount().getWorkspaceMember();
        var all = taskDefinitionRepository.findByWorkspaceIdOrderByIdAsc(workspaceId);
        if ("created".equalsIgnoreCase(view)) return all.stream().filter(d -> d.getCreatedBy()!=null && d.getCreatedBy().getId().equals(me.getId())).toList();
        TaskParticipantRole role = switch (view == null ? "" : view.toLowerCase()) {
            case "admin" -> TaskParticipantRole.ADMIN;
            case "observer" -> TaskParticipantRole.OBSERVER;
            case "executor" -> TaskParticipantRole.EXECUTOR;
            default -> null;
        };
        if (role == null) return all.stream().filter(taskAuthorizationService::canRead).toList();
        return all.stream().filter(d -> participantMatchesMember(d.getId(), role, me.getId(), new java.util.HashSet<>())).toList();
    }

    private boolean participantMatchesMember(
            Long definitionId,
            TaskParticipantRole role,
            Long memberId,
            java.util.Set<Long> visitedGroups
    ) {
        for (TaskParticipant p :
                taskParticipantRepository.findByTaskDefinitionIdAndRoleOrderByIdAsc(
                        definitionId,
                        role
                )) {

            if (p.getActorType() == ActorType.MEMBER
                    && p.getActorId().equals(memberId)) {
                return true;
            }

            if (p.getActorType() == ActorType.GROUP
                    && memberMatchesGroup(
                    p.getActorId(),
                    memberId,
                    java.util.Set.of(),
                    RoleMatchMode.ANY,
                    new java.util.HashSet<>()
            )) {
                return true;
            }
        }

        return false;
    }


    private void validateAssignment(AssignmentPolicy policy, WorkspaceMember assignedMember, MemberGroup targetGroup, WorkspaceMember preferredMember) {
        switch (policy) {
            case SINGLE_MEMBER -> { if (assignedMember == null) throw new IllegalArgumentException("assignedMemberId is required for SINGLE_MEMBER"); }
            case GROUP_SHARED, OPEN_GROUP -> { if (targetGroup == null) throw new IllegalArgumentException("targetGroupId is required for " + policy); }
            case OPEN_WORKSPACE -> { }
            case PREFERRED_MEMBER -> { if (preferredMember == null) throw new IllegalArgumentException("preferredMemberId is required for PREFERRED_MEMBER"); }
        }
    }

    @Transactional(readOnly = true)
    public List<TaskDefinition> getSubtasks(Long workspaceId, Long definitionId) {
        TaskDefinition parent = getDefinitionOrThrow(workspaceId, definitionId);
        taskAuthorizationService.requireRead(parent);
        if (parent.getWorkNode() == null) return List.of();

        return workNodeRepository.findByParentNodeIdOrderBySortOrderAscIdAsc(parent.getWorkNode().getId())
                .stream()
                .filter(node -> node.getType() == WorkNodeType.TASK)
                .map(node -> taskDefinitionRepository.findByWorkNodeId(node.getId()).orElse(null))
                .filter(java.util.Objects::nonNull)
                .filter(taskAuthorizationService::canRead)
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskDefinition getDefinition(Long workspaceId, Long definitionId) {
        TaskDefinition definition = getDefinitionOrThrow(workspaceId, definitionId);
        taskAuthorizationService.requireRead(definition);
        return definition;
    }

    @Transactional(readOnly = true)
    public List<TaskDefinition> getDefinitions(
            Long workspaceId
    ) {
        getWorkspaceOrThrow(workspaceId);

        return taskDefinitionRepository
                .findByWorkspaceIdAndActiveTrueOrderByIdAsc(workspaceId)
                .stream()
                .filter(taskAuthorizationService::canRead)
                .toList();
    }

    @Transactional
    public TaskInstance createInstance(
            Long workspaceId,
            Long definitionId,
            LocalDate scheduledDate
    ) {
        TaskDefinition definition =
                getDefinitionOrThrow(
                        workspaceId,
                        definitionId
                );

        taskAuthorizationService.requireManage(definition);

        if (!definition.isActive()) {
            throw new InvalidTaskStateException(
                    "Task definition is inactive"
            );
        }

        if (definition.getAssignedMember() == null) {
            throw new InvalidTaskStateException(
                    "Task definition has no assigned member"
            );
        }

        TaskInstance existing = taskInstanceRepository
                .findByTaskDefinitionIdAndScheduledDate(definitionId, scheduledDate)
                .orElse(null);
        if (existing != null) {
            return existing;
        }

        TaskInstance created = taskInstanceRepository.save(
                new TaskInstance(definition, definition.getAssignedMember(), scheduledDate)
        );
        taskAuditService.recordUserEvent(
                definition, created, TaskAuditEventType.INSTANCE_CREATED,
                currentUserService.getCurrentAccount().getWorkspaceMember(),
                null, null, ActorType.MEMBER, created.getMember().getId(),
                "Created for " + scheduledDate
        );
        return created;
    }

    @Transactional(readOnly = true)
    public TaskInstance getInstanceForDefinitionDate(Long workspaceId, Long definitionId, LocalDate date) {
        TaskDefinition definition = getDefinitionOrThrow(workspaceId, definitionId);
        taskAuthorizationService.requireRead(definition);
        return taskInstanceRepository.findByTaskDefinitionIdAndScheduledDate(definitionId, date).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<TaskInstance> getMemberTasks(
            Long workspaceId,
            Long memberId,
            LocalDate date
    ) {
        getMemberOrThrow(workspaceId, memberId);

        return taskInstanceRepository
                .findByMemberIdAndScheduledDateOrderByIdAsc(memberId, date)
                .stream()
                .filter(instance -> taskAuthorizationService.canRead(instance.getTaskDefinition()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TaskDefinition> getOpenDefinitions(Long workspaceId, LocalDate date, Long memberId) {
        getWorkspaceOrThrow(workspaceId);
        taskAuthorizationService.requireWorkspace(workspaceId);
        WorkspaceMember current = taskAuthorizationService.currentMember();
        WorkspaceMember member = memberId == null ? current : getMemberOrThrow(workspaceId, memberId);
        if (!member.getId().equals(current.getId())
                && !current.hasPermission(WorkspacePermission.VIEW_ALL_TASKS)
                && !current.hasPermission(WorkspacePermission.ADMIN_OVERRIDE)) {
            throw new org.springframework.security.access.AccessDeniedException("Current user cannot inspect open tasks for another member");
        }
        return taskDefinitionRepository.findByWorkspaceIdAndActiveTrueOrderByIdAsc(workspaceId).stream()
                .filter(d -> d.getAssignmentPolicy() != AssignmentPolicy.SINGLE_MEMBER)
                .filter(d -> d.getAssignmentPolicy() != AssignmentPolicy.GROUP_SHARED)
                .filter(d -> taskGenerationService.shouldGenerate(d, date))
                .filter(d -> taskInstanceRepository.findByTaskDefinitionIdAndScheduledDate(d.getId(), date)
                        .map(i -> i.getStatus() == TaskInstanceStatus.RELEASED)
                        .orElse(true))
                .filter(d -> member == null || isEligible(d, member))
                .toList();
    }

    @Transactional
    public TaskInstance claim(Long workspaceId, Long definitionId, LocalDate date, Long memberId) {
        TaskDefinition definition = getDefinitionOrThrow(workspaceId, definitionId);
        WorkspaceMember member = getMemberOrThrow(workspaceId, memberId);
        taskAuthorizationService.requireClaimForSelf(definition, member);
        if (!definition.isActive())
            throw new InvalidTaskStateException("Inactive task definition cannot be claimed");
        if (definition.getAssignmentPolicy() == AssignmentPolicy.SINGLE_MEMBER || definition.getAssignmentPolicy() == AssignmentPolicy.GROUP_SHARED)
            throw new InvalidTaskStateException("This task is not claimable");
        if (!taskGenerationService.shouldGenerate(definition, date))
            throw new InvalidTaskStateException("Task is not scheduled for this date");
        if (!isEligible(definition, member))
            throw new InvalidTaskStateException("Member is not eligible for this task");
        TaskInstance instance = taskInstanceRepository.findByTaskDefinitionIdAndScheduledDate(definitionId, date)
                .orElse(null);
        if (instance != null && instance.getStatus() != TaskInstanceStatus.RELEASED)
            throw new InvalidTaskStateException("Task has already been claimed for this date");
        if (instance == null) {
            instance = new TaskInstance(definition, member, date);
            instance.markClaimed();
        } else {
            instance.reclaim(member);
        }
        TaskInstance saved = taskInstanceRepository.save(instance);
        taskAuditService.recordUserEvent(
                definition, saved, TaskAuditEventType.INSTANCE_CLAIMED, member,
                null, null, ActorType.MEMBER, member.getId(), null
        );
        return saved;
    }

    @Transactional
    public TaskInstance delegate(Long workspaceId, Long instanceId, Long toMemberId, String reason) {
        TaskInstance instance = getInstanceForUpdateOrThrow(workspaceId, instanceId);
        TaskDefinition definition = instance.getTaskDefinition();
        taskAuthorizationService.requireDelegate(instance);
        if (!definition.isDelegationAllowed())
            throw new InvalidTaskStateException("Delegation is not allowed for this task");
        if (instance.getStatus() == TaskInstanceStatus.COMPLETED || instance.getStatus() == TaskInstanceStatus.MISSED || instance.getStatus() == TaskInstanceStatus.EXCUSED || instance.getStatus() == TaskInstanceStatus.CANCELLED || instance.getStatus() == TaskInstanceStatus.RELEASED)
            throw new InvalidTaskStateException("Finished task cannot be delegated");
        WorkspaceMember target = getMemberOrThrow(workspaceId, toMemberId);
        if (!isEligibleForDelegation(definition, target))
            throw new InvalidTaskStateException("Target member is not eligible for delegation");
        WorkspaceMember from = instance.getMember();
        if (from.getId().equals(target.getId())) return instance;
        WorkspaceMember delegatedBy = currentUserService.getCurrentAccount().getWorkspaceMember();
        taskDelegationRepository.save(new TaskDelegation(instance, from, target, delegatedBy, reason));
        boolean wasInProgress = instance.getStatus() == TaskInstanceStatus.IN_PROGRESS;
        instance.changeExecutor(target);
        if (wasInProgress) {
            instance.pause();
        }
        taskAuditService.recordUserEvent(
                definition, instance, TaskAuditEventType.INSTANCE_DELEGATED, delegatedBy,
                ActorType.MEMBER, from.getId(), ActorType.MEMBER, target.getId(), reason
        );
        return instance;
    }

    private boolean isEligibleForDelegation(TaskDefinition definition, WorkspaceMember member) {
        if (definition.getTargetGroup() != null) return isEligible(definition, member);
        // A directly assigned task may be delegated to another member of the same workspace when delegation is enabled.
        return member.getWorkspace().getId().equals(definition.getWorkspace().getId());
    }

    private boolean isEligible(TaskDefinition definition, WorkspaceMember member) {
        if (!member.getWorkspace().getId().equals(definition.getWorkspace().getId())) return false;
        AssignmentPolicy policy = definition.getAssignmentPolicy();
        if (policy == AssignmentPolicy.OPEN_WORKSPACE || policy == AssignmentPolicy.PREFERRED_MEMBER) {
            if (definition.getTargetGroup() == null) return true;
        }
        if (policy == AssignmentPolicy.OPEN_GROUP || definition.getTargetGroup() != null) {
            return memberMatchesGroup(definition.getTargetGroup().getId(), member.getId(), definition.getRequiredGroupRoles(), definition.getRoleMatchMode(), new java.util.HashSet<>());
        }
        return policy == AssignmentPolicy.SINGLE_MEMBER && definition.getAssignedMember() != null
                && definition.getAssignedMember().getId().equals(member.getId());
    }

    private boolean memberMatchesGroup(Long groupId, Long memberId, Set<GroupRole> requiredRoles, RoleMatchMode mode, Set<Long> visited) {
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

    @Transactional
    public TaskInstance start(
            Long workspaceId,
            Long instanceId
    ) {
        TaskInstance instance =
                getInstanceForUpdateOrThrow(
                        workspaceId,
                        instanceId
                );

        taskAuthorizationService.requireExecute(instance);

        if (!instance.getTaskDefinition().isActive()) {
            throw new InvalidTaskStateException("Inactive task definition cannot be started");
        }

        if (instance.getStatus()
                != TaskInstanceStatus.PENDING) {

            throw new InvalidTaskStateException(
                    "Only a pending task can be started"
            );
        }

        instance.start();
        taskAuditService.recordUserEvent(
                instance.getTaskDefinition(), instance, TaskAuditEventType.INSTANCE_STARTED,
                currentUserService.getCurrentAccount().getWorkspaceMember(),
                null, null, ActorType.MEMBER, instance.getMember().getId(), null
        );

        return instance;
    }

    @Transactional
    public TaskInstance pause(Long workspaceId, Long instanceId) {
        TaskInstance instance = getInstanceForUpdateOrThrow(workspaceId, instanceId);
        taskAuthorizationService.requireExecute(instance);

        if (instance.getStatus() != TaskInstanceStatus.IN_PROGRESS) {
            throw new InvalidTaskStateException("Only an in-progress task can be paused");
        }

        instance.pause();
        taskAuditService.recordUserEvent(
                instance.getTaskDefinition(), instance, TaskAuditEventType.INSTANCE_PAUSED,
                currentUserService.getCurrentAccount().getWorkspaceMember(),
                ActorType.MEMBER, instance.getMember().getId(),
                ActorType.MEMBER, instance.getMember().getId(), null
        );
        return instance;
    }

    @Transactional
    public TaskInstance resume(Long workspaceId, Long instanceId) {
        TaskInstance instance = getInstanceForUpdateOrThrow(workspaceId, instanceId);
        taskAuthorizationService.requireExecute(instance);

        if (!instance.getTaskDefinition().isActive()) {
            throw new InvalidTaskStateException("Inactive task definition cannot be resumed");
        }
        if (instance.getStatus() != TaskInstanceStatus.PAUSED) {
            throw new InvalidTaskStateException("Only a paused task can be resumed");
        }

        instance.resume();
        taskAuditService.recordUserEvent(
                instance.getTaskDefinition(), instance, TaskAuditEventType.INSTANCE_RESUMED,
                currentUserService.getCurrentAccount().getWorkspaceMember(),
                ActorType.MEMBER, instance.getMember().getId(),
                ActorType.MEMBER, instance.getMember().getId(), null
        );
        return instance;
    }

    @Transactional
    public TaskInstance cancel(Long workspaceId, Long instanceId) {
        TaskInstance instance = getInstanceForUpdateOrThrow(workspaceId, instanceId);
        if (taskAuthorizationService.canManage(instance.getTaskDefinition())) {
            taskAuthorizationService.requireAdministrativeAction(instance);
        } else {
            taskAuthorizationService.requireExecute(instance);
        }

        if (instance.getStatus() == TaskInstanceStatus.COMPLETED
                || instance.getStatus() == TaskInstanceStatus.MISSED
                || instance.getStatus() == TaskInstanceStatus.EXCUSED
                || instance.getStatus() == TaskInstanceStatus.CANCELLED) {
            throw new InvalidTaskStateException("Finished task cannot be cancelled");
        }

        instance.cancel();
        taskAuditService.recordUserEvent(
                instance.getTaskDefinition(), instance, TaskAuditEventType.INSTANCE_CANCELLED,
                currentUserService.getCurrentAccount().getWorkspaceMember(),
                ActorType.MEMBER, instance.getMember().getId(),
                ActorType.MEMBER, instance.getMember().getId(), null
        );
        return instance;
    }

    @Transactional
    public TaskInstance release(Long workspaceId, Long instanceId) {
        TaskInstance instance = getInstanceForUpdateOrThrow(workspaceId, instanceId);
        taskAuthorizationService.requireExecute(instance);
        TaskDefinition definition = instance.getTaskDefinition();

        if (definition.getAssignmentPolicy() == AssignmentPolicy.SINGLE_MEMBER
                || definition.getAssignmentPolicy() == AssignmentPolicy.GROUP_SHARED) {
            throw new InvalidTaskStateException("This task cannot be released back to the open pool");
        }
        if (instance.getStatus() != TaskInstanceStatus.PENDING
                && instance.getStatus() != TaskInstanceStatus.IN_PROGRESS
                && instance.getStatus() != TaskInstanceStatus.PAUSED) {
            throw new InvalidTaskStateException("Task cannot be released from status " + instance.getStatus());
        }

        WorkspaceMember from = instance.getMember();
        instance.release();
        taskAuditService.recordUserEvent(
                definition, instance, TaskAuditEventType.INSTANCE_RELEASED,
                currentUserService.getCurrentAccount().getWorkspaceMember(),
                ActorType.MEMBER, from.getId(), null, null, null
        );
        return instance;
    }

    @Transactional
    public TaskInstance complete(
            Long workspaceId,
            Long instanceId
    ) {
        TaskInstance instance =
                getInstanceForUpdateOrThrow(
                        workspaceId,
                        instanceId
                );

        taskAuthorizationService.requireExecute(instance);

        if (!instance.getTaskDefinition().isActive()) {
            throw new InvalidTaskStateException("Inactive task definition cannot be completed");
        }

        if (instance.getStatus()
                != TaskInstanceStatus.PENDING
                &&
                instance.getStatus()
                        != TaskInstanceStatus.IN_PROGRESS) {

            throw new InvalidTaskStateException(
                    "Task cannot be completed from status "
                            + instance.getStatus()
            );
        }

        instance.complete();

        processReward(
                workspaceId,
                instance
        );

        taskAuditService.recordUserEvent(
                instance.getTaskDefinition(), instance, TaskAuditEventType.INSTANCE_COMPLETED,
                currentUserService.getCurrentAccount().getWorkspaceMember(),
                null, null, ActorType.MEMBER, instance.getMember().getId(), null
        );

        return instance;
    }

    private void processReward(
            Long workspaceId,
            TaskInstance instance
    ) {
        if (instance.isRewardProcessed()) {
            return;
        }

        Integer rewardAmount =
                instance.getRewardAmount();

        PointType rewardPointType =
                instance.getRewardPointType();

        if (rewardAmount == null
                || rewardAmount <= 0
                || rewardPointType == null) {

            instance.markRewardProcessed();
            return;
        }

        pointService.earn(
                workspaceId,
                instance.getMember().getId(),
                rewardPointType.getId(),
                rewardAmount,
                PointTransactionSourceType.TASK,
                instance.getId(),
                "Task completed: "
                        + instance.getTitle()
        );

        instance.markRewardProcessed();
    }

    private void validateAmountAndPointType(
            Long pointTypeId,
            Integer amount,
            String fieldName
    ) {
        boolean hasPointType =
                pointTypeId != null;

        boolean hasAmount =
                amount != null && amount > 0;

        if (hasPointType != hasAmount) {
            throw new IllegalArgumentException(
                    fieldName
                            + " point type and amount must be specified together"
            );
        }
    }

    private Workspace getWorkspaceOrThrow(Long workspaceId) {
        return workspaceRepository
                .findById(workspaceId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Workspace "
                                        + workspaceId
                                        + " not found"
                        )
                );
    }

    private WorkspaceMember getMemberOrThrow(
            Long workspaceId,
            Long memberId
    ) {
        return workspaceMemberRepository
                .findByIdAndWorkspaceId(
                        memberId,
                        workspaceId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Workspace member "
                                        + memberId
                                        + " not found in workspace "
                                        + workspaceId
                        )
                );
    }

    private TaskDefinition getDefinitionOrThrow(
            Long workspaceId,
            Long definitionId
    ) {
        return taskDefinitionRepository
                .findByIdAndWorkspaceId(
                        definitionId,
                        workspaceId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Task definition "
                                        + definitionId
                                        + " not found in workspace "
                                        + workspaceId
                        )
                );
    }

    private TaskInstance getInstanceForUpdateOrThrow(
            Long workspaceId,
            Long instanceId
    ) {
        return taskInstanceRepository
                .findByIdAndWorkspaceIdForUpdate(
                        instanceId,
                        workspaceId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Task instance "
                                        + instanceId
                                        + " not found in workspace "
                                        + workspaceId
                        )
                );
    }

    private PointType getOptionalPointType(
            Long workspaceId,
            Long pointTypeId
    ) {
        if (pointTypeId == null) {
            return null;
        }

        return pointTypeRepository
                .findByIdAndWorkspaceId(
                        pointTypeId,
                        workspaceId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Point type "
                                        + pointTypeId
                                        + " not found in workspace "
                                        + workspaceId
                        )
                );
    }

    @Transactional
    public TaskInstance miss(
            Long workspaceId,
            Long instanceId
    ) {
        TaskInstance instance =
                getInstanceForUpdateOrThrow(
                        workspaceId,
                        instanceId
                );

        taskAuthorizationService.requireAdministrativeAction(instance);

        if (instance.getStatus() == TaskInstanceStatus.COMPLETED) {
            throw new InvalidTaskStateException(
                    "Completed task cannot be marked as missed"
            );
        }

        if (instance.getStatus() == TaskInstanceStatus.MISSED) {
            return instance;
        }

        if (instance.getStatus() == TaskInstanceStatus.EXCUSED) {
            return instance;
        }

        List<MemberExceptionPeriod> exceptionPeriods =
                memberExceptionPeriodRepository.findActiveForDate(
                        instance.getMember().getId(),
                        instance.getScheduledDate()
                );

        if (!exceptionPeriods.isEmpty()) {

            MemberExceptionPeriod period =
                    exceptionPeriods.getFirst();

            String autoComment = "Automatically excused: "
                    + period.getType()
                    + (period.getComment() == null ? "" : " - " + period.getComment());
            instance.excuse(TaskExcuseReason.EXCEPTION_PERIOD, autoComment);
            taskAuditService.recordSystemEvent(
                    instance.getTaskDefinition(), instance, TaskAuditEventType.INSTANCE_EXCUSED,
                    null, null, ActorType.MEMBER, instance.getMember().getId(), autoComment
            );
            return instance;
        }

        instance.markMissed();

        processPenalty(workspaceId, instance);

        taskAuditService.recordUserEvent(
                instance.getTaskDefinition(), instance, TaskAuditEventType.INSTANCE_MISSED,
                currentUserService.getCurrentAccount().getWorkspaceMember(),
                null, null, ActorType.MEMBER, instance.getMember().getId(), null
        );

        return instance;
    }

    private void processPenalty(
            Long workspaceId,
            TaskInstance instance
    ) {
        if (instance.isPenaltyProcessed()) {
            return;
        }

        /*
         * Необов'язкові задачі не штрафуємо.
         * Використовуємо mandatory snapshot конкретного instance.
         */
        if (!instance.isMandatory()) {
            instance.markPenaltyProcessed();
            return;
        }

        Integer penaltyAmount =
                instance.getPenaltyAmount();

        PointType penaltyPointType =
                instance.getPenaltyPointType();

        /*
         * Обов'язкова задача може не мати
         * налаштованого штрафу.
         */
        if (penaltyAmount == null
                || penaltyAmount <= 0
                || penaltyPointType == null) {

            instance.markPenaltyProcessed();
            return;
        }

        pointService.penalty(
                workspaceId,
                instance.getMember().getId(),
                penaltyPointType,
                penaltyAmount,
                PointTransactionSourceType.TASK,
                instance.getId(),
                "Task missed: "
                        + instance.getTitle()
        );

        instance.markPenaltyProcessed();
    }

    @Transactional
    public TaskInstance excuse(
            Long workspaceId,
            Long instanceId,
            TaskExcuseReason reason,
            String comment
    ) {
        TaskInstance instance =
                getInstanceForUpdateOrThrow(
                        workspaceId,
                        instanceId
                );

        taskAuthorizationService.requireAdministrativeAction(instance);

        if (instance.getStatus() != TaskInstanceStatus.PENDING
                && instance.getStatus() != TaskInstanceStatus.IN_PROGRESS) {

            throw new InvalidTaskStateException(
                    "Task cannot be excused from status "
                            + instance.getStatus()
            );
        }

        instance.excuse(
                reason,
                comment
        );

        /*
         * EXCUSED означає, що штраф за цей instance
         * більше не повинен застосовуватися.
         */
        instance.markPenaltyProcessed();

        taskAuditService.recordUserEvent(
                instance.getTaskDefinition(), instance, TaskAuditEventType.INSTANCE_EXCUSED,
                currentUserService.getCurrentAccount().getWorkspaceMember(),
                null, null, ActorType.MEMBER, instance.getMember().getId(),
                reason + (comment == null || comment.isBlank() ? "" : ": " + comment)
        );

        return instance;
    }

    private void validateRecurrence(
            TaskRecurrenceType recurrenceType,
            Integer recurrenceDayOfWeek,
            Integer recurrenceDayOfMonth
    ) {
        switch (recurrenceType) {

            case ONCE, DAILY -> {
                if (recurrenceDayOfWeek != null
                        || recurrenceDayOfMonth != null) {
                    throw new IllegalArgumentException(
                            recurrenceType
                                    + " must not have recurrence day"
                    );
                }
            }

            case WEEKLY -> {
                if (recurrenceDayOfWeek == null) {
                    throw new IllegalArgumentException(
                            "WEEKLY task must have recurrenceDayOfWeek"
                    );
                }

                if (recurrenceDayOfMonth != null) {
                    throw new IllegalArgumentException(
                            "WEEKLY task must not have recurrenceDayOfMonth"
                    );
                }
            }

            case MONTHLY -> {
                if (recurrenceDayOfMonth == null) {
                    throw new IllegalArgumentException(
                            "MONTHLY task must have recurrenceDayOfMonth"
                    );
                }

                if (recurrenceDayOfWeek != null) {
                    throw new IllegalArgumentException(
                            "MONTHLY task must not have recurrenceDayOfWeek"
                    );
                }
            }

            case CUSTOM -> throw new IllegalArgumentException(
                    "CUSTOM recurrence is not supported yet"
            );
        }
    }
}