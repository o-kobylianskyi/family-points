package com.olehkobylianskyi.familypoints.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class RewardDefinitionRequest {

    @NotBlank
    @Size(max = 150)
    private String title;

    @Size(max = 1000)
    private String description;

    @NotNull
    private Long pointTypeId;

    @Min(1)
    private int priceAmount;

    @Min(0)
    private Integer minimumReputation;

    private boolean requiresApproval;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getPointTypeId() { return pointTypeId; }
    public void setPointTypeId(Long pointTypeId) { this.pointTypeId = pointTypeId; }
    public int getPriceAmount() { return priceAmount; }
    public void setPriceAmount(int priceAmount) { this.priceAmount = priceAmount; }
    public Integer getMinimumReputation() { return minimumReputation; }
    public void setMinimumReputation(Integer minimumReputation) { this.minimumReputation = minimumReputation; }
    public boolean isRequiresApproval() { return requiresApproval; }
    public void setRequiresApproval(boolean requiresApproval) { this.requiresApproval = requiresApproval; }
}
