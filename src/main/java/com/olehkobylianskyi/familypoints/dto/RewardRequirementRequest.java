package com.olehkobylianskyi.familypoints.dto;

import com.olehkobylianskyi.familypoints.entity.*;
import java.util.ArrayList;
import java.util.List;

public class RewardRequirementRequest {

    private RewardRequirementPhase phase = RewardRequirementPhase.BEFORE_REWARD;
    private RewardRequirementType requirementType = RewardRequirementType.CUSTOM;
    private Long taskDefinitionId;
    private RewardRequirementTimeScope timeScope;
    private Integer windowValue;
    private String description;
    private boolean required = true;
    private RewardBlockingMode blockingMode = RewardBlockingMode.NONE;
    private List<Long> blockedCategoryIds = new ArrayList<>();
    private List<Long> blockedRewardDefinitionIds = new ArrayList<>();

    public RewardRequirementPhase getPhase() { return phase; }
    public void setPhase(RewardRequirementPhase phase) { this.phase = phase; }
    public RewardRequirementType getRequirementType() { return requirementType; }
    public void setRequirementType(RewardRequirementType requirementType) { this.requirementType = requirementType; }
    public Long getTaskDefinitionId() { return taskDefinitionId; }
    public void setTaskDefinitionId(Long taskDefinitionId) { this.taskDefinitionId = taskDefinitionId; }
    public RewardRequirementTimeScope getTimeScope() { return timeScope; }
    public void setTimeScope(RewardRequirementTimeScope timeScope) { this.timeScope = timeScope; }
    public Integer getWindowValue() { return windowValue; }
    public void setWindowValue(Integer windowValue) { this.windowValue = windowValue; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
    public RewardBlockingMode getBlockingMode() { return blockingMode; }
    public void setBlockingMode(RewardBlockingMode blockingMode) { this.blockingMode = blockingMode; }
    public List<Long> getBlockedCategoryIds() { return blockedCategoryIds; }
    public void setBlockedCategoryIds(List<Long> blockedCategoryIds) { this.blockedCategoryIds = blockedCategoryIds; }
    public List<Long> getBlockedRewardDefinitionIds() { return blockedRewardDefinitionIds; }
    public void setBlockedRewardDefinitionIds(List<Long> blockedRewardDefinitionIds) { this.blockedRewardDefinitionIds = blockedRewardDefinitionIds; }
}
