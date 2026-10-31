package com.olehkobylianskyi.familypoints.dto;

import com.olehkobylianskyi.familypoints.entity.UserAccount;

public class UserAccountResponse {

    private final Long id;
    private final String username;
    private final boolean enabled;
    private final Long workspaceMemberId;
    private final Long workspaceId;

    public UserAccountResponse(
            Long id,
            String username,
            boolean enabled,
            Long workspaceMemberId,
            Long workspaceId
    ) {
        this.id = id;
        this.username = username;
        this.enabled = enabled;
        this.workspaceMemberId = workspaceMemberId;
        this.workspaceId = workspaceId;
    }

    public static UserAccountResponse from(UserAccount account) {
        return new UserAccountResponse(
                account.getId(),
                account.getUsername(),
                account.isEnabled(),
                account.getWorkspaceMember().getId(),
                account.getWorkspaceMember().getWorkspace().getId()
        );
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Long getWorkspaceMemberId() {
        return workspaceMemberId;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }
}