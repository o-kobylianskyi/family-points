package com.olehkobylianskyi.familypoints.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "role_set_bindings")
public class RoleSetBinding {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_set_id", nullable = false)
    private RoleSet roleSet;

    @Enumerated(EnumType.STRING)
    @Column(name = "context_type", nullable = false, length = 30)
    private RoleContextType contextType;

    @Column(name = "context_id")
    private Long contextId;

    protected RoleSetBinding() {}

    public RoleSetBinding(RoleSet roleSet, RoleContextType contextType, Long contextId) {
        this.roleSet = roleSet;
        this.contextType = contextType;
        this.contextId = contextId;
    }

    public Long getId() { return id; }
    public RoleSet getRoleSet() { return roleSet; }
    public RoleContextType getContextType() { return contextType; }
    public Long getContextId() { return contextId; }
}
