package com.olehkobylianskyi.familypoints.repository;

import com.olehkobylianskyi.familypoints.entity.RewardObligation;
import com.olehkobylianskyi.familypoints.entity.RewardObligationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RewardObligationRepository extends JpaRepository<RewardObligation, Long> {
    List<RewardObligation> findByMemberIdAndStatusOrderByCreatedAtAsc(Long memberId, RewardObligationStatus status);
    List<RewardObligation> findByMemberWorkspaceIdAndStatusOrderByCreatedAtDesc(Long workspaceId, RewardObligationStatus status);
    Optional<RewardObligation> findByIdAndWorkspaceId(Long id, Long workspaceId);
}
