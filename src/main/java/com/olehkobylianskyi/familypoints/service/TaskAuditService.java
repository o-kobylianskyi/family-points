package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.*;
import com.olehkobylianskyi.familypoints.repository.TaskAuditEventRepository;
import org.springframework.stereotype.Service;

@Service
public class TaskAuditService {

    private final TaskAuditEventRepository taskAuditEventRepository;

    public TaskAuditService(TaskAuditEventRepository taskAuditEventRepository) {
        this.taskAuditEventRepository = taskAuditEventRepository;
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
