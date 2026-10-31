package com.olehkobylianskyi.familypoints.dto;

import com.olehkobylianskyi.familypoints.entity.*;
import java.util.List;

public class RewardRequirementResponse {
    private final Long id;
    private final RewardRequirementPhase phase;
    private final RewardRequirementType requirementType;
    private final Long taskDefinitionId;
    private final String taskTitle;
    private final RewardRequirementTimeScope timeScope;
    private final Integer windowValue;
    private final String description;
    private final boolean required;
    private final RewardBlockingMode blockingMode;
    private final List<Long> blockedCategoryIds;
    private final List<Long> blockedRewardDefinitionIds;

    private RewardRequirementResponse(RewardRequirement requirement) {
        this.id = requirement.getId();
        this.phase = requirement.getPhase();
        this.requirementType = requirement.getRequirementType();
        this.taskDefinitionId = requirement.getTaskDefinition() == null ? null : requirement.getTaskDefinition().getId();
        this.taskTitle = requirement.getTaskDefinition() == null ? null : requirement.getTaskDefinition().getTitle();
        this.timeScope = requirement.getTimeScope();
        this.windowValue = requirement.getWindowValue();
        this.description = requirement.getDescription();
        this.required = requirement.isRequired();
        this.blockingMode = requirement.getBlockingMode();
        this.blockedCategoryIds = requirement.getBlockedCategories().stream().map(RewardCategory::getId).toList();
        this.blockedRewardDefinitionIds = requirement.getBlockedRewards().stream().map(RewardDefinition::getId).toList();
    }

    public static RewardRequirementResponse from(RewardRequirement requirement) {
        return new RewardRequirementResponse(requirement);
    }

    public Long getId() { return id; }
    public RewardRequirementPhase getPhase() { return phase; }
    public RewardRequirementType getRequirementType() { return requirementType; }
    public Long getTaskDefinitionId() { return taskDefinitionId; }
    public String getTaskTitle() { return taskTitle; }
    public RewardRequirementTimeScope getTimeScope() { return timeScope; }
    public Integer getWindowValue() { return windowValue; }
    public String getDescription() { return description; }
    public boolean isRequired() { return required; }
    public RewardBlockingMode getBlockingMode() { return blockingMode; }
    public List<Long> getBlockedCategoryIds() { return blockedCategoryIds; }
    public List<Long> getBlockedRewardDefinitionIds() { return blockedRewardDefinitionIds; }
}
