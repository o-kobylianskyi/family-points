package com.olehkobylianskyi.familypoints.repository;

import com.olehkobylianskyi.familypoints.entity.RewardRequirement;
import com.olehkobylianskyi.familypoints.entity.RewardRequirementPhase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RewardRequirementRepository extends JpaRepository<RewardRequirement, Long> {
    List<RewardRequirement> findByRewardDefinitionIdOrderBySortOrderAscIdAsc(Long rewardDefinitionId);
    List<RewardRequirement> findByRewardRequestIdOrderBySortOrderAscIdAsc(Long rewardRequestId);
    List<RewardRequirement> findByRewardRequestIdAndPhaseOrderBySortOrderAscIdAsc(Long rewardRequestId, RewardRequirementPhase phase);
    List<RewardRequirement> findByRewardDefinitionIdAndPhaseOrderBySortOrderAscIdAsc(Long rewardDefinitionId, RewardRequirementPhase phase);
    void deleteByRewardDefinitionId(Long rewardDefinitionId);
    void deleteByRewardRequestId(Long rewardRequestId);
}
