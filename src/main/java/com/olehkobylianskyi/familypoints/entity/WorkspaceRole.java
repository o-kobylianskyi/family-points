package com.olehkobylianskyi.familypoints.entity;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "family_roles",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_family_role_family_code",
                        columnNames = {"family_id", "code"}
                )
        }
)
public class WorkspaceRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_id", nullable = false)
    private Workspace workspace;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "family_role_permissions",
            joinColumns = @JoinColumn(name = "role_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "permission", nullable = false, length = 60)
    private Set<WorkspacePermission> permissions = new HashSet<>();

    @Column(nullable = false)
    private boolean systemRole;

    protected WorkspaceRole() {
    }

    public WorkspaceRole(
            Workspace workspace,
            String code,
            String name,
            boolean systemRole
    ) {
        this.workspace = workspace;
        this.code = code;
        this.name = name;
        this.systemRole = systemRole;
    }

    public Long getId() {
        return id;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public Set<WorkspacePermission> getPermissions() {
        return permissions;
    }

    public boolean isSystemRole() {
        return systemRole;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPermissions(Set<WorkspacePermission> permissions) {
        this.permissions.clear();

        if (permissions != null) {
            this.permissions.addAll(permissions);
        }
    }

    public void addPermission(WorkspacePermission permission) {
        permissions.add(permission);
    }

    public void removePermission(WorkspacePermission permission) {
        permissions.remove(permission);
    }

    public boolean hasPermission(WorkspacePermission permission) {
        return permissions.contains(permission);
    }
}