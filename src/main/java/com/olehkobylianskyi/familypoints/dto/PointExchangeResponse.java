package com.olehkobylianskyi.familypoints.dto;

import com.olehkobylianskyi.familypoints.entity.PointExchangeRequest;
import com.olehkobylianskyi.familypoints.entity.PointExchangeStatus;

import java.time.LocalDateTime;

public class PointExchangeResponse {

    private final Long id;
    private final Long memberId;

    private final String fromPointTypeCode;
    private final String toPointTypeCode;

    private final int rateFromAmount;
    private final int rateToAmount;

    private final int fromAmount;
    private final int toAmount;

    private final PointExchangeStatus status;

    private final LocalDateTime requestedAt;
    private final LocalDateTime processedAt;

    private final Long processedByMemberId;
    private final String rejectionReason;

    public PointExchangeResponse(
            Long id,
            Long memberId,
            String fromPointTypeCode,
            String toPointTypeCode,
            int rateFromAmount,
            int rateToAmount,
            int fromAmount,
            int toAmount,
            PointExchangeStatus status,
            LocalDateTime requestedAt,
            LocalDateTime processedAt,
            Long processedByMemberId,
            String rejectionReason
    ) {
        this.id = id;
        this.memberId = memberId;
        this.fromPointTypeCode = fromPointTypeCode;
        this.toPointTypeCode = toPointTypeCode;
        this.rateFromAmount = rateFromAmount;
        this.rateToAmount = rateToAmount;
        this.fromAmount = fromAmount;
        this.toAmount = toAmount;
        this.status = status;
        this.requestedAt = requestedAt;
        this.processedAt = processedAt;
        this.processedByMemberId = processedByMemberId;
        this.rejectionReason = rejectionReason;
    }

    public static PointExchangeResponse from(
            PointExchangeRequest request
    ) {
        return new PointExchangeResponse(
                request.getId(),
                request.getMember().getId(),

                request.getFromPointType().getCode(),
                request.getToPointType().getCode(),

                request.getRateFromAmount(),
                request.getRateToAmount(),

                request.getFromAmount(),
                request.getToAmount(),

                request.getStatus(),

                request.getRequestedAt(),
                request.getProcessedAt(),

                request.getProcessedBy() == null
                        ? null
                        : request.getProcessedBy().getId(),

                request.getRejectionReason()
        );
    }

    public Long getId() {
        return id;
    }

    public Long getMemberId() {
        return memberId;
    }

    public String getFromPointTypeCode() {
        return fromPointTypeCode;
    }

    public String getToPointTypeCode() {
        return toPointTypeCode;
    }

    public int getRateFromAmount() {
        return rateFromAmount;
    }

    public int getRateToAmount() {
        return rateToAmount;
    }

    public int getFromAmount() {
        return fromAmount;
    }

    public int getToAmount() {
        return toAmount;
    }

    public PointExchangeStatus getStatus() {
        return status;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public Long getProcessedByMemberId() {
        return processedByMemberId;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }
}