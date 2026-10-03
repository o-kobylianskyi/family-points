package com.olehkobylianskyi.familypoints.repository;

import com.olehkobylianskyi.familypoints.entity.PointExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PointExchangeRateRepository
        extends JpaRepository<PointExchangeRate, Long> {

    List<PointExchangeRate>
    findByWorkspaceIdAndActiveTrue(Long workspaceId);

    Optional<PointExchangeRate>
    findByIdAndWorkspaceIdAndActiveTrue(
            Long id,
            Long workspaceId
    );

    boolean existsByWorkspaceIdAndFromPointTypeIdAndToPointTypeId(
            Long workspaceId,
            Long fromPointTypeId,
            Long toPointTypeId
    );
}