package com.olehkobylianskyi.familypoints.repository;

import com.olehkobylianskyi.familypoints.entity.RewardDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RewardDefinitionRepository extends JpaRepository<RewardDefinition, Long> {
    List<RewardDefinition> findByWorkspaceIdAndActiveTrueOrderByIdAsc(Long workspaceId);
    List<RewardDefinition> findByWorkspaceIdOrderByIdAsc(Long workspaceId);
    Optional<RewardDefinition> findByIdAndWorkspaceId(Long id, Long workspaceId);
}
