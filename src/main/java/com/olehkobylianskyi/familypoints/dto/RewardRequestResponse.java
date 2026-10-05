package com.olehkobylianskyi.familypoints.dto;

import com.olehkobylianskyi.familypoints.entity.RewardRequest;
import java.time.LocalDateTime;
import java.util.List;

public class RewardRequestResponse {
    private final Long id;
    private final Long requestedByMemberId;
    private final String requestedByMemberName;
    private final Long rewardDefinitionId;
    private final String title;
    private final String description;
    private final String status;
    private final Long pointTypeId;
    private final String pointTypeCode;
    private final Integer priceAmount;
    private final Integer minimumReputation;
    private final LocalDateTime createdAt;
    private final LocalDateTime reviewedAt;
    private final List<RewardRequirementResponse> requirements;

    public RewardRequestResponse(RewardRequest request, List<RewardRequirementResponse> requirements) {
        this.id = request.getId();
        this.requestedByMemberId = request.getRequestedBy().getId();
        this.requestedByMemberName = request.getRequestedBy().getName();
        this.rewardDefinitionId = request.getRewardDefinition() == null ? null : request.getRewardDefinition().getId();
        this.title = request.getTitle();
        this.description = request.getDescription();
        this.status = request.getStatus().name();
        this.pointTypeId = request.getApprovedPointType() == null ? null : request.getApprovedPointType().getId();
        this.pointTypeCode = request.getApprovedPointType() == null ? null : request.getApprovedPointType().getCode();
        this.priceAmount = request.getApprovedPriceAmount();
        this.minimumReputation = request.getMinimumReputation();
        this.createdAt = request.getCreatedAt();
        this.reviewedAt = request.getReviewedAt();
        this.requirements = requirements;
    }

    public Long getId() { return id; }
    public Long getRequestedByMemberId() { return requestedByMemberId; }
    public String getRequestedByMemberName() { return requestedByMemberName; }
    public Long getRewardDefinitionId() { return rewardDefinitionId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public Long getPointTypeId() { return pointTypeId; }
    public String getPointTypeCode() { return pointTypeCode; }
    public Integer getPriceAmount() { return priceAmount; }
    public Integer getMinimumReputation() { return minimumReputation; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public List<RewardRequirementResponse> getRequirements() { return requirements; }
}
