package com.olehkobylianskyi.familypoints.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public class OfflinePointOperationRequest {
    @NotNull
    private UUID operationId;
    // Nullable only for pending entries created by older Android releases.
    private Long pointTypeId;
    @Min(1)
    @Max(100000)
    private int amount;
    @Size(max = 255)
    private String description;

    public Long getPointTypeId() { return pointTypeId; }
    public void setPointTypeId(Long pointTypeId) { this.pointTypeId = pointTypeId; }
    public UUID getOperationId() { return operationId; }
    public void setOperationId(UUID operationId) { this.operationId = operationId; }
    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
