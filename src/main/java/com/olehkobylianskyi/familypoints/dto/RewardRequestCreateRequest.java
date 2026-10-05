package com.olehkobylianskyi.familypoints.dto;

import jakarta.validation.constraints.Size;

public class RewardRequestCreateRequest {
    private Long rewardDefinitionId;

    @Size(max = 150)
    private String title;

    @Size(max = 1000)
    private String description;

    public Long getRewardDefinitionId() { return rewardDefinitionId; }
    public void setRewardDefinitionId(Long rewardDefinitionId) { this.rewardDefinitionId = rewardDefinitionId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
