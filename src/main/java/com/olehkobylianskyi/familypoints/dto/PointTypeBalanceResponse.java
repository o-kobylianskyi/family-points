package com.olehkobylianskyi.familypoints.dto;

public class PointTypeBalanceResponse {

    private final Long pointTypeId;
    private final String code;
    private final String name;
    private final long balance;

    public PointTypeBalanceResponse(
            Long pointTypeId,
            String code,
            String name,
            long balance
    ) {
        this.pointTypeId = pointTypeId;
        this.code = code;
        this.name = name;
        this.balance = balance;
    }

    public Long getPointTypeId() {
        return pointTypeId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public long getBalance() {
        return balance;
    }
}