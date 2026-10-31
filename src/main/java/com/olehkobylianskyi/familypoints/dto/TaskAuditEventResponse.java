package com.olehkobylianskyi.familypoints.dto;

import com.olehkobylianskyi.familypoints.entity.ActorType;
import com.olehkobylianskyi.familypoints.entity.TaskAuditEvent;
import com.olehkobylianskyi.familypoints.entity.TaskAuditEventType;

import java.time.LocalDateTime;

public class TaskAuditEventResponse {

    private final Long id;
    private final Long taskDefinitionId;
    private final Long taskInstanceId;
    private final TaskAuditEventType eventType;
    private final Long performedByMemberId;
    private final String performedByName;
    private final LocalDateTime occurredAt;
    private final ActorType fromActorType;
    private final Long fromActorId;
    private final String fromActorName;
    private final ActorType toActorType;
    private final Long toActorId;
    private final String toActorName;
    private final String details;

    public TaskAuditEventResponse(
            TaskAuditEvent event,
            String performedByName,
            String fromActorName,
            String toActorName
    ) {
        this.id = event.getId();
        this.taskDefinitionId = event.getTaskDefinition().getId();
        this.taskInstanceId = event.getTaskInstance() == null ? null : event.getTaskInstance().getId();
        this.eventType = event.getEventType();
        this.performedByMemberId = event.getPerformedBy() == null ? null : event.getPerformedBy().getId();
        this.performedByName = performedByName;
        this.occurredAt = event.getOccurredAt();
        this.fromActorType = event.getFromActorType();
        this.fromActorId = event.getFromActorId();
        this.fromActorName = fromActorName;
        this.toActorType = event.getToActorType();
        this.toActorId = event.getToActorId();
        this.toActorName = toActorName;
        this.details = event.getDetails();
    }

    public Long getId() { return id; }
    public Long getTaskDefinitionId() { return taskDefinitionId; }
    public Long getTaskInstanceId() { return taskInstanceId; }
    public TaskAuditEventType getEventType() { return eventType; }
    public Long getPerformedByMemberId() { return performedByMemberId; }
    public String getPerformedByName() { return performedByName; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public ActorType getFromActorType() { return fromActorType; }
    public Long getFromActorId() { return fromActorId; }
    public String getFromActorName() { return fromActorName; }
    public ActorType getToActorType() { return toActorType; }
    public Long getToActorId() { return toActorId; }
    public String getToActorName() { return toActorName; }
    public String getDetails() { return details; }
}
