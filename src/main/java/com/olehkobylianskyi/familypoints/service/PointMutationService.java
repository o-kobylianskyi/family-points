package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.dto.OfflinePointOperationRequest;
import com.olehkobylianskyi.familypoints.dto.PointOperationRequest;
import com.olehkobylianskyi.familypoints.dto.PointOperationTypedRequest;
import com.olehkobylianskyi.familypoints.entity.PointTransaction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * HTTP point mutation boundary. The family fence and PointService's REQUIRED
 * ledger transaction participate in the same transaction, family lock first.
 * Internal task/reward postings still need their own outer transaction fence.
 */
@Service
public class PointMutationService {
    private final WorkspaceEpochTransactionGuard guard;
    private final PointService points;

    public PointMutationService(WorkspaceEpochTransactionGuard guard, PointService points) {
        this.guard = guard;
        this.points = points;
    }

    @Transactional
    public PointTransaction applyOfflineOperation(Long workspaceId, Long memberId,
            String operation, OfflinePointOperationRequest request, Long expectedEpoch) {
        guard.requireCurrentEpoch(workspaceId, expectedEpoch);
        return points.applyOfflineOperation(workspaceId, memberId, request.getOperationId(),
                request.getPointTypeId(), operation, request.getAmount(), request.getDescription());
    }

    @Transactional
    public PointTransaction applyTypedManualOperation(Long workspaceId, Long memberId,
            String operation, PointOperationTypedRequest request, Long expectedEpoch) {
        guard.requireCurrentEpoch(workspaceId, expectedEpoch);
        return points.applyTypedManualOperation(workspaceId, memberId, request.getPointTypeId(),
                operation, request.getAmount(), request.getDescription());
    }

    @Transactional
    public PointTransaction applyManualOperation(Long workspaceId, Long memberId,
            String operation, PointOperationRequest request, Long expectedEpoch) {
        guard.requireCurrentEpoch(workspaceId, expectedEpoch);
        return switch (operation) {
            case "earn" -> points.earn(workspaceId, memberId, request.getAmount(), request.getDescription());
            case "spend" -> points.spend(workspaceId, memberId, request.getAmount(), request.getDescription());
            case "penalty" -> points.penalty(workspaceId, memberId, request.getAmount(), request.getDescription());
            default -> throw new IllegalArgumentException("Invalid manual point operation");
        };
    }
}
