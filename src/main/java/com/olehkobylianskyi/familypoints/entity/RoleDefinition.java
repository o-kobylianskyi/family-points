package com.olehkobylianskyi.familypoints.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "role_definitions")
public class RoleDefinition {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_set_id")
    private RoleSet roleSet;

    @Column(name = "system_code", length = 80)
    private String systemCode;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoleVisibility visibility = RoleVisibility.PRIVATE;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_context_type", nullable = false, length = 30)
    private RoleContextType ownerContextType;

    @Column(name = "owner_context_id")
    private Long ownerContextId;

    @Column(name = "system_default", nullable = false)
    private boolean systemDefault;

    @Column(nullable = false)
    private boolean active = true;

    protected RoleDefinition() {}

    public RoleDefinition(Workspace workspace, RoleSet roleSet, String name, String description,
                          RoleVisibility visibility, RoleContextType ownerContextType, Long ownerContextId) {
        this.workspace = workspace;
        this.roleSet = roleSet;
        this.name = name;
        this.description = description;
        this.visibility = visibility;
        this.ownerContextType = ownerContextType;
        this.ownerContextId = ownerContextId;
    }

    public Long getId() { return id; }
    public Workspace getWorkspace() { return workspace; }
    public RoleSet getRoleSet() { return roleSet; }
    public String getSystemCode() { return systemCode; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public RoleVisibility getVisibility() { return visibility; }
    public RoleContextType getOwnerContextType() { return ownerContextType; }
    public Long getOwnerContextId() { return ownerContextId; }
    public boolean isSystemDefault() { return systemDefault; }
    public boolean isActive() { return active; }
    public void setRoleSet(RoleSet roleSet) { this.roleSet = roleSet; }
    public void setSystemCode(String systemCode) { this.systemCode = systemCode; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setVisibility(RoleVisibility visibility) { this.visibility = visibility; }
    public void setSystemDefault(boolean systemDefault) { this.systemDefault = systemDefault; }
    public void setActive(boolean active) { this.active = active; }
}
