package com.olehkobylianskyi.familypoints.repository;

import com.olehkobylianskyi.familypoints.entity.RewardRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RewardRequestRepository extends JpaRepository<RewardRequest, Long> {
    List<RewardRequest> findByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId);
    List<RewardRequest> findByRequestedByIdOrderByCreatedAtDesc(Long memberId);
    Optional<RewardRequest> findByIdAndWorkspaceId(Long id, Long workspaceId);
}
