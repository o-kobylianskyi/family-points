package com.olehkobylianskyi.familypoints.dto;

import com.olehkobylianskyi.familypoints.entity.TaskRecurrenceType;
import com.olehkobylianskyi.familypoints.entity.AssignmentPolicy;
import com.olehkobylianskyi.familypoints.entity.RoleMatchMode;
import java.util.LinkedHashSet;
import java.util.Set;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public class TaskDefinitionCreateRequest {

    @NotNull
    private AssignmentPolicy assignmentPolicy = AssignmentPolicy.SINGLE_MEMBER;

    private Long assignedMemberId;
    private Long targetGroupId;
    private Long preferredMemberId;
    private Long responsibleMemberId;
    private Long parentTaskDefinitionId;
    private boolean delegationAllowed;
    private RoleMatchMode roleMatchMode = RoleMatchMode.ANY;
    private Set<Long> requiredGroupRoleIds = new LinkedHashSet<>();
    private java.util.List<TaskActorRef> administrators = new java.util.ArrayList<>();
    private java.util.List<TaskActorRef> observers = new java.util.ArrayList<>();
    private java.util.List<TaskActorRef> executors = new java.util.ArrayList<>();

    @NotBlank
    @Size(max = 150)
    private String title;

    @Size(max = 1000)
    private String description;

    private boolean mandatory;

    @NotNull
    private TaskRecurrenceType recurrenceType;

    /**
     * Дата, з якої завдання починає діяти.
     *
     * ONCE:
     * завдання буде створене саме на цю дату.
     *
     * DAILY/WEEKLY/MONTHLY:
     * до цієї дати екземпляри завдання не генеруються.
     */
    @NotNull
    private LocalDate startDate;

    /**
     * Остання дата, коли завдання може генеруватися.
     *
     * null = кінцевої дати немає.
     */
    private LocalDate endDate;

    /**
     * День тижня для WEEKLY.
     *
     * 1 = Monday
     * 2 = Tuesday
     * 3 = Wednesday
     * 4 = Thursday
     * 5 = Friday
     * 6 = Saturday
     * 7 = Sunday
     *
     * Для інших recurrenceType має бути null.
     */
    @Min(1)
    @Max(7)
    private Integer recurrenceDayOfWeek;

    /**
     * День місяця для MONTHLY.
     *
     * Допустимі значення: 1..31.
     *
     * Для інших recurrenceType має бути null.
     */
    @Min(1)
    @Max(31)
    private Integer recurrenceDayOfMonth;

    private Long rewardPointTypeId;

    @Min(0)
    private Integer rewardAmount;

    private Long penaltyPointTypeId;

    @Min(0)
    private Integer penaltyAmount;

    /**
     * Час виконання конкретного TaskInstance.
     *
     * Наприклад:
     * startDate = 2026-10-05
     * dueTime   = 18:00
     *
     * означає завдання на 05.10 до 18:00.
     */
    private LocalTime dueTime;


    public TaskDefinitionCreateRequest() {
    }


    public AssignmentPolicy getAssignmentPolicy() { return assignmentPolicy; }
    public void setAssignmentPolicy(AssignmentPolicy assignmentPolicy) { this.assignmentPolicy = assignmentPolicy; }
    public Long getTargetGroupId() { return targetGroupId; }
    public void setTargetGroupId(Long targetGroupId) { this.targetGroupId = targetGroupId; }
    public Long getPreferredMemberId() { return preferredMemberId; }
    public void setPreferredMemberId(Long preferredMemberId) { this.preferredMemberId = preferredMemberId; }
    public Long getResponsibleMemberId() { return responsibleMemberId; }
    public Long getParentTaskDefinitionId() { return parentTaskDefinitionId; }
    public void setParentTaskDefinitionId(Long parentTaskDefinitionId) { this.parentTaskDefinitionId = parentTaskDefinitionId; }
    public void setResponsibleMemberId(Long responsibleMemberId) { this.responsibleMemberId = responsibleMemberId; }
    public boolean isDelegationAllowed() { return delegationAllowed; }
    public void setDelegationAllowed(boolean delegationAllowed) { this.delegationAllowed = delegationAllowed; }
    public RoleMatchMode getRoleMatchMode() { return roleMatchMode; }
    public void setRoleMatchMode(RoleMatchMode roleMatchMode) { this.roleMatchMode = roleMatchMode; }
    public Set<Long> getRequiredGroupRoleIds() { return requiredGroupRoleIds; }
    public void setRequiredGroupRoleIds(Set<Long> ids) { this.requiredGroupRoleIds = ids == null ? new LinkedHashSet<>() : ids; }
    public java.util.List<TaskActorRef> getAdministrators() { return administrators; }
    public void setAdministrators(java.util.List<TaskActorRef> v) { administrators = v == null ? new java.util.ArrayList<>() : v; }
    public java.util.List<TaskActorRef> getObservers() { return observers; }
    public void setObservers(java.util.List<TaskActorRef> v) { observers = v == null ? new java.util.ArrayList<>() : v; }
    public java.util.List<TaskActorRef> getExecutors() { return executors; }
    public void setExecutors(java.util.List<TaskActorRef> v) { executors = v == null ? new java.util.ArrayList<>() : v; }

    public Long getAssignedMemberId() {
        return assignedMemberId;
    }

    public void setAssignedMemberId(Long assignedMemberId) {
        this.assignedMemberId = assignedMemberId;
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }


    public TaskRecurrenceType getRecurrenceType() {
        return recurrenceType;
    }

    public void setRecurrenceType(TaskRecurrenceType recurrenceType) {
        this.recurrenceType = recurrenceType;
    }


    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }


    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }


    public Integer getRecurrenceDayOfWeek() {
        return recurrenceDayOfWeek;
    }

    public void setRecurrenceDayOfWeek(Integer recurrenceDayOfWeek) {
        this.recurrenceDayOfWeek = recurrenceDayOfWeek;
    }


    public Integer getRecurrenceDayOfMonth() {
        return recurrenceDayOfMonth;
    }

    public void setRecurrenceDayOfMonth(Integer recurrenceDayOfMonth) {
        this.recurrenceDayOfMonth = recurrenceDayOfMonth;
    }


    public Long getRewardPointTypeId() {
        return rewardPointTypeId;
    }

    public void setRewardPointTypeId(Long rewardPointTypeId) {
        this.rewardPointTypeId = rewardPointTypeId;
    }


    public Integer getRewardAmount() {
        return rewardAmount;
    }

    public void setRewardAmount(Integer rewardAmount) {
        this.rewardAmount = rewardAmount;
    }


    public Long getPenaltyPointTypeId() {
        return penaltyPointTypeId;
    }

    public void setPenaltyPointTypeId(Long penaltyPointTypeId) {
        this.penaltyPointTypeId = penaltyPointTypeId;
    }


    public Integer getPenaltyAmount() {
        return penaltyAmount;
    }

    public void setPenaltyAmount(Integer penaltyAmount) {
        this.penaltyAmount = penaltyAmount;
    }


    public LocalTime getDueTime() {
        return dueTime;
    }

    public void setDueTime(LocalTime dueTime) {
        this.dueTime = dueTime;
    }
}