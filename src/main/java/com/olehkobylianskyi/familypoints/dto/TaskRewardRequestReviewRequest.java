package com.olehkobylianskyi.familypoints.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class TaskRewardRequestReviewRequest {

    private Long pointTypeId;

    @Min(0)
    private Integer pointAmount;

    @Min(0)
    private Integer reputationAmount;

    private Long rewardDefinitionId;

    @Size(max = 150)
    private String customRewardTitle;

    @Size(max = 500)
    private String comment;

    public Long getPointTypeId() { return pointTypeId; }
    public void setPointTypeId(Long pointTypeId) { this.pointTypeId = pointTypeId; }
    public Integer getPointAmount() { return pointAmount; }
    public void setPointAmount(Integer pointAmount) { this.pointAmount = pointAmount; }
    public Integer getReputationAmount() { return reputationAmount; }
    public void setReputationAmount(Integer reputationAmount) { this.reputationAmount = reputationAmount; }
    public Long getRewardDefinitionId() { return rewardDefinitionId; }
    public void setRewardDefinitionId(Long rewardDefinitionId) { this.rewardDefinitionId = rewardDefinitionId; }
    public String getCustomRewardTitle() { return customRewardTitle; }
    public void setCustomRewardTitle(String customRewardTitle) { this.customRewardTitle = customRewardTitle; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
