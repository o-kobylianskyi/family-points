package com.olehkobylianskyi.familypoints.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class PointExchangeCreateRequest {

    @NotNull
    private Long exchangeRateId;

    @Min(1)
    private int fromAmount;

    public PointExchangeCreateRequest() {
    }

    public Long getExchangeRateId() {
        return exchangeRateId;
    }

    public void setExchangeRateId(Long exchangeRateId) {
        this.exchangeRateId = exchangeRateId;
    }

    public int getFromAmount() {
        return fromAmount;
    }

    public void setFromAmount(int fromAmount) {
        this.fromAmount = fromAmount;
    }
}