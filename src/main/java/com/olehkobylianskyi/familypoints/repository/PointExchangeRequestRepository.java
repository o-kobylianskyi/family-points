package com.olehkobylianskyi.familypoints.repository;

import com.olehkobylianskyi.familypoints.entity.PointExchangeRequest;
import com.olehkobylianskyi.familypoints.entity.PointExchangeStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PointExchangeRequestRepository
        extends JpaRepository<PointExchangeRequest, Long> {

    List<PointExchangeRequest>
    findByMemberWorkspaceIdAndStatusOrderByRequestedAtDesc(
            Long workspaceId,
            PointExchangeStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select r
            from PointExchangeRequest r
            where r.id = :requestId
              and r.member.workspace.id = :workspaceId
            """)
    Optional<PointExchangeRequest>
    findByIdAndWorkspaceIdForUpdate(
            @Param("requestId") Long requestId,
            @Param("workspaceId") Long workspaceId
    );
}