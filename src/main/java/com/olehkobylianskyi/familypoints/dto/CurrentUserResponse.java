package com.olehkobylianskyi.familypoints.dto;

import com.olehkobylianskyi.familypoints.entity.WorkspacePermission;
import com.olehkobylianskyi.familypoints.entity.UserAccount;

import java.util.List;

public class CurrentUserResponse {

    private final String username;

    private final Long memberId;
    private final String memberName;
    private final String memberType;

    private final Long workspaceId;

    private final Long workspaceRoleId;
    private final String workspaceRoleCode;
    private final String workspaceRoleName;

    private final List<String> permissions;

    public CurrentUserResponse(
            String username,
            Long memberId,
            String memberName,
            String memberType,
            Long workspaceId,
            Long workspaceRoleId,
            String workspaceRoleCode,
            String workspaceRoleName,
            List<String> permissions
    ) {
        this.username = username;
        this.memberId = memberId;
        this.memberName = memberName;
        this.memberType = memberType;
        this.workspaceId = workspaceId;
        this.workspaceRoleId = workspaceRoleId;
        this.workspaceRoleCode = workspaceRoleCode;
        this.workspaceRoleName = workspaceRoleName;
        this.permissions = permissions;
    }

    public static CurrentUserResponse from(UserAccount account) {

        var member = account.getWorkspaceMember();
        var role = member.getWorkspaceRole();

        List<String> permissions = role
                .getPermissions()
                .stream()
                .map(WorkspacePermission::name)
                .sorted()
                .toList();

        return new CurrentUserResponse(
                account.getUsername(),
                member.getId(),
                member.getName(),
                member.getMemberType().name(),
                member.getWorkspace().getId(),
                role.getId(),
                role.getCode(),
                role.getName(),
                permissions
        );
    }

    public String getUsername() {
        return username;
    }

    public Long getMemberId() {
        return memberId;
    }

    public String getMemberName() {
        return memberName;
    }

    public String getMemberType() {
        return memberType;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public Long getWorkspaceRoleId() {
        return workspaceRoleId;
    }

    public String getWorkspaceRoleCode() {
        return workspaceRoleCode;
    }

    public String getWorkspaceRoleName() {
        return workspaceRoleName;
    }

    public List<String> getPermissions() {
        return permissions;
    }
}