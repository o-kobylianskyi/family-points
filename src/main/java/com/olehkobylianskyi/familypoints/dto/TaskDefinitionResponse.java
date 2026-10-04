package com.olehkobylianskyi.familypoints.dto;

import com.olehkobylianskyi.familypoints.entity.*;
import java.time.*;
import java.util.List;

public class TaskDefinitionResponse {
    private final Long id, assignedMemberId, targetGroupId, preferredMemberId, responsibleMemberId, createdByMemberId, parentTaskDefinitionId;
    private final String title, description, createdByMemberName;
    private final boolean mandatory, active, delegationAllowed;
    private final TaskRecurrenceType recurrenceType;
    private final AssignmentPolicy assignmentPolicy;
    private final RoleMatchMode roleMatchMode;
    private final List<Long> requiredGroupRoleIds;
    private final LocalDate startDate, endDate;
    private final Integer recurrenceDayOfWeek, recurrenceDayOfMonth;
    private final LocalDateTime createdAt, updatedAt;
    private final LocalTime dueTime;
    private final Long rewardPointTypeId, penaltyPointTypeId;
    private final String rewardPointTypeCode, penaltyPointTypeCode;
    private final Integer rewardAmount, penaltyAmount;

    private TaskDefinitionResponse(TaskDefinition t) {
        id=t.getId(); assignedMemberId=id(t.getAssignedMember()); targetGroupId=id(t.getTargetGroup()); preferredMemberId=id(t.getPreferredMember());
        responsibleMemberId=id(t.getResponsibleMember()); createdByMemberId=id(t.getCreatedBy()); createdByMemberName=t.getCreatedBy()==null?null:t.getCreatedBy().getName();
        parentTaskDefinitionId = t.getWorkNode() != null
                && t.getWorkNode().getParentNode() != null
                && t.getWorkNode().getParentNode().getTaskDefinition() != null
                ? t.getWorkNode().getParentNode().getTaskDefinition().getId()
                : null;
        title=t.getTitle(); description=t.getDescription(); mandatory=t.isMandatory(); active=t.isActive(); delegationAllowed=t.isDelegationAllowed();
        recurrenceType=t.getRecurrenceType(); assignmentPolicy=t.getAssignmentPolicy(); roleMatchMode=t.getRoleMatchMode();
        requiredGroupRoleIds=t.getRequiredGroupRoles().stream().map(GroupRole::getId).toList();
        startDate=t.getStartDate(); endDate=t.getEndDate(); recurrenceDayOfWeek=t.getRecurrenceDayOfWeek(); recurrenceDayOfMonth=t.getRecurrenceDayOfMonth();
        createdAt=t.getCreatedAt(); updatedAt=t.getUpdatedAt(); dueTime=t.getDueTime();
        PointType r=t.getRewardPointType(), p=t.getPenaltyPointType();
        rewardPointTypeId=id(r); rewardPointTypeCode=r==null?null:r.getCode(); rewardAmount=t.getRewardAmount();
        penaltyPointTypeId=id(p); penaltyPointTypeCode=p==null?null:p.getCode(); penaltyAmount=t.getPenaltyAmount();
    }
    private static Long id(Object x){ if(x==null)return null; if(x instanceof WorkspaceMember v)return v.getId(); if(x instanceof MemberGroup v)return v.getId(); if(x instanceof PointType v)return v.getId(); return null; }
    public static TaskDefinitionResponse from(TaskDefinition t){return new TaskDefinitionResponse(t);}
    public Long getId(){return id;} public Long getParentTaskDefinitionId(){return parentTaskDefinitionId;} public Long getAssignedMemberId(){return assignedMemberId;} public Long getTargetGroupId(){return targetGroupId;} public Long getPreferredMemberId(){return preferredMemberId;} public Long getResponsibleMemberId(){return responsibleMemberId;} public Long getCreatedByMemberId(){return createdByMemberId;}
    public String getTitle(){return title;} public String getCreatedByMemberName(){return createdByMemberName;} public String getDescription(){return description;} public boolean isMandatory(){return mandatory;} public boolean isActive(){return active;} public boolean isDelegationAllowed(){return delegationAllowed;}
    public TaskRecurrenceType getRecurrenceType(){return recurrenceType;} public AssignmentPolicy getAssignmentPolicy(){return assignmentPolicy;} public RoleMatchMode getRoleMatchMode(){return roleMatchMode;} public List<Long> getRequiredGroupRoleIds(){return requiredGroupRoleIds;}
    public LocalDate getStartDate(){return startDate;} public LocalDate getEndDate(){return endDate;} public Integer getRecurrenceDayOfWeek(){return recurrenceDayOfWeek;} public Integer getRecurrenceDayOfMonth(){return recurrenceDayOfMonth;} public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getUpdatedAt(){return updatedAt;} public LocalTime getDueTime(){return dueTime;}
    public Long getRewardPointTypeId(){return rewardPointTypeId;} public String getRewardPointTypeCode(){return rewardPointTypeCode;} public Integer getRewardAmount(){return rewardAmount;} public Long getPenaltyPointTypeId(){return penaltyPointTypeId;} public String getPenaltyPointTypeCode(){return penaltyPointTypeCode;} public Integer getPenaltyAmount(){return penaltyAmount;}
}
