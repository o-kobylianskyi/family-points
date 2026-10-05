package com.olehkobylianskyi.familypoints.dto;

import com.olehkobylianskyi.familypoints.entity.*;
import java.time.LocalDateTime;
import java.util.List;

public class RewardObligationResponse {
    private final Long id;
    private final Long memberId;
    private final String memberName;
    private final Long rewardPurchaseId;
    private final String rewardTitle;
    private final String title;
    private final String status;
    private final String blockingMode;
    private final LocalDateTime dueAt;
    private final LocalDateTime createdAt;
    private final List<Long> blockedCategoryIds;
    private final List<Long> blockedRewardDefinitionIds;

    public RewardObligationResponse(RewardObligation obligation) {
        this.id = obligation.getId();
        this.memberId = obligation.getMember().getId();
        this.memberName = obligation.getMember().getName();
        this.rewardPurchaseId = obligation.getRewardPurchase().getId();
        this.rewardTitle = obligation.getRewardPurchase().getRewardTitle();
        this.title = obligation.getTitle();
        this.status = obligation.getStatus().name();
        this.blockingMode = obligation.getBlockingMode().name();
        this.dueAt = obligation.getDueAt();
        this.createdAt = obligation.getCreatedAt();
        this.blockedCategoryIds = obligation.getBlockedCategories().stream()
                .map(RewardCategory::getId)
                .toList();
        this.blockedRewardDefinitionIds = obligation.getBlockedRewards().stream()
                .map(RewardDefinition::getId)
                .toList();
    }

    public Long getId() { return id; }
    public Long getMemberId() { return memberId; }
    public String getMemberName() { return memberName; }
    public Long getRewardPurchaseId() { return rewardPurchaseId; }
    public String getRewardTitle() { return rewardTitle; }
    public String getTitle() { return title; }
    public String getStatus() { return status; }
    public String getBlockingMode() { return blockingMode; }
    public LocalDateTime getDueAt() { return dueAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<Long> getBlockedCategoryIds() { return blockedCategoryIds; }
    public List<Long> getBlockedRewardDefinitionIds() { return blockedRewardDefinitionIds; }
}
