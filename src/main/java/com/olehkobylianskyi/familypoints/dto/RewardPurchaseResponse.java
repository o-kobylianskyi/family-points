package com.olehkobylianskyi.familypoints.dto;

import com.olehkobylianskyi.familypoints.entity.RewardPurchase;
import com.olehkobylianskyi.familypoints.entity.RewardPurchaseStatus;
import java.time.LocalDateTime;

public class RewardPurchaseResponse {

    private final Long id;
    private final Long rewardDefinitionId;
    private final Long memberId;
    private final String memberName;
    private final String rewardTitle;
    private final Long pointTypeId;
    private final String pointTypeCode;
    private final int priceAmount;
    private final RewardPurchaseStatus status;
    private final LocalDateTime purchasedAt;
    private final LocalDateTime resolvedAt;

    private RewardPurchaseResponse(RewardPurchase purchase) {
        this.id = purchase.getId();
        this.rewardDefinitionId = purchase.getRewardDefinition() == null
                ? null
                : purchase.getRewardDefinition().getId();
        this.memberId = purchase.getMember().getId();
        this.memberName = purchase.getMember().getName();
        this.rewardTitle = purchase.getRewardTitle();
        this.pointTypeId = purchase.getPointType().getId();
        this.pointTypeCode = purchase.getPointType().getCode();
        this.priceAmount = purchase.getPriceAmount();
        this.status = purchase.getStatus();
        this.purchasedAt = purchase.getPurchasedAt();
        this.resolvedAt = purchase.getResolvedAt();
    }

    public static RewardPurchaseResponse from(RewardPurchase purchase) {
        return new RewardPurchaseResponse(purchase);
    }

    public Long getId() { return id; }
    public Long getRewardDefinitionId() { return rewardDefinitionId; }
    public Long getMemberId() { return memberId; }
    public String getMemberName() { return memberName; }
    public String getRewardTitle() { return rewardTitle; }
    public Long getPointTypeId() { return pointTypeId; }
    public String getPointTypeCode() { return pointTypeCode; }
    public int getPriceAmount() { return priceAmount; }
    public RewardPurchaseStatus getStatus() { return status; }
    public LocalDateTime getPurchasedAt() { return purchasedAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
}
