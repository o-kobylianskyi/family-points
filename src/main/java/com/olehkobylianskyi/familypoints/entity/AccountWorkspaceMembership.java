package com.olehkobylianskyi.familypoints.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "account_workspace_memberships")
public class AccountWorkspaceMembership {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private UserAccount account;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_member_id", nullable = false, unique = true)
    private WorkspaceMember workspaceMember;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected AccountWorkspaceMembership() {}

    public AccountWorkspaceMembership(UserAccount account, WorkspaceMember workspaceMember) {
        this.account = account;
        this.workspaceMember = workspaceMember;
        this.workspace = workspaceMember.getWorkspace();
    }

    public Long getId() { return id; }
    public UserAccount getAccount() { return account; }
    public WorkspaceMember getWorkspaceMember() { return workspaceMember; }
    public Workspace getWorkspace() { return workspace; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
