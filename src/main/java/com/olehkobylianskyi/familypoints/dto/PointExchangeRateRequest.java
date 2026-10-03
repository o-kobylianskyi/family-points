package com.olehkobylianskyi.familypoints.dto;

import com.olehkobylianskyi.familypoints.entity.PointExchangeApprovalMode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class PointExchangeRateRequest {

    @NotNull
    private Long fromPointTypeId;

    @NotNull
    private Long toPointTypeId;

    @Min(1)
    private int fromAmount;

    @Min(1)
    private int toAmount;

    @NotNull
    private PointExchangeApprovalMode approvalMode;

    public PointExchangeRateRequest() {
    }

    public Long getFromPointTypeId() {
        return fromPointTypeId;
    }

    public void setFromPointTypeId(Long fromPointTypeId) {
        this.fromPointTypeId = fromPointTypeId;
    }

    public Long getToPointTypeId() {
        return toPointTypeId;
    }

    public void setToPointTypeId(Long toPointTypeId) {
        this.toPointTypeId = toPointTypeId;
    }

    public int getFromAmount() {
        return fromAmount;
    }

    public void setFromAmount(int fromAmount) {
        this.fromAmount = fromAmount;
    }

    public int getToAmount() {
        return toAmount;
    }

    public void setToAmount(int toAmount) {
        this.toAmount = toAmount;
    }

    public PointExchangeApprovalMode getApprovalMode() {
        return approvalMode;
    }

    public void setApprovalMode(
            PointExchangeApprovalMode approvalMode
    ) {
        this.approvalMode = approvalMode;
    }
}