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
    private final String accountEmail;
    private final String invitationEmail;
    private final String accessStatus;

    public WorkspaceMemberResponse(
            Long id,
            String name,
            WorkspaceMemberType memberType,
            Long workspaceRoleId,
            String workspaceRoleCode,
            String workspaceRoleName,
            Long workspaceId
    ) {
        this(id, name, memberType, workspaceRoleId, workspaceRoleCode, workspaceRoleName, workspaceId, null, null, null);
    }

    public WorkspaceMemberResponse(
            Long id, String name, WorkspaceMemberType memberType, Long workspaceRoleId,
            String workspaceRoleCode, String workspaceRoleName, Long workspaceId, String accountEmail
    ) {
        this(id, name, memberType, workspaceRoleId, workspaceRoleCode, workspaceRoleName, workspaceId,
                accountEmail, null, null);
    }

    public WorkspaceMemberResponse(
            Long id, String name, WorkspaceMemberType memberType, Long workspaceRoleId,
            String workspaceRoleCode, String workspaceRoleName, Long workspaceId,
            String accountEmail, String invitationEmail, String accessStatus
    ) {
        this.id = id;
        this.name = name;
        this.memberType = memberType;
        this.workspaceRoleId = workspaceRoleId;
        this.workspaceRoleCode = workspaceRoleCode;
        this.workspaceRoleName = workspaceRoleName;
        this.workspaceId = workspaceId;
        this.accountEmail = accountEmail;
        this.invitationEmail = invitationEmail;
        this.accessStatus = accessStatus;
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

    public static WorkspaceMemberResponse from(WorkspaceMember member, String accountEmail) {
        return new WorkspaceMemberResponse(
                member.getId(), member.getName(), member.getMemberType(),
                member.getWorkspaceRole().getId(), member.getWorkspaceRole().getCode(),
                member.getWorkspaceRole().getName(), member.getWorkspace().getId(), accountEmail);
    }

    public static WorkspaceMemberResponse from(
            WorkspaceMember member, String accountEmail, String invitationEmail, String accessStatus) {
        return new WorkspaceMemberResponse(
                member.getId(), member.getName(), member.getMemberType(),
                member.getWorkspaceRole().getId(), member.getWorkspaceRole().getCode(),
                member.getWorkspaceRole().getName(), member.getWorkspace().getId(),
                accountEmail, invitationEmail, accessStatus);
    }

    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    public String getInvitationEmail() { return invitationEmail; }

    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    public String getAccessStatus() { return accessStatus; }

    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    public String getAccountEmail() {
        return accountEmail;
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