package com.olehkobylianskyi.familypoints.dto;

import com.olehkobylianskyi.familypoints.entity.PointExchangeApprovalMode;
import com.olehkobylianskyi.familypoints.entity.PointExchangeRate;

public class PointExchangeRateResponse {

    private final Long id;

    private final Long fromPointTypeId;
    private final String fromPointTypeCode;

    private final Long toPointTypeId;
    private final String toPointTypeCode;

    private final int fromAmount;
    private final int toAmount;

    private final PointExchangeApprovalMode approvalMode;
    private final boolean active;

    public PointExchangeRateResponse(
            Long id,
            Long fromPointTypeId,
            String fromPointTypeCode,
            Long toPointTypeId,
            String toPointTypeCode,
            int fromAmount,
            int toAmount,
            PointExchangeApprovalMode approvalMode,
            boolean active
    ) {
        this.id = id;
        this.fromPointTypeId = fromPointTypeId;
        this.fromPointTypeCode = fromPointTypeCode;
        this.toPointTypeId = toPointTypeId;
        this.toPointTypeCode = toPointTypeCode;
        this.fromAmount = fromAmount;
        this.toAmount = toAmount;
        this.approvalMode = approvalMode;
        this.active = active;
    }

    public static PointExchangeRateResponse from(
            PointExchangeRate rate
    ) {
        return new PointExchangeRateResponse(
                rate.getId(),

                rate.getFromPointType().getId(),
                rate.getFromPointType().getCode(),

                rate.getToPointType().getId(),
                rate.getToPointType().getCode(),

                rate.getFromAmount(),
                rate.getToAmount(),

                rate.getApprovalMode(),
                rate.isActive()
        );
    }

    public Long getId() {
        return id;
    }

    public Long getFromPointTypeId() {
        return fromPointTypeId;
    }

    public String getFromPointTypeCode() {
        return fromPointTypeCode;
    }

    public Long getToPointTypeId() {
        return toPointTypeId;
    }

    public String getToPointTypeCode() {
        return toPointTypeCode;
    }

    public int getFromAmount() {
        return fromAmount;
    }

    public int getToAmount() {
        return toAmount;
    }

    public PointExchangeApprovalMode getApprovalMode() {
        return approvalMode;
    }

    public boolean isActive() {
        return active;
    }
}