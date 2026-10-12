package com.olehkobylianskyi.familypoints.repository;

import com.olehkobylianskyi.familypoints.entity.RewardPurchase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface RewardPurchaseRepository extends JpaRepository<RewardPurchase, Long> {
    @Query("""
            select count(p) from RewardPurchase p
            where p.rewardDefinition.id = :rewardId
              and p.status <> com.olehkobylianskyi.familypoints.entity.RewardPurchaseStatus.CANCELLED
              and (:memberId is null or p.member.id = :memberId)
              and (:fromTime is null or p.purchasedAt >= :fromTime)
              and (:untilTime is null or p.purchasedAt < :untilTime)
            """)
    long countForLimit(@Param("rewardId") Long rewardId,
                       @Param("memberId") Long memberId,
                       @Param("fromTime") LocalDateTime fromTime,
                       @Param("untilTime") LocalDateTime untilTime);

    List<RewardPurchase> findByMemberIdOrderByPurchasedAtDesc(Long memberId);
    Optional<RewardPurchase> findFirstByMemberIdAndRewardDefinitionIdOrderByPurchasedAtDesc(Long memberId, Long rewardDefinitionId);
    List<RewardPurchase> findByRewardDefinitionWorkspaceIdOrderByPurchasedAtDesc(Long workspaceId);
    List<RewardPurchase> findByMemberWorkspaceIdOrderByPurchasedAtDesc(Long workspaceId);
    Optional<RewardPurchase> findByIdAndRewardDefinitionWorkspaceId(Long id, Long workspaceId);
}
