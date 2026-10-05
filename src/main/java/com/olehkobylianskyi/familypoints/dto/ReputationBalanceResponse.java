package com.olehkobylianskyi.familypoints.dto;

public class ReputationBalanceResponse {
    private final Long memberId;
    private final long balance;

    public ReputationBalanceResponse(Long memberId, long balance) {
        this.memberId = memberId;
        this.balance = balance;
    }

    public Long getMemberId() { return memberId; }
    public long getBalance() { return balance; }
}
