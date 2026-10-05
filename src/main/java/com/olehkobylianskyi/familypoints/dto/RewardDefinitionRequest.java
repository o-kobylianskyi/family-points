package com.olehkobylianskyi.familypoints.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.olehkobylianskyi.familypoints.entity.RewardAcquisitionMode;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;

public class RewardDefinitionRequest {

    @NotBlank
    @Size(max = 150)
    private String title;

    @Size(max = 1000)
    private String description;

    @NotNull
    private Long pointTypeId;

    @Min(0)
    private int priceAmount;

    @Min(0)
    private Integer minimumReputation;

    private boolean requiresApproval;

    private RewardAcquisitionMode acquisitionMode = RewardAcquisitionMode.DIRECT;

    private List<Long> categoryIds = new ArrayList<>();

    @Valid
    private List<RewardRequirementRequest> requirements = new ArrayList<>();

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
    public RewardAcquisitionMode getAcquisitionMode() { return acquisitionMode; }
    public void setAcquisitionMode(RewardAcquisitionMode acquisitionMode) { this.acquisitionMode = acquisitionMode; }
    public List<Long> getCategoryIds() { return categoryIds; }
    public void setCategoryIds(List<Long> categoryIds) { this.categoryIds = categoryIds; }
    public List<RewardRequirementRequest> getRequirements() { return requirements; }
    public void setRequirements(List<RewardRequirementRequest> requirements) { this.requirements = requirements; }
}
