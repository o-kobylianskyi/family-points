package com.olehkobylianskyi.familypoints.dto;

import com.olehkobylianskyi.familypoints.entity.RewardDefinition;
import java.time.LocalDateTime;
import java.util.List;
import com.olehkobylianskyi.familypoints.entity.RewardCategory;

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
    private final String rewardKind;
    private final Integer defaultDurationMinutes;
    private final String acquisitionMode;
    private final List<RewardCategoryResponse> categories;
    private final List<RewardRequirementResponse> requirements;
    private final boolean active;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private RewardDefinitionResponse(
            RewardDefinition reward,
            List<RewardRequirementResponse> requirements
    ) {
        this.id = reward.getId();
        this.title = reward.getTitle();
        this.description = reward.getDescription();
        this.pointTypeId = reward.getPointType().getId();
        this.pointTypeCode = reward.getPointType().getCode();
        this.pointTypeName = reward.getPointType().getName();
        this.priceAmount = reward.getPriceAmount();
        this.minimumReputation = reward.getMinimumReputation();
        this.requiresApproval = reward.isRequiresApproval();
        this.rewardKind = reward.getRewardKind().name();
        this.defaultDurationMinutes = reward.getDefaultDurationMinutes();
        this.acquisitionMode = reward.getAcquisitionMode().name();
        this.categories = reward.getCategories().stream()
                .map(RewardCategoryResponse::new)
                .toList();
        this.requirements = requirements == null ? List.of() : requirements;
        this.active = reward.isActive();
        this.createdAt = reward.getCreatedAt();
        this.updatedAt = reward.getUpdatedAt();
    }

    public static RewardDefinitionResponse from(RewardDefinition reward) {
        return new RewardDefinitionResponse(reward, List.of());
    }

    public static RewardDefinitionResponse from(
            RewardDefinition reward,
            List<RewardRequirementResponse> requirements
    ) {
        return new RewardDefinitionResponse(reward, requirements);
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
    public String getRewardKind() { return rewardKind; }
    public Integer getDefaultDurationMinutes() { return defaultDurationMinutes; }
    public String getAcquisitionMode() { return acquisitionMode; }
    public List<RewardCategoryResponse> getCategories() { return categories; }
    public List<RewardRequirementResponse> getRequirements() { return requirements; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
