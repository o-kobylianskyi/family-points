package com.olehkobylianskyi.familypoints.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "task_participants", uniqueConstraints = @UniqueConstraint(
        name = "uk_task_participant_definition_role_actor",
        columnNames = {"task_definition_id", "role", "actor_type", "actor_id"}
))
public class TaskParticipant {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_definition_id", nullable = false)
    private TaskDefinition taskDefinition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskParticipantRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 10)
    private ActorType actorType;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    protected TaskParticipant() {}

    public TaskParticipant(TaskDefinition taskDefinition, TaskParticipantRole role, ActorType actorType, Long actorId) {
        this.taskDefinition = taskDefinition;
        this.role = role;
        this.actorType = actorType;
        this.actorId = actorId;
    }

    public Long getId() { return id; }
    public TaskDefinition getTaskDefinition() { return taskDefinition; }
    public TaskParticipantRole getRole() { return role; }
    public ActorType getActorType() { return actorType; }
    public Long getActorId() { return actorId; }
}
