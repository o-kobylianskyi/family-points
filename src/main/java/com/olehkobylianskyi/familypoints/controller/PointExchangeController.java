package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.dto.*;
import com.olehkobylianskyi.familypoints.entity.PointExchangeRate;
import com.olehkobylianskyi.familypoints.entity.PointExchangeRequest;
import com.olehkobylianskyi.familypoints.service.PointExchangeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/workspaces/{workspaceId}/point-exchanges")
public class PointExchangeController {

    private final PointExchangeService pointExchangeService;

    public PointExchangeController(
            PointExchangeService pointExchangeService
    ) {
        this.pointExchangeService = pointExchangeService;
    }

    @PostMapping("/rates")
    public ResponseEntity<PointExchangeRateResponse> createRate(
            @PathVariable Long workspaceId,
            @Valid @RequestBody PointExchangeRateRequest request
    ) {
        PointExchangeRate rate =
                pointExchangeService.createRate(
                        workspaceId,
                        request.getFromPointTypeId(),
                        request.getToPointTypeId(),
                        request.getFromAmount(),
                        request.getToAmount(),
                        request.getApprovalMode()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(PointExchangeRateResponse.from(rate));
    }

    @GetMapping("/rates")
    public List<PointExchangeRateResponse> getRates(
            @PathVariable Long workspaceId
    ) {
        return pointExchangeService
                .getRates(workspaceId)
                .stream()
                .map(PointExchangeRateResponse::from)
                .toList();
    }

    @PostMapping("/members/{memberId}/requests")
    public ResponseEntity<PointExchangeResponse> createExchange(
            @PathVariable Long workspaceId,
            @PathVariable Long memberId,
            @Valid @RequestBody PointExchangeCreateRequest request
    ) {
        PointExchangeRequest exchange =
                pointExchangeService.createExchange(
                        workspaceId,
                        memberId,
                        request.getExchangeRateId(),
                        request.getFromAmount()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(PointExchangeResponse.from(exchange));
    }

    @GetMapping("/requests/pending")
    public List<PointExchangeResponse> getPendingRequests(
            @PathVariable Long workspaceId
    ) {
        return pointExchangeService
                .getPendingRequests(workspaceId)
                .stream()
                .map(PointExchangeResponse::from)
                .toList();
    }

    @PostMapping("/requests/{requestId}/approve")
    public PointExchangeResponse approve(
            @PathVariable Long workspaceId,
            @PathVariable Long requestId,
            @RequestParam Long parentMemberId
    ) {
        PointExchangeRequest exchange =
                pointExchangeService.approve(
                        workspaceId,
                        requestId,
                        parentMemberId
                );

        return PointExchangeResponse.from(exchange);
    }

    @PostMapping("/requests/{requestId}/reject")
    public PointExchangeResponse reject(
            @PathVariable Long workspaceId,
            @PathVariable Long requestId,
            @RequestParam Long parentMemberId,
            @Valid @RequestBody PointExchangeRejectRequest request
    ) {
        PointExchangeRequest exchange =
                pointExchangeService.reject(
                        workspaceId,
                        requestId,
                        parentMemberId,
                        request.getReason()
                );

        return PointExchangeResponse.from(exchange);
    }
}