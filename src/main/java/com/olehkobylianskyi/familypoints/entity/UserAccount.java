package com.olehkobylianskyi.familypoints.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "user_accounts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_accounts_username",
                        columnNames = "username"
                )
        }
)
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false)
    private boolean enabled = true;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "family_member_id",
            nullable = false,
            unique = true
    )
    private WorkspaceMember workspaceMember;

    protected UserAccount() {
    }

    public UserAccount(
            String username,
            String passwordHash,
            WorkspaceMember workspaceMember
    ) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.workspaceMember = workspaceMember;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public WorkspaceMember getWorkspaceMember() {
        return workspaceMember;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setWorkspaceMember(WorkspaceMember workspaceMember) {
        this.workspaceMember = workspaceMember;
    }
}