package com.olehkobylianskyi.familypoints.dto;

import com.olehkobylianskyi.familypoints.entity.TaskRewardRequest;
import java.time.LocalDateTime;

public class TaskRewardRequestResponse {

    private final Long id;
    private final Long taskInstanceId;
    private final String taskTitle;
    private final Long requestedByMemberId;
    private final String requestedByMemberName;
    private final String status;

    private final Long requestedPointTypeId;
    private final String requestedPointTypeCode;
    private final Integer requestedPointAmount;
    private final Integer requestedReputationAmount;
    private final Long requestedRewardDefinitionId;
    private final String requestedRewardTitle;
    private final String requestedCustomRewardTitle;
    private final String requestedComment;

    private final Long approvedPointTypeId;
    private final String approvedPointTypeCode;
    private final Integer approvedPointAmount;
    private final Integer approvedReputationAmount;
    private final Long approvedRewardDefinitionId;
    private final String approvedRewardTitle;
    private final String approvedCustomRewardTitle;
    private final String reviewerComment;

    private final LocalDateTime createdAt;
    private final LocalDateTime reviewedAt;
    private final LocalDateTime fulfilledAt;

    public TaskRewardRequestResponse(TaskRewardRequest request) {
        this.id = request.getId();
        this.taskInstanceId = request.getTaskInstance().getId();
        this.taskTitle = request.getTaskInstance().getTitle();
        this.requestedByMemberId = request.getRequestedBy().getId();
        this.requestedByMemberName = request.getRequestedBy().getName();
        this.status = request.getStatus().name();

        this.requestedPointTypeId = request.getRequestedPointType() == null ? null : request.getRequestedPointType().getId();
        this.requestedPointTypeCode = request.getRequestedPointType() == null ? null : request.getRequestedPointType().getCode();
        this.requestedPointAmount = request.getRequestedPointAmount();
        this.requestedReputationAmount = request.getRequestedReputationAmount();
        this.requestedRewardDefinitionId = request.getRequestedRewardDefinition() == null ? null : request.getRequestedRewardDefinition().getId();
        this.requestedRewardTitle = request.getRequestedRewardDefinition() == null ? null : request.getRequestedRewardDefinition().getTitle();
        this.requestedCustomRewardTitle = request.getRequestedCustomRewardTitle();
        this.requestedComment = request.getRequestedComment();

        this.approvedPointTypeId = request.getApprovedPointType() == null ? null : request.getApprovedPointType().getId();
        this.approvedPointTypeCode = request.getApprovedPointType() == null ? null : request.getApprovedPointType().getCode();
        this.approvedPointAmount = request.getApprovedPointAmount();
        this.approvedReputationAmount = request.getApprovedReputationAmount();
        this.approvedRewardDefinitionId = request.getApprovedRewardDefinition() == null ? null : request.getApprovedRewardDefinition().getId();
        this.approvedRewardTitle = request.getApprovedRewardDefinition() == null ? null : request.getApprovedRewardDefinition().getTitle();
        this.approvedCustomRewardTitle = request.getApprovedCustomRewardTitle();
        this.reviewerComment = request.getReviewerComment();

        this.createdAt = request.getCreatedAt();
        this.reviewedAt = request.getReviewedAt();
        this.fulfilledAt = request.getFulfilledAt();
    }

    public Long getId() { return id; }
    public Long getTaskInstanceId() { return taskInstanceId; }
    public String getTaskTitle() { return taskTitle; }
    public Long getRequestedByMemberId() { return requestedByMemberId; }
    public String getRequestedByMemberName() { return requestedByMemberName; }
    public String getStatus() { return status; }
    public Long getRequestedPointTypeId() { return requestedPointTypeId; }
    public String getRequestedPointTypeCode() { return requestedPointTypeCode; }
    public Integer getRequestedPointAmount() { return requestedPointAmount; }
    public Integer getRequestedReputationAmount() { return requestedReputationAmount; }
    public Long getRequestedRewardDefinitionId() { return requestedRewardDefinitionId; }
    public String getRequestedRewardTitle() { return requestedRewardTitle; }
    public String getRequestedCustomRewardTitle() { return requestedCustomRewardTitle; }
    public String getRequestedComment() { return requestedComment; }
    public Long getApprovedPointTypeId() { return approvedPointTypeId; }
    public String getApprovedPointTypeCode() { return approvedPointTypeCode; }
    public Integer getApprovedPointAmount() { return approvedPointAmount; }
    public Integer getApprovedReputationAmount() { return approvedReputationAmount; }
    public Long getApprovedRewardDefinitionId() { return approvedRewardDefinitionId; }
    public String getApprovedRewardTitle() { return approvedRewardTitle; }
    public String getApprovedCustomRewardTitle() { return approvedCustomRewardTitle; }
    public String getReviewerComment() { return reviewerComment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public LocalDateTime getFulfilledAt() { return fulfilledAt; }
}
