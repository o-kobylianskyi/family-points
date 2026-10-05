package com.olehkobylianskyi.familypoints.dto;

import com.olehkobylianskyi.familypoints.entity.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class TaskInstanceResponse {

    private final Long id;
    private final Long taskDefinitionId;
    private final Long memberId;
    private final Long originalAssignedMemberId;
    private final java.time.LocalDateTime claimedAt;
    private final boolean delegationAllowed;

    private final String title;
    private final String description;
    private final boolean mandatory;
    private final LocalTime dueTime;

    private final Long rewardPointTypeId;
    private final String rewardPointTypeCode;
    private final Integer rewardAmount;

    private final Long penaltyPointTypeId;
    private final String penaltyPointTypeCode;
    private final Integer penaltyAmount;
    private final Integer rewardReputationAmount;
    private final Integer penaltyReputationAmount;

    private final LocalDate scheduledDate;
    private final TaskInstanceStatus status;
    private final LocalDateTime startedAt;
    private final LocalDateTime completedAt;

    private final TaskExcuseReason excuseReason;
    private final String excuseComment;

    private final boolean rewardProcessed;
    private final boolean penaltyProcessed;

    public TaskInstanceResponse(
            Long id,
            Long taskDefinitionId,
            Long memberId,
            Long originalAssignedMemberId,
            java.time.LocalDateTime claimedAt,
            boolean delegationAllowed,
            String title,
            String description,
            boolean mandatory,
            LocalTime dueTime,
            Long rewardPointTypeId,
            String rewardPointTypeCode,
            Integer rewardAmount,
            Long penaltyPointTypeId,
            String penaltyPointTypeCode,
            Integer penaltyAmount,
            Integer rewardReputationAmount,
            Integer penaltyReputationAmount,
            LocalDate scheduledDate,
            TaskInstanceStatus status,
            LocalDateTime startedAt,
            LocalDateTime completedAt,
            TaskExcuseReason excuseReason,
            String excuseComment,
            boolean rewardProcessed,
            boolean penaltyProcessed
    ) {
        this.id = id;
        this.taskDefinitionId = taskDefinitionId;
        this.memberId = memberId;
        this.originalAssignedMemberId = originalAssignedMemberId;
        this.claimedAt = claimedAt;
        this.delegationAllowed = delegationAllowed;
        this.title = title;
        this.description = description;
        this.mandatory = mandatory;
        this.dueTime = dueTime;

        this.rewardPointTypeId = rewardPointTypeId;
        this.rewardPointTypeCode = rewardPointTypeCode;
        this.rewardAmount = rewardAmount;

        this.penaltyPointTypeId = penaltyPointTypeId;
        this.penaltyPointTypeCode = penaltyPointTypeCode;
        this.penaltyAmount = penaltyAmount;
        this.rewardReputationAmount = rewardReputationAmount;
        this.penaltyReputationAmount = penaltyReputationAmount;

        this.scheduledDate = scheduledDate;
        this.status = status;
        this.startedAt = startedAt;
        this.completedAt = completedAt;

        this.excuseReason = excuseReason;
        this.excuseComment = excuseComment;

        this.rewardProcessed = rewardProcessed;
        this.penaltyProcessed = penaltyProcessed;
    }

    public static TaskInstanceResponse from(
            TaskInstance instance
    ) {
        PointType rewardPointType =
                instance.getRewardPointType();

        PointType penaltyPointType =
                instance.getPenaltyPointType();

        return new TaskInstanceResponse(
                instance.getId(),
                instance.getTaskDefinition().getId(),
                instance.getMember().getId(),
                instance.getOriginalAssignedMember() == null ? null : instance.getOriginalAssignedMember().getId(),
                instance.getClaimedAt(),
                instance.getTaskDefinition().isDelegationAllowed(),

                instance.getTitle(),
                instance.getDescription(),
                instance.isMandatory(),
                instance.getDueTime(),

                rewardPointType != null
                        ? rewardPointType.getId()
                        : null,

                rewardPointType != null
                        ? rewardPointType.getCode()
                        : null,

                instance.getRewardAmount(),

                penaltyPointType != null
                        ? penaltyPointType.getId()
                        : null,

                penaltyPointType != null
                        ? penaltyPointType.getCode()
                        : null,

                instance.getPenaltyAmount(),
                instance.getRewardReputationAmount(),
                instance.getPenaltyReputationAmount(),

                instance.getScheduledDate(),
                instance.getStatus(),
                instance.getStartedAt(),
                instance.getCompletedAt(),

                instance.getExcuseReason(),
                instance.getExcuseComment(),

                instance.isRewardProcessed(),
                instance.isPenaltyProcessed()
        );
    }

    public Long getId() {
        return id;
    }

    public Long getTaskDefinitionId() {
        return taskDefinitionId;
    }

    public Long getMemberId() {
        return memberId;
    }

    public Long getOriginalAssignedMemberId() { return originalAssignedMemberId; }
    public java.time.LocalDateTime getClaimedAt() { return claimedAt; }
    public boolean isDelegationAllowed() { return delegationAllowed; }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public LocalTime getDueTime() {
        return dueTime;
    }

    public Long getRewardPointTypeId() {
        return rewardPointTypeId;
    }

    public String getRewardPointTypeCode() {
        return rewardPointTypeCode;
    }

    public Integer getRewardAmount() {
        return rewardAmount;
    }

    public Long getPenaltyPointTypeId() {
        return penaltyPointTypeId;
    }

    public String getPenaltyPointTypeCode() {
        return penaltyPointTypeCode;
    }

    public Integer getPenaltyAmount() {
        return penaltyAmount;
    }

    public Integer getRewardReputationAmount() { return rewardReputationAmount; }
    public Integer getPenaltyReputationAmount() { return penaltyReputationAmount; }

    public LocalDate getScheduledDate() {
        return scheduledDate;
    }

    public TaskInstanceStatus getStatus() {
        return status;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public TaskExcuseReason getExcuseReason() {
        return excuseReason;
    }

    public String getExcuseComment() {
        return excuseComment;
    }

    public boolean isRewardProcessed() {
        return rewardProcessed;
    }

    public boolean isPenaltyProcessed() {
        return penaltyProcessed;
    }
}