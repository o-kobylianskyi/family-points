package com.olehkobylianskyi.familypoints.repository;

import com.olehkobylianskyi.familypoints.entity.RewardPurchase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RewardPurchaseRepository extends JpaRepository<RewardPurchase, Long> {
    List<RewardPurchase> findByMemberIdOrderByPurchasedAtDesc(Long memberId);
    List<RewardPurchase> findByRewardDefinitionWorkspaceIdOrderByPurchasedAtDesc(Long workspaceId);
    Optional<RewardPurchase> findByIdAndRewardDefinitionWorkspaceId(Long id, Long workspaceId);
}
