package com.olehkobylianskyi.familypoints.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "role_permission_grants")
public class RolePermissionGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_definition_id", nullable = false)
    private RoleDefinition roleDefinition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private GroupPermission permission;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RolePermissionScope scope;

    @Column(name = "legacy_group_permission_grant_id")
    private Long legacyGroupPermissionGrantId;

    @Column(nullable = false)
    private boolean active = true;

    protected RolePermissionGrant() {}

    public RolePermissionGrant(RoleDefinition roleDefinition, GroupPermission permission, RolePermissionScope scope) {
        this.roleDefinition = roleDefinition;
        this.permission = permission;
        this.scope = scope;
    }

    public Long getId() { return id; }
    public RoleDefinition getRoleDefinition() { return roleDefinition; }
    public GroupPermission getPermission() { return permission; }
    public RolePermissionScope getScope() { return scope; }
    public Long getLegacyGroupPermissionGrantId() { return legacyGroupPermissionGrantId; }
    public boolean isActive() { return active; }

    public void setLegacyGroupPermissionGrantId(Long legacyGroupPermissionGrantId) { this.legacyGroupPermissionGrantId = legacyGroupPermissionGrantId; }
    public void setActive(boolean active) { this.active = active; }
}
