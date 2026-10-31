package com.olehkobylianskyi.familypoints.dto;

import com.olehkobylianskyi.familypoints.entity.WorkspaceMemberType;

public class WorkspaceMemberUpdateRequest {

    private String name;

    private WorkspaceMemberType memberType;

    private Long workspaceRoleId;

    public WorkspaceMemberUpdateRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public WorkspaceMemberType getMemberType() {
        return memberType;
    }

    public void setMemberType(WorkspaceMemberType memberType) {
        this.memberType = memberType;
    }

    public Long getWorkspaceRoleId() {
        return workspaceRoleId;
    }

    public void setWorkspaceRoleId(Long workspaceRoleId) {
        this.workspaceRoleId = workspaceRoleId;
    }
}