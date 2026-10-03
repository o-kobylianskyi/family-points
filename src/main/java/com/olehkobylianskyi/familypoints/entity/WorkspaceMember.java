package com.olehkobylianskyi.familypoints.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "family_members")
public class WorkspaceMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_type", nullable = false, length = 30)
    private WorkspaceMemberType memberType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_role_id", nullable = false)
    private WorkspaceRole workspaceRole;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_id", nullable = false)
    private Workspace workspace;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    protected WorkspaceMember() {}

    public WorkspaceMember(String name, WorkspaceMemberType memberType, WorkspaceRole workspaceRole, Workspace workspace) {
        this.name = name;
        this.memberType = memberType;
        this.workspaceRole = workspaceRole;
        this.workspace = workspace;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public WorkspaceMemberType getMemberType() { return memberType; }
    public WorkspaceRole getWorkspaceRole() { return workspaceRole; }
    public Workspace getWorkspace() { return workspace; }
    public boolean isActive() { return active; }
    public LocalDateTime getDeletedAt() { return deletedAt; }

    public void setName(String name) { this.name = name; }
    public void setMemberType(WorkspaceMemberType memberType) { this.memberType = memberType; }
    public void setWorkspaceRole(WorkspaceRole workspaceRole) { this.workspaceRole = workspaceRole; }

    public void softDelete() {
        this.active = false;
        this.deletedAt = LocalDateTime.now();
        this.name = "Видалено";
    }

    public boolean hasPermission(WorkspacePermission permission) {
        return active && workspaceRole.hasPermission(permission);
    }
}
