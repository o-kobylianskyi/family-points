package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.dto.PointBalanceResponse;
import com.olehkobylianskyi.familypoints.dto.PointOperationRequest;
import com.olehkobylianskyi.familypoints.dto.PointTransactionResponse;
import com.olehkobylianskyi.familypoints.service.PointService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import com.olehkobylianskyi.familypoints.dto.PointBalancesResponse;

import java.util.List;

@RestController
@RequestMapping(
        "/workspaces/{workspaceId}/members/{memberId}/points"
)
public class PointController {

    private final PointService pointService;

    public PointController(PointService pointService) {
        this.pointService = pointService;
    }

    @PostMapping("/earn")
    public PointTransactionResponse earn(
            @PathVariable Long workspaceId,
            @PathVariable Long memberId,
            @Valid @RequestBody PointOperationRequest request
    ) {
        return PointTransactionResponse.from(
                pointService.earn(
                        workspaceId,
                        memberId,
                        request.getAmount(),
                        request.getDescription()
                )
        );
    }

    @PostMapping("/spend")
    public PointTransactionResponse spend(
            @PathVariable Long workspaceId,
            @PathVariable Long memberId,
            @Valid @RequestBody PointOperationRequest request
    ) {
        return PointTransactionResponse.from(
                pointService.spend(
                        workspaceId,
                        memberId,
                        request.getAmount(),
                        request.getDescription()
                )
        );
    }

    @PostMapping("/penalty")
    public PointTransactionResponse penalty(
            @PathVariable Long workspaceId,
            @PathVariable Long memberId,
            @Valid @RequestBody PointOperationRequest request
    ) {
        return PointTransactionResponse.from(
                pointService.penalty(
                        workspaceId,
                        memberId,
                        request.getAmount(),
                        request.getDescription()
                )
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