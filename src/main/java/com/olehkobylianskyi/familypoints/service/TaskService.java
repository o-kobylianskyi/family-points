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
    private final TaskScheduleSlotRepository taskScheduleSlots;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final PointTypeRepository pointTypeRepository;
    private final PointService pointService;
    private final ReputationService reputationService;
    private final MemberExceptionPeriodRepository memberExceptionPeriodRepository;
    private final WorkNodeRepository workNodeRepository;
    private final CurrentUserService currentUserService;
    private final TaskGenerationService taskGenerationService;
    private final MemberGroupRepository memberGroupRepository;
    private final RoleDefinitionRepository roleDefinitionRepository;
    private final RoleAssignmentRepository roleAssignmentRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final GroupCompositionRepository groupCompositionRepository;
    private final TaskDelegationRepository taskDelegationRepository;
    private final TaskParticipantRepository taskParticipantRepository;
    private final TaskAuthorizationService taskAuthorizationService;
    private final TaskAuditService taskAuditService;
    private final TaskRewardNegotiationService taskRewardNegotiationService;

    public TaskService(
            TaskDefinitionRepository taskDefinitionRepository,
            TaskInstanceRepository taskInstanceRepository,
            TaskScheduleSlotRepository taskScheduleSlots,
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            PointTypeRepository pointTypeRepository,
            PointService pointService,
            ReputationService reputationService,
            MemberExceptionPeriodRepository memberExceptionPeriodRepository,
            WorkNodeRepository workNodeRepository,
            CurrentUserService currentUserService,
            TaskGenerationService taskGenerationService,
            MemberGroupRepository memberGroupRepository,
            RoleDefinitionRepository roleDefinitionRepository,
            RoleAssignmentRepository roleAssignmentRepository,
            GroupMembershipRepository groupMembershipRepository,
            GroupCompositionRepository groupCompositionRepository,
            TaskDelegationRepository taskDelegationRepository,
            TaskParticipantRepository taskParticipantRepository,
            TaskAuthorizationService taskAuthorizationService,
            TaskAuditService taskAuditService,
            TaskRewardNegotiationService taskRewardNegotiationService
    ) {
        this.taskDefinitionRepository = taskDefinitionRepository;
        this.taskInstanceRepository = taskInstanceRepository;
        this.taskScheduleSlots = taskScheduleSlots;
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.pointTypeRepository = pointTypeRepository;
        this.pointService = pointService;
        this.reputationService = reputationService;
        this.memberExceptionPeriodRepository = memberExceptionPeriodRepository;
        this.workNodeRepository = workNodeRepository;
        this.currentUserService = currentUserService;
        this.taskGenerationService = taskGenerationService;
        this.memberGroupRepository = memberGroupRepository;
        this.roleDefinitionRepository = roleDefinitionRepository;
        this.roleAssignmentRepository = roleAssignmentRepository;
        this.groupMembershipRepository = groupMembershipRepository;
        this.groupCompositionRepository = groupCompositionRepository;
        this.taskDelegationRepository = taskDelegationRepository;
        this.taskParticipantRepository = taskParticipantRepository;
        this.taskAuthorizationService = taskAuthorizationService;
        this.taskAuditService = taskAuditService;
        this.taskRewardNegotiationService = taskRewardNegotiationService;
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
            Integer rewardReputationAmount,
            Integer penaltyReputationAmount,
            LocalTime dueTime,
            java.util.List<LocalTime> initialSlotTimes
    ) {
        taskAuthorizationService.requireCreate(workspaceId);
        Workspace workspace = getWorkspaceOrThrow(workspaceId);
        AssignmentPolicy policy = assignmentPolicy == null ? AssignmentPolicy.SINGLE_MEMBER : assignmentPolicy;

        WorkspaceMember assignedMember = assignedMemberId == null ? null : getMemberOrThrow(workspaceId, assignedMemberId);
        WorkspaceMember preferredMember = preferredMemberId == null ? null : getMemberOrThrow(workspaceId, preferredMemberId);
        MemberGroup targetGroup = targetGroupId == null ? null : memberGroupRepository.findByIdAndWorkspaceId(targetGroupId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Member group not found: " + targetGroupId));

        validateAssignment(policy, assignedMember, targetGroup, preferredMember);
        validateParticipantModel(administrators, observers, executors, policy);

        if (startDate == null) throw new IllegalArgumentException("Task start date must be specified");
        if (endDate != null && endDate.isBefore(startDate))
            throw new IllegalArgumentException("Task end date cannot be before start date");
        validateRecurrence(recurrenceType, recurrenceDayOfWeek, recurrenceDayOfMonth);

        PointType rewardPointType = getOptionalPointType(workspaceId, rewardPointTypeId);
        PointType penaltyPointType = getOptionalPointType(workspaceId, penaltyPointTypeId);
        validateAmountAndPointType(rewardPointTypeId, rewardAmount, "Reward");
        validateAmountAndPointType(penaltyPointTypeId, penaltyAmount, "Penalty");
        validateReputationAmount(rewardReputationAmount, "Reward reputation");
        validateReputationAmount(penaltyReputationAmount, "Penalty reputation");

        // Legacy assignedMember remains populated only for SINGLE_MEMBER until shared/open TaskInstance lifecycle is enabled.
        TaskDefinition definition = new TaskDefinition(
                workspace, policy == AssignmentPolicy.SINGLE_MEMBER ? assignedMember : null, title.trim(), description, mandatory,
                recurrenceType, startDate, endDate, recurrenceDayOfWeek, recurrenceDayOfMonth,
                rewardPointType, rewardAmount, penaltyPointType, penaltyAmount, dueTime);

        WorkspaceMember creator = currentUserService.getCurrentMember();
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
        definition.setRewardReputationAmount(rewardReputationAmount);
        definition.setPenaltyReputationAmount(penaltyReputationAmount);

        if (requiredGroupRoleIds != null && !requiredGroupRoleIds.isEmpty()) {
            if (targetGroup == null) throw new IllegalArgumentException("Group roles require targetGroupId");
            for (Long roleId : requiredGroupRoleIds) {
                RoleDefinition role = roleDefinitionRepository.findByIdAndWorkspaceId(roleId, workspaceId)
                        .filter(candidate -> isRoleAvailableInGroup(candidate, targetGroup.getId()))
                        .orElseThrow(() -> new ResourceNotFoundException("Role definition not available in target group: " + roleId));
                definition.getRequiredRoleDefinitions().add(role);
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

        // Persist schedule templates before any execution is generated.
        // No default execution must be created for definitions with explicit slots.
        if (initialSlotTimes != null && !initialSlotTimes.isEmpty()) {
            if (initialSlotTimes.size() > 12 || new java.util.HashSet<>(initialSlotTimes).size() != initialSlotTimes.size()
                    || initialSlotTimes.contains(null)) {
                throw new IllegalArgumentException("Slot times must be unique and non-null (max 12)");
            }
            for (int index = 0; index < initialSlotTimes.size(); index++) {
                taskScheduleSlots.save(new TaskScheduleSlot(savedDefinition,
                        "time_" + (index + 1), initialSlotTimes.get(index)));
            }
        } else if (policy == AssignmentPolicy.SINGLE_MEMBER) {
            taskGenerationService.generateForDefinition(savedDefinition, LocalDate.now());
        }

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
        validateParticipantModel(request.getAdministrators(), request.getObservers(), request.getExecutors(), policy);
        if (request.getStartDate() == null) throw new IllegalArgumentException("Task start date must be specified");
        if (request.getEndDate() != null && request.getEndDate().isBefore(request.getStartDate()))
            throw new IllegalArgumentException("Task end date cannot be before start date");
        validateRecurrence(request.getRecurrenceType(), request.getRecurrenceDayOfWeek(), request.getRecurrenceDayOfMonth());
        validateAmountAndPointType(request.getRewardPointTypeId(), request.getRewardAmount(), "Reward");
        validateAmountAndPointType(request.getPenaltyPointTypeId(), request.getPenaltyAmount(), "Penalty");
        validateReputationAmount(request.getRewardReputationAmount(), "Reward reputation");
        validateReputationAmount(request.getPenaltyReputationAmount(), "Penalty reputation");

        definition.setAssignmentPolicy(policy);
        definition.setAssignedMember(policy == AssignmentPolicy.SINGLE_MEMBER ? assignedMember : null);
        definition.setTargetGroup(targetGroup);
        definition.setPreferredMember(preferredMember);
        definition.setResponsibleMember(responsible);
        definition.setDelegationAllowed(request.isDelegationAllowed());
        definition.setRoleMatchMode(request.getRoleMatchMode() == null ? RoleMatchMode.ANY : request.getRoleMatchMode());
        definition.getRequiredRoleDefinitions().clear();
        if (request.getRequiredGroupRoleIds() != null && !request.getRequiredGroupRoleIds().isEmpty()) {
            if (targetGroup == null) throw new IllegalArgumentException("Group roles require targetGroupId");
            for (Long roleId : request.getRequiredGroupRoleIds()) {
                RoleDefinition role = roleDefinitionRepository.findByIdAndWorkspaceId(roleId, workspaceId)
                        .filter(candidate -> isRoleAvailableInGroup(candidate, targetGroup.getId()))
                        .orElseThrow(() -> new ResourceNotFoundException("Role definition not available in target group: " + roleId));
                definition.getRequiredRoleDefinitions().add(role);
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
        definition.setRewardReputationAmount(request.getRewardReputationAmount());
        definition.setPenaltyReputationAmount(request.getPenaltyReputationAmount());

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
                currentUserService.getCurrentMember(),
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
            WorkspaceMember performedBy = currentUserService.getCurrentMember();
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
            if ((role == TaskParticipantRole.ADMIN || role == TaskParticipantRole.OBSERVER)
                    && actor.getActorType() != ActorType.MEMBER) {
                throw new IllegalArgumentException(role + " participants must be workspace members");
            }
            validateActor(definition.getWorkspace().getId(), actor.getActorType(), actor.getActorId());
            addParticipant(definition, role, actor.getActorType(), actor.getActorId());
        }
    }

    private void validateParticipantModel(
            java.util.List<com.olehkobylianskyi.familypoints.dto.TaskActorRef> administrators,
            java.util.List<com.olehkobylianskyi.familypoints.dto.TaskActorRef> observers,
            java.util.List<com.olehkobylianskyi.familypoints.dto.TaskActorRef> executors,
            AssignmentPolicy policy
    ) {
        if (administrators != null && administrators.stream()
                .filter(java.util.Objects::nonNull)
                .anyMatch(actor -> actor.getActorType() != ActorType.MEMBER)) {
            throw new IllegalArgumentException("Task administrators can only be workspace members");
        }
        if (observers != null && observers.stream()
                .filter(java.util.Objects::nonNull)
                .anyMatch(actor -> actor.getActorType() != ActorType.MEMBER)) {
            throw new IllegalArgumentException("Task observers can only be workspace members");
        }
        long executorCount = executors == null ? 0 : executors.stream()
                .filter(java.util.Objects::nonNull)
                .filter(actor -> actor.getActorType() != null && actor.getActorId() != null)
                .count();
        if (policy == AssignmentPolicy.PARTICIPANTS && executorCount == 0) {
            throw new IllegalArgumentException("At least one executor is required");
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
        WorkspaceMember me = currentUserService.getCurrentMember();
        var all = taskDefinitionRepository.findByWorkspaceIdOrderByIdAsc(workspaceId);
        if ("created".equalsIgnoreCase(view)) return all.stream().filter(d -> d.getCreatedBy()!=null && d.getCreatedBy().getId().equals(me.getId())).toList();
        TaskParticipantRole role = switch (view == null ? "" : view.toLowerCase()) {
            case "admin" -> TaskParticipantRole.ADMIN;
            case "observer" -> TaskParticipantRole.OBSERVER;
            case "executor" -> TaskParticipantRole.EXECUTOR;
            default -> null;
        };
        if (role == null) return all.stream().filter(taskAuthorizationService::canRead).toList();
        // The 'admin' view includes effective managers, not only explicit ADMIN participants.
        // Authors, workspace task managers and scoped group managers otherwise disappear here.
        if (role == TaskParticipantRole.ADMIN) {
            return all.stream().filter(taskAuthorizationService::canManage).toList();
        }
        return all.stream()
                .filter(taskAuthorizationService::canRead)
                .filter(d -> participantMatchesMember(d.getId(), role, me.getId(), new java.util.HashSet<>()))
                .toList();
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
            case OPEN_WORKSPACE, PARTICIPANTS -> { }
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

    public boolean canManageDefinition(TaskDefinition definition) {
        return taskAuthorizationService.canManage(definition);
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
                .findByTaskDefinitionIdAndScheduledDateAndSlotKey(definitionId, scheduledDate, "default")
                .orElse(null);
        if (existing != null) {
            return existing;
        }

        TaskInstance created = taskInstanceRepository.save(
                new TaskInstance(definition, definition.getAssignedMember(), scheduledDate)
        );
        taskAuditService.recordUserEvent(
                definition, created, TaskAuditEventType.INSTANCE_CREATED,
                currentUserService.getCurrentMember(),
                null, null, ActorType.MEMBER, created.getMember().getId(),
                "Created for " + scheduledDate
        );
        return created;
    }

    /** System-only materialization of an overdue slot; the deadline worker applies penalties separately. */
    @Transactional
    public TaskInstance materializeOverdueSlot(Long workspaceId, Long definitionId, LocalDate date, String slotKey) {
        TaskDefinition definition = taskDefinitionRepository.findForOccurrenceUpdate(definitionId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Task definition not found"));
        if (!definition.isActive() || !definition.isMandatory()
                || definition.getAssignedMember() == null
                || !taskGenerationService.shouldGenerate(definition, date)) {
            throw new InvalidTaskStateException("Task occurrence is not eligible for deadline processing");
        }
        var slot = taskScheduleSlots.findByTaskDefinitionIdAndSlotKeyAndEnabledTrue(definitionId, slotKey)
                .orElseThrow(() -> new InvalidTaskStateException("Unknown scheduled slot"));
        if (slot.getScheduledTime() == null
                || java.time.LocalDateTime.now().isBefore(java.time.LocalDateTime.of(date, slot.getScheduledTime()))) {
            throw new InvalidTaskStateException("Task occurrence is not overdue");
        }
        return taskInstanceRepository.findByTaskDefinitionIdAndScheduledDateAndSlotKey(definitionId, date, slotKey)
                .orElseGet(() -> taskInstanceRepository.save(
                        new TaskInstance(definition, definition.getAssignedMember(), date)
                                .withSlot(slotKey, slot.getScheduledTime())));
    }

    @Transactional
    public TaskInstance startScheduledOccurrence(Long workspaceId, Long definitionId, LocalDate date) {
        return startScheduledOccurrence(workspaceId, definitionId, date, "default");
    }

    @Transactional
    public TaskInstance startScheduledOccurrence(Long workspaceId, Long definitionId, LocalDate date, String slotKey) {
        taskAuthorizationService.requireWorkspace(workspaceId);
        TaskDefinition definition = taskDefinitionRepository.findForOccurrenceUpdate(definitionId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Task definition not found: " + definitionId));
        WorkspaceMember member = taskAuthorizationService.currentMember();
        if (definition.getAssignedMember() == null
                || !definition.getAssignedMember().getId().equals(member.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Only assigned member may start this task");
        }
        if (!definition.isActive() || !taskGenerationService.shouldGenerate(definition, date)
                || date.isAfter(LocalDate.now())) {
            throw new InvalidTaskStateException("Task is not available for this date");
        }
        if ("default".equals(slotKey) && !taskScheduleSlots.findByTaskDefinitionIdOrderByScheduledTimeAscSlotKeyAsc(definitionId).isEmpty()) {
            throw new InvalidTaskStateException("Implicit default occurrence is disabled when explicit slots exist");
        }
        java.time.LocalTime slotTime = definition.getDueTime();
        if (!"default".equals(slotKey)) {
            var slot = taskScheduleSlots.findByTaskDefinitionIdAndSlotKeyAndEnabledTrue(definitionId, slotKey)
                    .orElseThrow(() -> new InvalidTaskStateException("Unknown or disabled task schedule slot"));
            slotTime = slot.getScheduledTime();
        }
        final java.time.LocalTime dueTime = slotTime;
        TaskInstance instance = taskInstanceRepository.findByTaskDefinitionIdAndScheduledDateAndSlotKey(definitionId, date, slotKey)
                .orElseGet(() -> taskInstanceRepository.save(
                        new TaskInstance(definition, member, date).withSlot(slotKey, dueTime)));
        if (instance.getStatus() == TaskInstanceStatus.PENDING) {
            instance.start();
            taskAuditService.recordSystemEvent(definition, instance, TaskAuditEventType.INSTANCE_STARTED,
                    null, null, ActorType.MEMBER, member.getId(), "Started scheduled slot " + slotKey);
        } else if (instance.getStatus() != TaskInstanceStatus.IN_PROGRESS) {
            throw new InvalidTaskStateException("Occurrence has already been resolved");
        }
        return instance;
    }

    @Transactional(readOnly = true)
    public TaskInstance getInstanceForDefinitionDate(Long workspaceId, Long definitionId, LocalDate date) {
        return getInstanceForDefinitionDate(workspaceId, definitionId, date, "default");
    }

    @Transactional(readOnly = true)
    public TaskInstance getInstanceForDefinitionDate(Long workspaceId, Long definitionId, LocalDate date, String slotKey) {
        TaskDefinition definition = getDefinitionOrThrow(workspaceId, definitionId);
        taskAuthorizationService.requireRead(definition);
        return taskInstanceRepository.findByTaskDefinitionIdAndScheduledDateAndSlotKey(definitionId, date, slotKey).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<TaskInstance> getMemberTasks(
            Long workspaceId,
            Long memberId,
            LocalDate date
    ) {
        getMemberOrThrow(workspaceId, memberId);
        WorkspaceMember me = taskAuthorizationService.currentMember();
        if (!memberId.equals(me.getId())
                && !me.hasPermission(WorkspacePermission.VIEW_ALL_TASKS)
                && !me.hasPermission(WorkspacePermission.ADMIN_OVERRIDE)) {
            throw new org.springframework.security.access.AccessDeniedException("Cannot view another member's executions");
        }

        return taskInstanceRepository
                .findByMemberIdAndScheduledDateOrderByIdAsc(memberId, date)
                .stream()
                .filter(instance -> taskAuthorizationService.canRead(instance.getTaskDefinition()))
                .toList();
    }

    /**
     * Read-only calendar view: recurring definitions appear on their due dates
     * without writing an instance. Existing instance status takes precedence.
     */
    @Transactional(readOnly = true)
    public List<com.olehkobylianskyi.familypoints.dto.TaskScheduleOccurrenceResponse> getMemberSchedule(
            Long workspaceId, Long memberId, LocalDate date) {
        getWorkspaceOrThrow(workspaceId);
        WorkspaceMember member = getMemberOrThrow(workspaceId, memberId);
        WorkspaceMember current = taskAuthorizationService.currentMember();
        if (!member.getId().equals(current.getId())
                && !current.hasPermission(WorkspacePermission.VIEW_ALL_TASKS)
                && !current.hasPermission(WorkspacePermission.ADMIN_OVERRIDE)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Current user cannot inspect another member's schedule");
        }

        java.util.Map<String, com.olehkobylianskyi.familypoints.dto.TaskScheduleOccurrenceResponse> occurrences =
                new java.util.LinkedHashMap<>();
        for (TaskDefinition definition : taskDefinitionRepository.findByWorkspaceIdAndActiveTrueOrderByIdAsc(workspaceId)) {
            if (definition.getAssignedMember() == null
                    || !definition.getAssignedMember().getId().equals(memberId)
                    || !taskAuthorizationService.canRead(definition)
                    || !taskGenerationService.shouldGenerate(definition, date)) continue;
            var slots = taskScheduleSlots.findByTaskDefinitionIdAndEnabledTrueOrderByScheduledTimeAscSlotKeyAsc(definition.getId());
            if (slots.isEmpty() && taskScheduleSlots.findByTaskDefinitionIdOrderByScheduledTimeAscSlotKeyAsc(definition.getId()).isEmpty()) {
                var occurrence = com.olehkobylianskyi.familypoints.dto.TaskScheduleOccurrenceResponse.planned(definition, memberId, date);
                occurrences.put(occurrence.occurrenceKey(), occurrence);
            } else {
                for (var slot : slots) {
                    var occurrence = com.olehkobylianskyi.familypoints.dto.TaskScheduleOccurrenceResponse.plannedSlot(
                            definition, memberId, date, slot.getSlotKey(), slot.getScheduledTime());
                    occurrences.put(occurrence.occurrenceKey(), occurrence);
                }
            }
        }
        for (TaskInstance instance : taskInstanceRepository.findByMemberIdAndScheduledDateOrderByIdAsc(memberId, date)) {
            if (!taskAuthorizationService.canRead(instance.getTaskDefinition())) continue;
            var existing = com.olehkobylianskyi.familypoints.dto.TaskScheduleOccurrenceResponse.existing(instance);
            occurrences.put(existing.occurrenceKey(), existing);
        }
        return List.copyOf(occurrences.values());
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
                .filter(taskAuthorizationService::canRead)
                .filter(d -> d.getAssignmentPolicy() != AssignmentPolicy.SINGLE_MEMBER)
                .filter(d -> d.getAssignmentPolicy() != AssignmentPolicy.GROUP_SHARED)
                .filter(d -> taskGenerationService.shouldGenerate(d, date))
                .filter(d -> taskInstanceRepository.findByTaskDefinitionIdAndScheduledDateAndSlotKey(d.getId(), date, "default")
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
        TaskInstance instance = taskInstanceRepository.findByTaskDefinitionIdAndScheduledDateAndSlotKey(definitionId, date, "default")
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
        WorkspaceMember delegatedBy = currentUserService.getCurrentMember();
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
        if (policy == AssignmentPolicy.PARTICIPANTS) {
            return participantMatchesMember(
                    definition.getId(),
                    TaskParticipantRole.EXECUTOR,
                    member.getId(),
                    new java.util.HashSet<>()
            );
        }
        if (policy == AssignmentPolicy.OPEN_WORKSPACE || policy == AssignmentPolicy.PREFERRED_MEMBER) {
            if (definition.getTargetGroup() == null) return true;
        }
        if (policy == AssignmentPolicy.OPEN_GROUP || definition.getTargetGroup() != null) {
            return memberMatchesGroup(definition.getTargetGroup().getId(), member.getId(), definition.getRequiredRoleDefinitions(), definition.getRoleMatchMode(), new java.util.HashSet<>());
        }
        return policy == AssignmentPolicy.SINGLE_MEMBER && definition.getAssignedMember() != null
                && definition.getAssignedMember().getId().equals(member.getId());
    }

    private boolean memberMatchesGroup(Long groupId, Long memberId, Set<RoleDefinition> requiredRoles, RoleMatchMode mode, Set<Long> visited) {
        if (!visited.add(groupId)) return false;
        var membership = groupMembershipRepository.findByMemberGroupIdAndMemberId(groupId, memberId).orElse(null);
        if (membership != null && membership.isActive()) {
            if (requiredRoles == null || requiredRoles.isEmpty()) return true;
            Set<Long> assignedRoleIds = roleAssignmentRepository
                    .findByWorkspaceIdAndActorTypeAndActorIdAndContextTypeAndContextIdAndActiveTrue(
                            membership.getMemberGroup().getWorkspace().getId(),
                            ActorType.MEMBER,
                            memberId,
                            RoleContextType.MEMBER_GROUP,
                            groupId
                    )
                    .stream()
                    .map(a -> a.getRoleDefinition().getId())
                    .collect(java.util.stream.Collectors.toSet());
            long matched = requiredRoles.stream().filter(r -> assignedRoleIds.contains(r.getId())).count();
            if ((mode == null || mode == RoleMatchMode.ANY) && matched > 0) return true;
            if (mode == RoleMatchMode.ALL && matched == requiredRoles.size()) return true;
        }
        for (GroupComposition composition : groupCompositionRepository.findByParentGroupIdAndActiveTrueOrderByIdAsc(groupId)) {
            if (memberMatchesGroup(composition.getChildGroup().getId(), memberId, requiredRoles, mode, visited)) return true;
        }
        return false;
    }

    private boolean isRoleAvailableInGroup(RoleDefinition role, Long groupId) {
        if (!role.isActive()) return false;
        if (role.getVisibility() == RoleVisibility.SHARED) return true;
        return role.getOwnerContextType() == RoleContextType.MEMBER_GROUP
                && java.util.Objects.equals(role.getOwnerContextId(), groupId);
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
                currentUserService.getCurrentMember(),
                null, null, ActorType.MEMBER, instance.getMember().getId(), null
        );

        return instance;
    }

    /**
     * Parent/manager confirmation for a specific scheduled date.
     * Never change a definition's recurrence or overwrite resolved instances.
     */
    @Transactional
    public TaskInstance completeScheduledByManager(Long workspaceId, Long definitionId, LocalDate date) {
        return completeScheduledByManager(workspaceId, definitionId, date, "default");
    }

    @Transactional
    public TaskInstance completeScheduledByManager(Long workspaceId, Long definitionId, LocalDate date, String slotKey) {
        TaskDefinition definition = taskDefinitionRepository.findForOccurrenceUpdate(definitionId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Task definition not found"));
        taskAuthorizationService.requireManage(definition);
        if (currentUserService.getCurrentMember().getMemberType() == WorkspaceMemberType.CHILD)
            throw new org.springframework.security.access.AccessDeniedException("ADULT_TASK_APPROVAL_REQUIRED");
        if (!definition.isActive() || !taskGenerationService.shouldGenerate(definition, date)
                || definition.getAssignedMember() == null) {
            throw new InvalidTaskStateException("Task is not scheduled for this date");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new InvalidTaskStateException("Future occurrences cannot be completed");
        }
        if ("default".equals(slotKey) && !taskScheduleSlots.findByTaskDefinitionIdOrderByScheduledTimeAscSlotKeyAsc(definitionId).isEmpty()) {
            throw new InvalidTaskStateException("Implicit default occurrence is disabled when explicit slots exist");
        }
        java.time.LocalTime dueTime = definition.getDueTime();
        if (!"default".equals(slotKey)) {
            var slot = taskScheduleSlots.findByTaskDefinitionIdAndSlotKeyAndEnabledTrue(definitionId, slotKey)
                    .orElseThrow(() -> new InvalidTaskStateException("Unknown or disabled task slot"));
            dueTime = slot.getScheduledTime();
        }
        final java.time.LocalTime selectedDueTime = dueTime;
        TaskInstance instance = taskInstanceRepository.findByTaskDefinitionIdAndScheduledDateAndSlotKey(definitionId, date, slotKey)
                .orElseGet(() -> taskInstanceRepository.save(
                        new TaskInstance(definition, definition.getAssignedMember(), date).withSlot(slotKey, selectedDueTime)));
        if (instance.getStatus() == TaskInstanceStatus.COMPLETED) return instance;
        if (instance.getStatus() != TaskInstanceStatus.PENDING
                && instance.getStatus() != TaskInstanceStatus.IN_PROGRESS) {
            throw new InvalidTaskStateException("This occurrence cannot be completed from status " + instance.getStatus());
        }
        instance.complete();
        processReward(workspaceId, instance);
        taskAuditService.recordUserEvent(definition, instance, TaskAuditEventType.INSTANCE_COMPLETED,
                currentUserService.getCurrentMember(), null, null,
                ActorType.MEMBER, instance.getMember().getId(), "Confirmed by task manager");
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
                currentUserService.getCurrentMember(),
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
                currentUserService.getCurrentMember(),
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
                currentUserService.getCurrentMember(),
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
                currentUserService.getCurrentMember(),
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

        // Child completion is a request, never a reward-minting action.
        if (currentUserService.getCurrentMember().getMemberType() == WorkspaceMemberType.CHILD) {
            instance.requestApproval();
            return instance;
        }

        instance.complete();

        processReward(
                workspaceId,
                instance
        );

        taskAuditService.recordUserEvent(
                instance.getTaskDefinition(), instance, TaskAuditEventType.INSTANCE_COMPLETED,
                currentUserService.getCurrentMember(),
                null, null, ActorType.MEMBER, instance.getMember().getId(), null
        );

        return instance;
    }


    /** Independent adult approval releases points and reputation only after the child requests completion. */
    @Transactional
    public TaskInstance approveChildCompletion(Long workspaceId, Long instanceId) {
        TaskInstance instance = getInstanceForUpdateOrThrow(workspaceId, instanceId);
        WorkspaceMember approver = currentUserService.getCurrentMember();
        if (approver.getMemberType() == WorkspaceMemberType.CHILD
                || (!approver.hasPermission(WorkspacePermission.APPROVE_TASKS)
                    && !approver.hasPermission(WorkspacePermission.MANAGE_TASKS)
                    && !approver.hasPermission(WorkspacePermission.ADMIN_OVERRIDE)))
            throw new org.springframework.security.access.AccessDeniedException(
                    "ADULT_TASK_APPROVAL_REQUIRED");
        if (instance.getStatus() != TaskInstanceStatus.WAITING_APPROVAL)
            throw new InvalidTaskStateException("Task is not waiting for adult approval");
        instance.complete();
        processReward(workspaceId, instance);
        taskAuditService.recordUserEvent(instance.getTaskDefinition(), instance,
                TaskAuditEventType.INSTANCE_COMPLETED, approver,
                null, null, ActorType.MEMBER, instance.getMember().getId(),
                "Approved by adult");
        return instance;
    }

    private void processReward(
            Long workspaceId,
            TaskInstance instance
    ) {
        if (instance.isRewardProcessed()) {
            return;
        }

        if (taskRewardNegotiationService.processApprovedOverride(workspaceId, instance)) {
            instance.markRewardProcessed();
            return;
        }

        Integer rewardAmount =
                instance.getRewardAmount();

        PointType rewardPointType =
                instance.getRewardPointType();

        if (rewardAmount != null
                && rewardAmount > 0
                && rewardPointType != null) {
            pointService.earn(
                    workspaceId,
                    instance.getMember().getId(),
                    rewardPointType.getId(),
                    rewardAmount,
                    PointTransactionSourceType.TASK,
                    instance.getId(),
                    "Task completed: " + instance.getTitle()
            );
        }

        Integer reputationAmount = instance.getRewardReputationAmount();
        if (reputationAmount != null && reputationAmount > 0) {
            reputationService.add(
                    workspaceId,
                    instance.getMember().getId(),
                    reputationAmount,
                    ReputationTransactionSourceType.TASK,
                    instance.getId(),
                    "Task completed: " + instance.getTitle()
            );
        }

        instance.markRewardProcessed();
    }

    private void validateReputationAmount(Integer amount, String fieldName) {
        if (amount != null && amount < 0) {
            throw new IllegalArgumentException(fieldName + " cannot be negative");
        }
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
                currentUserService.getCurrentMember(),
                null, null, ActorType.MEMBER, instance.getMember().getId(), null
        );

        return instance;
    }

    /** System-only deadline transition; no user or principal is available in a scheduled job. */
    @Transactional
    public TaskInstance missBySystem(Long workspaceId, Long instanceId) {
        TaskInstance instance = getInstanceForUpdateOrThrow(workspaceId, instanceId);
        if (instance.getStatus() != TaskInstanceStatus.PENDING
                && instance.getStatus() != TaskInstanceStatus.IN_PROGRESS) return instance;
        List<MemberExceptionPeriod> exceptions = memberExceptionPeriodRepository.findActiveForDate(
                instance.getMember().getId(), instance.getScheduledDate());
        if (!exceptions.isEmpty()) {
            instance.excuse(TaskExcuseReason.EXCEPTION_PERIOD, "Automatically excused: exception period");
            taskAuditService.recordSystemEvent(instance.getTaskDefinition(), instance,
                    TaskAuditEventType.INSTANCE_EXCUSED, null, null,
                    ActorType.MEMBER, instance.getMember().getId(), "Deadline exception period");
            return instance;
        }
        instance.markMissed();
        processPenalty(workspaceId, instance);
        taskAuditService.recordSystemEvent(instance.getTaskDefinition(), instance,
                TaskAuditEventType.INSTANCE_MISSED, null, null,
                ActorType.MEMBER, instance.getMember().getId(), "Automatic deadline");
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

        if (penaltyAmount != null
                && penaltyAmount > 0
                && penaltyPointType != null) {
            pointService.penalty(
                    workspaceId,
                    instance.getMember().getId(),
                    penaltyPointType,
                    penaltyAmount,
                    PointTransactionSourceType.TASK,
                    instance.getId(),
                    "Task missed: " + instance.getTitle()
            );
        }

        Integer reputationAmount = instance.getPenaltyReputationAmount();
        if (reputationAmount != null && reputationAmount > 0) {
            reputationService.add(
                    workspaceId,
                    instance.getMember().getId(),
                    -reputationAmount,
                    ReputationTransactionSourceType.TASK,
                    instance.getId(),
                    "Task missed: " + instance.getTitle()
            );
        }

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
                currentUserService.getCurrentMember(),
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