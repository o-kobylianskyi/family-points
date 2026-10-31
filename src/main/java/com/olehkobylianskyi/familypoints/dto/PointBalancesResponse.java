package com.olehkobylianskyi.familypoints.dto;

import java.util.List;

public class PointBalancesResponse {

    private final Long memberId;
    private final List<PointTypeBalanceResponse> balances;

    public PointBalancesResponse(
            Long memberId,
            List<PointTypeBalanceResponse> balances
    ) {
        this.memberId = memberId;
        this.balances = balances;
    }

    public Long getMemberId() {
        return memberId;
    }

    public List<PointTypeBalanceResponse> getBalances() {
        return balances;
    }
}