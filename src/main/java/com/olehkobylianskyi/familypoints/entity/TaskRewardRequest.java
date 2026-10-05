package com.olehkobylianskyi.familypoints.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "task_reward_requests")
public class TaskRewardRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_instance_id", nullable = false)
    private TaskInstance taskInstance;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_member_id", nullable = false)
    private WorkspaceMember requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TaskRewardRequestStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_point_type_id")
    private PointType requestedPointType;

    @Column(name = "requested_point_amount")
    private Integer requestedPointAmount;

    @Column(name = "requested_reputation_amount")
    private Integer requestedReputationAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_reward_definition_id")
    private RewardDefinition requestedRewardDefinition;

    @Column(name = "requested_custom_reward_title", length = 150)
    private String requestedCustomRewardTitle;

    @Column(name = "requested_comment", length = 500)
    private String requestedComment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_point_type_id")
    private PointType approvedPointType;

    @Column(name = "approved_point_amount")
    private Integer approvedPointAmount;

    @Column(name = "approved_reputation_amount")
    private Integer approvedReputationAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_reward_definition_id")
    private RewardDefinition approvedRewardDefinition;

    @Column(name = "approved_custom_reward_title", length = 150)
    private String approvedCustomRewardTitle;

    @Column(name = "reviewer_comment", length = 500)
    private String reviewerComment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "fulfilled_at")
    private LocalDateTime fulfilledAt;

    protected TaskRewardRequest() {}

    public TaskRewardRequest(
            Workspace workspace,
            TaskInstance taskInstance,
            WorkspaceMember requestedBy
    ) {
        this.workspace = workspace;
        this.taskInstance = taskInstance;
        this.requestedBy = requestedBy;
        this.status = TaskRewardRequestStatus.REQUESTED;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Workspace getWorkspace() { return workspace; }
    public TaskInstance getTaskInstance() { return taskInstance; }
    public WorkspaceMember getRequestedBy() { return requestedBy; }
    public TaskRewardRequestStatus getStatus() { return status; }
    public PointType getRequestedPointType() { return requestedPointType; }
    public Integer getRequestedPointAmount() { return requestedPointAmount; }
    public Integer getRequestedReputationAmount() { return requestedReputationAmount; }
    public RewardDefinition getRequestedRewardDefinition() { return requestedRewardDefinition; }
    public String getRequestedCustomRewardTitle() { return requestedCustomRewardTitle; }
    public String getRequestedComment() { return requestedComment; }
    public PointType getApprovedPointType() { return approvedPointType; }
    public Integer getApprovedPointAmount() { return approvedPointAmount; }
    public Integer getApprovedReputationAmount() { return approvedReputationAmount; }
    public RewardDefinition getApprovedRewardDefinition() { return approvedRewardDefinition; }
    public String getApprovedCustomRewardTitle() { return approvedCustomRewardTitle; }
    public String getReviewerComment() { return reviewerComment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public LocalDateTime getFulfilledAt() { return fulfilledAt; }

    public void setRequestedTerms(
            PointType pointType,
            Integer pointAmount,
            Integer reputationAmount,
            RewardDefinition rewardDefinition,
            String customRewardTitle,
            String comment
    ) {
        this.requestedPointType = pointType;
        this.requestedPointAmount = pointAmount;
        this.requestedReputationAmount = reputationAmount;
        this.requestedRewardDefinition = rewardDefinition;
        this.requestedCustomRewardTitle = customRewardTitle;
        this.requestedComment = comment;
    }

    public void approve(
            PointType pointType,
            Integer pointAmount,
            Integer reputationAmount,
            RewardDefinition rewardDefinition,
            String customRewardTitle,
            String reviewerComment
    ) {
        this.approvedPointType = pointType;
        this.approvedPointAmount = pointAmount;
        this.approvedReputationAmount = reputationAmount;
        this.approvedRewardDefinition = rewardDefinition;
        this.approvedCustomRewardTitle = customRewardTitle;
        this.reviewerComment = reviewerComment;
        this.status = TaskRewardRequestStatus.APPROVED;
        this.reviewedAt = LocalDateTime.now();
    }

    public void reject(String reviewerComment) {
        this.reviewerComment = reviewerComment;
        this.status = TaskRewardRequestStatus.REJECTED;
        this.reviewedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = TaskRewardRequestStatus.CANCELLED;
    }

    public void fulfill() {
        this.status = TaskRewardRequestStatus.FULFILLED;
        this.fulfilledAt = LocalDateTime.now();
    }
}
