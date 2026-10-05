package com.olehkobylianskyi.familypoints.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reward_purchases")
public class RewardPurchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reward_definition_id")
    private RewardDefinition rewardDefinition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reward_request_id")
    private RewardRequest rewardRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private WorkspaceMember member;

    @Column(name = "reward_title", nullable = false, length = 150)
    private String rewardTitle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "point_type_id", nullable = false)
    private PointType pointType;

    @Column(name = "price_amount", nullable = false)
    private int priceAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RewardPurchaseStatus status;

    @Column(name = "purchased_at", nullable = false, updatable = false)
    private LocalDateTime purchasedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    protected RewardPurchase() {}

    public RewardPurchase(
            RewardDefinition rewardDefinition,
            WorkspaceMember member,
            RewardPurchaseStatus status
    ) {
        this.rewardDefinition = rewardDefinition;
        this.member = member;
        this.rewardTitle = rewardDefinition.getTitle();
        this.pointType = rewardDefinition.getPointType();
        this.priceAmount = rewardDefinition.getPriceAmount();
        this.status = status;
        this.purchasedAt = LocalDateTime.now();
    }

    public RewardPurchase(
            RewardRequest rewardRequest,
            WorkspaceMember member,
            RewardPurchaseStatus status
    ) {
        this.rewardRequest = rewardRequest;
        this.rewardDefinition = rewardRequest.getRewardDefinition();
        this.member = member;
        this.rewardTitle = rewardRequest.getTitle();
        this.pointType = rewardRequest.getApprovedPointType();
        this.priceAmount = rewardRequest.getApprovedPriceAmount() == null
                ? 0
                : rewardRequest.getApprovedPriceAmount();
        this.status = status;
        this.purchasedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public RewardDefinition getRewardDefinition() { return rewardDefinition; }
    public RewardRequest getRewardRequest() { return rewardRequest; }
    public WorkspaceMember getMember() { return member; }
    public String getRewardTitle() { return rewardTitle; }
    public PointType getPointType() { return pointType; }
    public int getPriceAmount() { return priceAmount; }
    public RewardPurchaseStatus getStatus() { return status; }
    public LocalDateTime getPurchasedAt() { return purchasedAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }

    public void approve() {
        this.status = RewardPurchaseStatus.READY;
        this.resolvedAt = LocalDateTime.now();
    }

    public void redeem() {
        this.status = RewardPurchaseStatus.REDEEMED;
        this.resolvedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = RewardPurchaseStatus.CANCELLED;
        this.resolvedAt = LocalDateTime.now();
    }
}
