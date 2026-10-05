package com.olehkobylianskyi.familypoints.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.ArrayList;
import java.util.List;

public class RewardRequestReviewRequest {
    private Long pointTypeId;

    @Min(0)
    private Integer priceAmount;

    @Min(0)
    private Integer minimumReputation;

    @Min(1)
    private Integer durationMinutes;

    @Valid
    private List<RewardRequirementRequest> requirements = new ArrayList<>();

    public Long getPointTypeId() { return pointTypeId; }
    public void setPointTypeId(Long pointTypeId) { this.pointTypeId = pointTypeId; }
    public Integer getPriceAmount() { return priceAmount; }
    public void setPriceAmount(Integer priceAmount) { this.priceAmount = priceAmount; }
    public Integer getMinimumReputation() { return minimumReputation; }
    public void setMinimumReputation(Integer minimumReputation) { this.minimumReputation = minimumReputation; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public List<RewardRequirementRequest> getRequirements() { return requirements; }
    public void setRequirements(List<RewardRequirementRequest> requirements) { this.requirements = requirements; }
}
