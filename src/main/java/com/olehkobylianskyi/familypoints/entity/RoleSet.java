package com.olehkobylianskyi.familypoints.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "role_sets", uniqueConstraints = @UniqueConstraint(name = "uk_role_set_name", columnNames = {"family_id", "name"}))
public class RoleSet {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_id", nullable = false)
    private Workspace workspace;

    @Column(name = "system_code", length = 80)
    private String systemCode;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoleVisibility visibility = RoleVisibility.SHARED;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_context_type", nullable = false, length = 30)
    private RoleContextType ownerContextType = RoleContextType.WORKSPACE;

    @Column(name = "owner_context_id")
    private Long ownerContextId;

    @Column(name = "system_default", nullable = false)
    private boolean systemDefault;

    @Column(nullable = false)
    private boolean active = true;

    protected RoleSet() {}

    public RoleSet(Workspace workspace, String name, String description) {
        this.workspace = workspace;
        this.name = name;
        this.description = description;
    }

    public Long getId() { return id; }
    public Workspace getWorkspace() { return workspace; }
    public String getSystemCode() { return systemCode; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public RoleVisibility getVisibility() { return visibility; }
    public RoleContextType getOwnerContextType() { return ownerContextType; }
    public Long getOwnerContextId() { return ownerContextId; }
    public boolean isSystemDefault() { return systemDefault; }
    public boolean isActive() { return active; }
    public void setSystemCode(String systemCode) { this.systemCode = systemCode; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setVisibility(RoleVisibility visibility) { this.visibility = visibility; }
    public void setOwnerContext(RoleContextType type, Long id) { this.ownerContextType = type; this.ownerContextId = id; }
    public void setSystemDefault(boolean systemDefault) { this.systemDefault = systemDefault; }
    public void setActive(boolean active) { this.active = active; }
}
