package com.olehkobylianskyi.familypoints.dto;

import com.olehkobylianskyi.familypoints.entity.RewardDefinition;
import java.time.LocalDateTime;

public class RewardDefinitionResponse {

    private final Long id;
    private final String title;
    private final String description;
    private final Long pointTypeId;
    private final String pointTypeCode;
    private final String pointTypeName;
    private final int priceAmount;
    private final Integer minimumReputation;
    private final boolean requiresApproval;
    private final boolean active;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private RewardDefinitionResponse(RewardDefinition reward) {
        this.id = reward.getId();
        this.title = reward.getTitle();
        this.description = reward.getDescription();
        this.pointTypeId = reward.getPointType().getId();
        this.pointTypeCode = reward.getPointType().getCode();
        this.pointTypeName = reward.getPointType().getName();
        this.priceAmount = reward.getPriceAmount();
        this.minimumReputation = reward.getMinimumReputation();
        this.requiresApproval = reward.isRequiresApproval();
        this.active = reward.isActive();
        this.createdAt = reward.getCreatedAt();
        this.updatedAt = reward.getUpdatedAt();
    }

    public static RewardDefinitionResponse from(RewardDefinition reward) {
        return new RewardDefinitionResponse(reward);
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Long getPointTypeId() { return pointTypeId; }
    public String getPointTypeCode() { return pointTypeCode; }
    public String getPointTypeName() { return pointTypeName; }
    public int getPriceAmount() { return priceAmount; }
    public Integer getMinimumReputation() { return minimumReputation; }
    public boolean isRequiresApproval() { return requiresApproval; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
