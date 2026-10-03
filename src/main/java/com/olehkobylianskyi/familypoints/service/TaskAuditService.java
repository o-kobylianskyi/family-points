package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.*;
import com.olehkobylianskyi.familypoints.dto.TaskAuditEventResponse;
import com.olehkobylianskyi.familypoints.exception.ResourceNotFoundException;
import com.olehkobylianskyi.familypoints.repository.MemberGroupRepository;
import com.olehkobylianskyi.familypoints.repository.TaskAuditEventRepository;
import com.olehkobylianskyi.familypoints.repository.TaskDefinitionRepository;
import com.olehkobylianskyi.familypoints.repository.TaskInstanceRepository;
import com.olehkobylianskyi.familypoints.repository.WorkspaceMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TaskAuditService {

    private final TaskAuditEventRepository taskAuditEventRepository;
    private final TaskDefinitionRepository taskDefinitionRepository;
    private final TaskInstanceRepository taskInstanceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final MemberGroupRepository memberGroupRepository;
    private final TaskAuthorizationService taskAuthorizationService;

    public TaskAuditService(
            TaskAuditEventRepository taskAuditEventRepository,
            TaskDefinitionRepository taskDefinitionRepository,
            TaskInstanceRepository taskInstanceRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            MemberGroupRepository memberGroupRepository,
            TaskAuthorizationService taskAuthorizationService
    ) {
        this.taskAuditEventRepository = taskAuditEventRepository;
        this.taskDefinitionRepository = taskDefinitionRepository;
        this.taskInstanceRepository = taskInstanceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.memberGroupRepository = memberGroupRepository;
        this.taskAuthorizationService = taskAuthorizationService;
    }

    @Transactional(readOnly = true)
    public List<TaskAuditEventResponse> getDefinitionHistory(Long workspaceId, Long definitionId) {
        TaskDefinition definition = taskDefinitionRepository.findByIdAndWorkspaceId(definitionId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Task definition " + definitionId + " not found in workspace " + workspaceId));
        taskAuthorizationService.requireRead(definition);
        return taskAuditEventRepository.findByTaskDefinitionIdOrderByOccurredAtAscIdAsc(definitionId)
                .stream().map(event -> toResponse(workspaceId, event)).toList();
    }

    @Transactional(readOnly = true)
    public List<TaskAuditEventResponse> getInstanceHistory(Long workspaceId, Long instanceId) {
        TaskInstance instance = taskInstanceRepository.findById(instanceId)
                .filter(value -> value.getTaskDefinition().getWorkspace().getId().equals(workspaceId))
                .orElseThrow(() -> new ResourceNotFoundException("Task instance " + instanceId + " not found in workspace " + workspaceId));
        taskAuthorizationService.requireWorkspace(workspaceId);
        if (!taskAuthorizationService.canRead(instance.getTaskDefinition())
                && !taskAuthorizationService.canExecute(instance)) {
            throw new org.springframework.security.access.AccessDeniedException("Current user cannot view this task history");
        }
        return taskAuditEventRepository.findByTaskInstanceIdOrderByOccurredAtAscIdAsc(instanceId)
                .stream().map(event -> toResponse(workspaceId, event)).toList();
    }

    private TaskAuditEventResponse toResponse(Long workspaceId, TaskAuditEvent event) {
        String performedByName = event.getPerformedBy() == null ? null : event.getPerformedBy().getName();
        return new TaskAuditEventResponse(
                event,
                performedByName,
                actorName(workspaceId, event.getFromActorType(), event.getFromActorId()),
                actorName(workspaceId, event.getToActorType(), event.getToActorId())
        );
    }

    private String actorName(Long workspaceId, ActorType type, Long id) {
        if (type == null || id == null) return null;
        if (type == ActorType.MEMBER) {
            return workspaceMemberRepository.findByIdAndWorkspaceId(id, workspaceId)
                    .map(WorkspaceMember::getName).orElse("#" + id);
        }
        return memberGroupRepository.findByIdAndWorkspaceId(id, workspaceId)
                .map(MemberGroup::getName).orElse("#" + id);
    }

    public TaskAuditEvent recordUserEvent(
            TaskDefinition definition,
            TaskInstance instance,
            TaskAuditEventType eventType,
            WorkspaceMember performedBy,
            ActorType fromActorType,
            Long fromActorId,
            ActorType toActorType,
            Long toActorId,
            String details
    ) {
        if (performedBy == null) {
            throw new IllegalArgumentException("performedBy is required for a user audit event");
        }

        return save(definition, instance, eventType, performedBy,
                fromActorType, fromActorId, toActorType, toActorId, details);
    }

    public TaskAuditEvent recordSystemEvent(
            TaskDefinition definition,
            TaskInstance instance,
            TaskAuditEventType eventType,
            ActorType fromActorType,
            Long fromActorId,
            ActorType toActorType,
            Long toActorId,
            String details
    ) {
        return save(definition, instance, eventType, null,
                fromActorType, fromActorId, toActorType, toActorId, details);
    }

    private TaskAuditEvent save(
            TaskDefinition definition,
            TaskInstance instance,
            TaskAuditEventType eventType,
            WorkspaceMember performedBy,
            ActorType fromActorType,
            Long fromActorId,
            ActorType toActorType,
            Long toActorId,
            String details
    ) {
        if (definition == null || definition.getWorkspace() == null) {
            throw new IllegalArgumentException("Task definition with workspace is required");
        }

        TaskAuditEvent event = new TaskAuditEvent(
                definition.getWorkspace(),
                definition,
                instance,
                eventType,
                performedBy,
                fromActorType,
                fromActorId,
                toActorType,
                toActorId,
                details
        );

        return taskAuditEventRepository.save(event);
    }
}
