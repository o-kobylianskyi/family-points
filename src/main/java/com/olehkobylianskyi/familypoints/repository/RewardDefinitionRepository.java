package com.olehkobylianskyi.familypoints.repository;

import com.olehkobylianskyi.familypoints.entity.RewardDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface RewardDefinitionRepository extends JpaRepository<RewardDefinition, Long> {
    List<RewardDefinition> findByWorkspaceIdAndActiveTrueOrderByIdAsc(Long workspaceId);
    List<RewardDefinition> findByWorkspaceIdOrderByIdAsc(Long workspaceId);
    Optional<RewardDefinition> findByIdAndWorkspaceId(Long id, Long workspaceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RewardDefinition r where r.id = :id and r.workspace.id = :workspaceId")
    Optional<RewardDefinition> findForPurchase(@Param("id") Long id,
                                               @Param("workspaceId") Long workspaceId);
}
