package com.olehkobylianskyi.familypoints.repository;

import com.olehkobylianskyi.familypoints.entity.RewardPurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

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

    @Query("""
            select p from RewardPurchase p
            join p.member m
            where m.workspace.id = :workspaceId
              and (:memberId is null or m.id = :memberId)
              and (:beforeTime is null or p.purchasedAt < :beforeTime
                or (p.purchasedAt = :beforeTime and p.id < :beforeId))
              and (:fromTime is null or p.purchasedAt >= :fromTime)
              and (:toTime is null or p.purchasedAt <= :toTime)
              and (:status is null or p.status = :status)
            order by p.purchasedAt desc, p.id desc
            """)
    List<RewardPurchase> findHistoryPage(
            @Param("workspaceId") Long workspaceId,
            @Param("memberId") Long memberId,
            @Param("beforeTime") LocalDateTime beforeTime,
            @Param("beforeId") Long beforeId,
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime,
            @Param("status") com.olehkobylianskyi.familypoints.entity.RewardPurchaseStatus status,
            Pageable pageable);

    List<RewardPurchase> findByMemberIdOrderByPurchasedAtDesc(Long memberId);
    Optional<RewardPurchase> findFirstByMemberIdAndRewardDefinitionIdOrderByPurchasedAtDesc(Long memberId, Long rewardDefinitionId);
    List<RewardPurchase> findByRewardDefinitionWorkspaceIdOrderByPurchasedAtDesc(Long workspaceId);
    List<RewardPurchase> findByMemberWorkspaceIdOrderByPurchasedAtDesc(Long workspaceId);
    Optional<RewardPurchase> findByIdAndRewardDefinitionWorkspaceId(Long id, Long workspaceId);
}
