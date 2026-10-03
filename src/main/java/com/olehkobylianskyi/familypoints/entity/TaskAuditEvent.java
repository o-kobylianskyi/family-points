package com.olehkobylianskyi.familypoints.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "task_audit_events",
        indexes = {
                @Index(name = "idx_task_audit_definition_time", columnList = "task_definition_id, occurred_at"),
                @Index(name = "idx_task_audit_instance_time", columnList = "task_instance_id, occurred_at")
        }
)
public class TaskAuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_definition_id", nullable = false)
    private TaskDefinition taskDefinition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_instance_id")
    private TaskInstance taskInstance;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private TaskAuditEventType eventType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by_member_id")
    private WorkspaceMember performedBy;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_actor_type", length = 20)
    private ActorType fromActorType;

    @Column(name = "from_actor_id")
    private Long fromActorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_actor_type", length = 20)
    private ActorType toActorType;

    @Column(name = "to_actor_id")
    private Long toActorId;

    @Column(length = 1000)
    private String details;

    protected TaskAuditEvent() {
    }

    public TaskAuditEvent(
            Workspace workspace,
            TaskDefinition taskDefinition,
            TaskInstance taskInstance,
            TaskAuditEventType eventType,
            WorkspaceMember performedBy,
            ActorType fromActorType,
            Long fromActorId,
            ActorType toActorType,
            Long toActorId,
            String details
    ) {
        this.workspace = workspace;
        this.taskDefinition = taskDefinition;
        this.taskInstance = taskInstance;
        this.eventType = eventType;
        this.performedBy = performedBy;
        this.fromActorType = fromActorType;
        this.fromActorId = fromActorId;
        this.toActorType = toActorType;
        this.toActorId = toActorId;
        this.details = details;
        this.occurredAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Workspace getWorkspace() { return workspace; }
    public TaskDefinition getTaskDefinition() { return taskDefinition; }
    public TaskInstance getTaskInstance() { return taskInstance; }
    public TaskAuditEventType getEventType() { return eventType; }
    public WorkspaceMember getPerformedBy() { return performedBy; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public ActorType getFromActorType() { return fromActorType; }
    public Long getFromActorId() { return fromActorId; }
    public ActorType getToActorType() { return toActorType; }
    public Long getToActorId() { return toActorId; }
    public String getDetails() { return details; }
}
