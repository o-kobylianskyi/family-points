package com.olehkobylianskyi.familypoints.dto;

import com.olehkobylianskyi.familypoints.entity.WorkspaceMember;
import com.olehkobylianskyi.familypoints.entity.WorkspaceMemberType;

public class WorkspaceMemberResponse {

    private final Long id;
    private final String name;
    private final WorkspaceMemberType memberType;

    private final Long workspaceRoleId;
    private final String workspaceRoleCode;
    private final String workspaceRoleName;

    private final Long workspaceId;

    public WorkspaceMemberResponse(
            Long id,
            String name,
            WorkspaceMemberType memberType,
            Long workspaceRoleId,
            String workspaceRoleCode,
            String workspaceRoleName,
            Long workspaceId
    ) {
        this.id = id;
        this.name = name;
        this.memberType = memberType;
        this.workspaceRoleId = workspaceRoleId;
        this.workspaceRoleCode = workspaceRoleCode;
        this.workspaceRoleName = workspaceRoleName;
        this.workspaceId = workspaceId;
    }

    public static WorkspaceMemberResponse from(WorkspaceMember member) {
        return new WorkspaceMemberResponse(
                member.getId(),
                member.getName(),
                member.getMemberType(),
                member.getWorkspaceRole().getId(),
                member.getWorkspaceRole().getCode(),
                member.getWorkspaceRole().getName(),
                member.getWorkspace().getId()
        );
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public WorkspaceMemberType getMemberType() {
        return memberType;
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

    public Long getWorkspaceId() {
        return workspaceId;
    }
}