package com.olehkobylianskyi.familypoints.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reward_requests")
public class RewardRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_member_id", nullable = false)
    private WorkspaceMember requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reward_definition_id")
    private RewardDefinition rewardDefinition;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private RewardRequestStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_point_type_id")
    private PointType approvedPointType;

    @Column(name = "approved_price_amount")
    private Integer approvedPriceAmount;

    @Column(name = "minimum_reputation")
    private Integer minimumReputation;

    @Column(name = "approved_duration_minutes")
    private Integer approvedDurationMinutes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    protected RewardRequest() {}

    public RewardRequest(
            Workspace workspace,
            WorkspaceMember requestedBy,
            RewardDefinition rewardDefinition,
            String title,
            String description
    ) {
        this.workspace = workspace;
        this.requestedBy = requestedBy;
        this.rewardDefinition = rewardDefinition;
        this.title = title;
        this.description = description;
        this.status = RewardRequestStatus.REQUESTED;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Workspace getWorkspace() { return workspace; }
    public WorkspaceMember getRequestedBy() { return requestedBy; }
    public RewardDefinition getRewardDefinition() { return rewardDefinition; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public RewardRequestStatus getStatus() { return status; }
    public PointType getApprovedPointType() { return approvedPointType; }
    public Integer getApprovedPriceAmount() { return approvedPriceAmount; }
    public Integer getMinimumReputation() { return minimumReputation; }
    public Integer getApprovedDurationMinutes() { return approvedDurationMinutes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }

    public void approve(PointType pointType, Integer priceAmount, Integer minimumReputation, Integer durationMinutes, boolean hasBeforeRequirements) {
        this.approvedPointType = pointType;
        this.approvedPriceAmount = priceAmount;
        this.minimumReputation = minimumReputation;
        this.approvedDurationMinutes = durationMinutes;
        this.status = hasBeforeRequirements
                ? RewardRequestStatus.WAITING_REQUIREMENTS
                : RewardRequestStatus.READY_TO_PURCHASE;
        this.reviewedAt = LocalDateTime.now();
    }

    public void markReady() { this.status = RewardRequestStatus.READY_TO_PURCHASE; }
    public void markPurchased() {
        this.status = RewardRequestStatus.PURCHASED;
        this.completedAt = LocalDateTime.now();
    }
    public void reject() {
        this.status = RewardRequestStatus.REJECTED;
        this.reviewedAt = LocalDateTime.now();
    }
    public void cancel() {
        this.status = RewardRequestStatus.CANCELLED;
        this.completedAt = LocalDateTime.now();
    }
}
