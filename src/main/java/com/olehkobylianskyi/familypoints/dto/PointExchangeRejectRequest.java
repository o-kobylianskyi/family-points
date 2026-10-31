package com.olehkobylianskyi.familypoints.dto;

import jakarta.validation.constraints.Size;

public class PointExchangeRejectRequest {

    @Size(max = 255)
    private String reason;

    public PointExchangeRejectRequest() {
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}