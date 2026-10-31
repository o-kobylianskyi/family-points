package com.olehkobylianskyi.familypoints.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class PointOperationRequest {

    @Min(1)
    @Max(100000)
    private int amount;

    @Size(max = 255)
    private String description;

    public PointOperationRequest() {
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}