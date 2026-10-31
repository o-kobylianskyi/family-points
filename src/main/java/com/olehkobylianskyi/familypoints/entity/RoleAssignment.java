package com.olehkobylianskyi.familypoints.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "role_assignments")
public class RoleAssignment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_definition_id", nullable = false)
    private RoleDefinition roleDefinition;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 20)
    private ActorType actorType;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "context_type", nullable = false, length = 30)
    private RoleContextType contextType;

    @Column(name = "context_id")
    private Long contextId;

    @Column(nullable = false)
    private boolean active = true;

    protected RoleAssignment() {}

    public RoleAssignment(Workspace workspace, RoleDefinition roleDefinition, ActorType actorType, Long actorId,
                          RoleContextType contextType, Long contextId) {
        this.workspace = workspace;
        this.roleDefinition = roleDefinition;
        this.actorType = actorType;
        this.actorId = actorId;
        this.contextType = contextType;
        this.contextId = contextId;
    }

    public Long getId() { return id; }
    public Workspace getWorkspace() { return workspace; }
    public RoleDefinition getRoleDefinition() { return roleDefinition; }
    public ActorType getActorType() { return actorType; }
    public Long getActorId() { return actorId; }
    public RoleContextType getContextType() { return contextType; }
    public Long getContextId() { return contextId; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
