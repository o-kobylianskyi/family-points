package com.olehkobylianskyi.familypoints.dto;

import com.olehkobylianskyi.familypoints.entity.PointTransaction;
import com.olehkobylianskyi.familypoints.entity.PointTransactionSourceType;
import com.olehkobylianskyi.familypoints.entity.PointTransactionType;

import java.time.LocalDateTime;

public class PointTransactionResponse {

    private final Long id;
    private final Long memberId;

    private final String pointTypeCode;
    private final String pointTypeName;

    private final int amount;

    private final PointTransactionType type;
    private final PointTransactionSourceType sourceType;

    private final Long sourceId;

    private final String description;
    private final LocalDateTime createdAt;

    public PointTransactionResponse(
            Long id,
            Long memberId,
            String pointTypeCode,
            String pointTypeName,
            int amount,
            PointTransactionType type,
            PointTransactionSourceType sourceType,
            Long sourceId,
            String description,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.memberId = memberId;
        this.pointTypeCode = pointTypeCode;
        this.pointTypeName = pointTypeName;
        this.amount = amount;
        this.type = type;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.description = description;
        this.createdAt = createdAt;
    }

    public static PointTransactionResponse from(
            PointTransaction transaction
    ) {
        return new PointTransactionResponse(
                transaction.getId(),
                transaction.getMember().getId(),

                transaction.getPointType() == null
                        ? null
                        : transaction.getPointType().getCode(),

                transaction.getPointType() == null
                        ? null
                        : transaction.getPointType().getName(),

                transaction.getAmount(),
                transaction.getType(),
                transaction.getSourceType(),
                transaction.getSourceId(),
                transaction.getDescription(),
                transaction.getCreatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public Long getMemberId() {
        return memberId;
    }

    public String getPointTypeCode() {
        return pointTypeCode;
    }

    public String getPointTypeName() {
        return pointTypeName;
    }

    public int getAmount() {
        return amount;
    }

    public PointTransactionType getType() {
        return type;
    }

    public PointTransactionSourceType getSourceType() {
        return sourceType;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}