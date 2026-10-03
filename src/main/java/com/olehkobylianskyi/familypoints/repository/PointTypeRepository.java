package com.olehkobylianskyi.familypoints.repository;

import com.olehkobylianskyi.familypoints.entity.PointType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PointTypeRepository
        extends JpaRepository<PointType, Long> {

    Optional<PointType> findByWorkspaceIdAndCode(
            Long workspaceId,
            String code
    );

    Optional<PointType> findByIdAndWorkspaceId(
            Long id,
            Long workspaceId
    );

    List<PointType> findByWorkspaceIdAndActiveTrueOrderBySortOrderAsc(
            Long workspaceId
    );
}