package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.service.RewardPurchaseOperationService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** Additive idempotent routes: legacy reward purchase endpoints stay unchanged. */
@RestController
@RequestMapping("/workspaces/{workspaceId}/rewards")
public class RewardPurchaseOperationController {
    private final RewardPurchaseOperationService operations;

    public RewardPurchaseOperationController(RewardPurchaseOperationService operations) {
        this.operations = operations;
    }

    @PostMapping("/{rewardId}/purchase/idempotent")
    public ResponseEntity<RewardPurchaseOperationService.Receipt> purchaseSelf(
            @PathVariable long workspaceId, @PathVariable long rewardId,
            @RequestHeader("Idempotency-Key") UUID operationId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(operations.purchaseSelf(workspaceId, rewardId, operationId));
    }

    @PostMapping("/{rewardId}/purchase-for-member/{memberId}/idempotent")
    public ResponseEntity<RewardPurchaseOperationService.Receipt> purchaseForMember(
            @PathVariable long workspaceId, @PathVariable long rewardId,
            @PathVariable long memberId,
            @RequestHeader("Idempotency-Key") UUID operationId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(operations.purchaseForMember(workspaceId, memberId, rewardId, operationId));
    }

    @GetMapping("/purchases/receipts/{operationId}")
    public ResponseEntity<RewardPurchaseOperationService.Receipt> receipt(
            @PathVariable long workspaceId, @PathVariable UUID operationId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(operations.receipt(workspaceId, operationId));
    }
}
