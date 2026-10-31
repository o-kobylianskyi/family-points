package com.olehkobylianskyi.familypoints.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class PointOperationTypedRequest {
    @NotNull
    private Long pointTypeId;
    @Min(1) @Max(100000)
    private int amount;
    @Size(max = 255)
    private String description;

    public Long getPointTypeId() { return pointTypeId; }
    public void setPointTypeId(Long value) { pointTypeId = value; }
    public int getAmount() { return amount; }
    public void setAmount(int value) { amount = value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { description = value; }
}
