package com.olehkobylianskyi.familypoints.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.olehkobylianskyi.familypoints.entity.RewardAcquisitionMode;
import com.olehkobylianskyi.familypoints.entity.RewardKind;
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

    @Min(0)
    private Integer cooldownSeconds = 30;

    @Min(1)
    private Integer memberTotalLimit;

    @Min(1)
    private Integer memberDailyLimit;

    @Min(1)
    private Integer memberWeeklyLimit;

    @Min(1)
    private Integer workspaceTotalLimit;

    @Min(1)
    private Integer workspaceDailyLimit;

    @Min(1)
    private Integer workspaceWeeklyLimit;

    private boolean extraRequestAllowed;

    private boolean requiresApproval;

    private RewardKind rewardKind = RewardKind.STANDARD;

    @Min(1)
    private Integer defaultDurationMinutes;

    private RewardAcquisitionMode acquisitionMode = RewardAcquisitionMode.DIRECT;

    private List<Long> categoryIds = new ArrayList<>();

    @Valid
    private List<RewardRequirementRequest> requirements = new ArrayList<>();

    public Integer getCooldownSeconds() { return cooldownSeconds; }
    public void setCooldownSeconds(Integer value) { cooldownSeconds = value; }
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
    public Integer getMemberTotalLimit() { return memberTotalLimit; }
    public void setMemberTotalLimit(Integer value) { memberTotalLimit = value; }
    public Integer getMemberDailyLimit() { return memberDailyLimit; }
    public void setMemberDailyLimit(Integer value) { memberDailyLimit = value; }
    public Integer getMemberWeeklyLimit() { return memberWeeklyLimit; }
    public void setMemberWeeklyLimit(Integer value) { memberWeeklyLimit = value; }
    public Integer getWorkspaceTotalLimit() { return workspaceTotalLimit; }
    public void setWorkspaceTotalLimit(Integer value) { workspaceTotalLimit = value; }
    public Integer getWorkspaceDailyLimit() { return workspaceDailyLimit; }
    public void setWorkspaceDailyLimit(Integer value) { workspaceDailyLimit = value; }
    public Integer getWorkspaceWeeklyLimit() { return workspaceWeeklyLimit; }
    public void setWorkspaceWeeklyLimit(Integer value) { workspaceWeeklyLimit = value; }
    public boolean isExtraRequestAllowed() { return extraRequestAllowed; }
    public void setExtraRequestAllowed(boolean allowed) { extraRequestAllowed = allowed; }
    public boolean isRequiresApproval() { return requiresApproval; }
    public void setRequiresApproval(boolean requiresApproval) { this.requiresApproval = requiresApproval; }
    public RewardKind getRewardKind() { return rewardKind; }
    public void setRewardKind(RewardKind rewardKind) { this.rewardKind = rewardKind; }
    public Integer getDefaultDurationMinutes() { return defaultDurationMinutes; }
    public void setDefaultDurationMinutes(Integer defaultDurationMinutes) { this.defaultDurationMinutes = defaultDurationMinutes; }
    public RewardAcquisitionMode getAcquisitionMode() { return acquisitionMode; }
    public void setAcquisitionMode(RewardAcquisitionMode acquisitionMode) { this.acquisitionMode = acquisitionMode; }
    public List<Long> getCategoryIds() { return categoryIds; }
    public void setCategoryIds(List<Long> categoryIds) { this.categoryIds = categoryIds; }
    public List<RewardRequirementRequest> getRequirements() { return requirements; }
    public void setRequirements(List<RewardRequirementRequest> requirements) { this.requirements = requirements; }
}
