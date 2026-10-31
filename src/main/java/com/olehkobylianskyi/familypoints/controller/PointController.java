package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.dto.PointBalanceResponse;
import com.olehkobylianskyi.familypoints.dto.PointOperationRequest;
import com.olehkobylianskyi.familypoints.dto.PointTransactionResponse;
import com.olehkobylianskyi.familypoints.service.PointService;
import com.olehkobylianskyi.familypoints.service.PointMutationService;
import com.olehkobylianskyi.familypoints.security.WorkspaceDataEpochFilter;
import jakarta.validation.Valid;
import com.olehkobylianskyi.familypoints.dto.OfflinePointOperationRequest;
import com.olehkobylianskyi.familypoints.dto.PointOperationTypedRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.olehkobylianskyi.familypoints.dto.PointBalancesResponse;

import java.util.List;

@RestController
@RequestMapping(
        "/workspaces/{workspaceId}/members/{memberId}/points"
)
@PreAuthorize("@workspaceSecurity.canAccessWorkspace(#workspaceId)")
public class PointController {

    private final PointService pointService;
    private final PointMutationService mutations;

    public PointController(PointService pointService, PointMutationService mutations) {
        this.pointService = pointService;
        this.mutations = mutations;
    }

    @PostMapping("/offline/{operation}")
    @PreAuthorize("@workspaceSecurity.canManagePoints(#workspaceId)")
    public PointTransactionResponse applyOfflineOperation(
            @PathVariable Long workspaceId,
            @PathVariable Long memberId,
            @PathVariable String operation,
            @Valid @RequestBody OfflinePointOperationRequest request,
            @RequestHeader(name = WorkspaceDataEpochFilter.HEADER, required = false) Long expectedEpoch
    ) {
        return PointTransactionResponse.from(
                mutations.applyOfflineOperation(workspaceId, memberId, operation, request, expectedEpoch)
        );
    }

    @PostMapping("/manual/{operation}")
    @PreAuthorize("@workspaceSecurity.canManagePoints(#workspaceId)")
    public PointTransactionResponse applyManualTyped(
            @PathVariable Long workspaceId,
            @PathVariable Long memberId,
            @PathVariable String operation,
            @Valid @RequestBody PointOperationTypedRequest request,
            @RequestHeader(name = WorkspaceDataEpochFilter.HEADER, required = false) Long expectedEpoch
    ) {
        return PointTransactionResponse.from(mutations.applyTypedManualOperation(
                workspaceId, memberId, operation, request, expectedEpoch
        ));
    }

    @PostMapping("/earn")
    @PreAuthorize("@workspaceSecurity.canManagePoints(#workspaceId)")
    public PointTransactionResponse earn(
            @PathVariable Long workspaceId,
            @PathVariable Long memberId,
            @Valid @RequestBody PointOperationRequest request,
            @RequestHeader(name = WorkspaceDataEpochFilter.HEADER, required = false) Long expectedEpoch
    ) {
        return PointTransactionResponse.from(
                mutations.applyManualOperation(workspaceId, memberId, "earn", request, expectedEpoch)
        );
    }

    @PostMapping("/spend")
    @PreAuthorize("@workspaceSecurity.canManagePoints(#workspaceId)")
    public PointTransactionResponse spend(
            @PathVariable Long workspaceId,
            @PathVariable Long memberId,
            @Valid @RequestBody PointOperationRequest request,
            @RequestHeader(name = WorkspaceDataEpochFilter.HEADER, required = false) Long expectedEpoch
    ) {
        return PointTransactionResponse.from(
                mutations.applyManualOperation(workspaceId, memberId, "spend", request, expectedEpoch)
        );
    }

    @PostMapping("/penalty")
    @PreAuthorize("@workspaceSecurity.canManagePoints(#workspaceId)")
    public PointTransactionResponse penalty(
            @PathVariable Long workspaceId,
            @PathVariable Long memberId,
            @Valid @RequestBody PointOperationRequest request,
            @RequestHeader(name = WorkspaceDataEpochFilter.HEADER, required = false) Long expectedEpoch
    ) {
        return PointTransactionResponse.from(
                mutations.applyManualOperation(workspaceId, memberId, "penalty", request, expectedEpoch)
        );
    }

    @GetMapping("/balance")
    public PointBalanceResponse getBalance(
            @PathVariable Long workspaceId,
            @PathVariable Long memberId
    ) {
        return new PointBalanceResponse(
                memberId,
                pointService.getBalance(
                        workspaceId,
                        memberId
                )
        );
    }

    @GetMapping("/history")
    public List<PointTransactionResponse> getHistory(
            @PathVariable Long workspaceId,
            @PathVariable Long memberId
    ) {
        return pointService
                .getHistory(workspaceId, memberId)
                .stream()
                .map(PointTransactionResponse::from)
                .toList();
    }

    @GetMapping("/balances")
    public PointBalancesResponse getBalances(
            @PathVariable Long workspaceId,
            @PathVariable Long memberId
    ) {
        return pointService.getBalances(
                workspaceId,
                memberId
        );
    }
}
