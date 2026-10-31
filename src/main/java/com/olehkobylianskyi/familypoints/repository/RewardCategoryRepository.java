package com.olehkobylianskyi.familypoints.repository;

import com.olehkobylianskyi.familypoints.entity.RewardCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RewardCategoryRepository extends JpaRepository<RewardCategory, Long> {
    List<RewardCategory> findByWorkspaceIdAndActiveTrueOrderBySortOrderAscIdAsc(Long workspaceId);
    Optional<RewardCategory> findByIdAndWorkspaceId(Long id, Long workspaceId);
}
