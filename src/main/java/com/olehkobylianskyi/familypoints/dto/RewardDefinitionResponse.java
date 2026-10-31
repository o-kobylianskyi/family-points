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
    private final int cooldownSeconds;
    private final Integer memberTotalLimit;
    private final Integer memberDailyLimit;
    private final Integer memberWeeklyLimit;
    private final Integer workspaceTotalLimit;
    private final Integer workspaceDailyLimit;
    private final Integer workspaceWeeklyLimit;
    private final boolean extraRequestAllowed;
    private final boolean requiresApproval;
    private final String rewardKind;
    private final Integer defaultDurationMinutes;
    private final String acquisitionMode;
    private final String availabilityMode;
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
        this.cooldownSeconds = reward.getCooldownSeconds();
        this.memberTotalLimit = reward.getMemberTotalLimit();
        this.memberDailyLimit = reward.getMemberDailyLimit();
        this.memberWeeklyLimit = reward.getMemberWeeklyLimit();
        this.workspaceTotalLimit = reward.getWorkspaceTotalLimit();
        this.workspaceDailyLimit = reward.getWorkspaceDailyLimit();
        this.workspaceWeeklyLimit = reward.getWorkspaceWeeklyLimit();
        this.extraRequestAllowed = reward.isExtraRequestAllowed();
        this.requiresApproval = reward.isRequiresApproval();
        this.rewardKind = reward.getRewardKind().name();
        this.defaultDurationMinutes = reward.getDefaultDurationMinutes();
        this.acquisitionMode = reward.getAcquisitionMode().name();
        this.availabilityMode = reward.getAvailabilityMode() == null ? "ALWAYS" : reward.getAvailabilityMode().name();
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
    public int getCooldownSeconds() { return cooldownSeconds; }
    public Integer getMemberTotalLimit() { return memberTotalLimit; }
    public Integer getMemberDailyLimit() { return memberDailyLimit; }
    public Integer getMemberWeeklyLimit() { return memberWeeklyLimit; }
    public Integer getWorkspaceTotalLimit() { return workspaceTotalLimit; }
    public Integer getWorkspaceDailyLimit() { return workspaceDailyLimit; }
    public Integer getWorkspaceWeeklyLimit() { return workspaceWeeklyLimit; }
    public boolean isExtraRequestAllowed() { return extraRequestAllowed; }
    public boolean isRequiresApproval() { return requiresApproval; }
    public String getRewardKind() { return rewardKind; }
    public Integer getDefaultDurationMinutes() { return defaultDurationMinutes; }
    public String getAcquisitionMode() { return acquisitionMode; }
    public String getAvailabilityMode() { return availabilityMode; }
    public List<RewardCategoryResponse> getCategories() { return categories; }
    public List<RewardRequirementResponse> getRequirements() { return requirements; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
