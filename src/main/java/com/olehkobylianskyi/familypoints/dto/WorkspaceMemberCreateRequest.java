package com.olehkobylianskyi.familypoints.dto;

import com.olehkobylianskyi.familypoints.entity.WorkspaceMemberType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class WorkspaceMemberCreateRequest {

    @NotBlank
    private String name;

    @NotNull
    private WorkspaceMemberType memberType;

    @NotNull
    private Long workspaceRoleId;

    public WorkspaceMemberCreateRequest() {
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