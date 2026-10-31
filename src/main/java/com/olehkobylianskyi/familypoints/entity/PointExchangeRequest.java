package com.olehkobylianskyi.familypoints.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "point_exchange_requests")
public class PointExchangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private WorkspaceMember member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exchange_rate_id", nullable = false)
    private PointExchangeRate exchangeRate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_point_type_id", nullable = false)
    private PointType fromPointType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_point_type_id", nullable = false)
    private PointType toPointType;

    @Column(nullable = false)
    private int rateFromAmount;

    @Column(nullable = false)
    private int rateToAmount;

    @Column(nullable = false)
    private int fromAmount;

    @Column(nullable = false)
    private int toAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PointExchangeStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    private LocalDateTime processedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by_member_id")
    private WorkspaceMember processedBy;

    @Column(length = 255)
    private String rejectionReason;

    protected PointExchangeRequest() {
    }

    public PointExchangeRequest(
            WorkspaceMember member,
            PointExchangeRate exchangeRate,
            int rateFromAmount,
            int rateToAmount,
            int fromAmount,
            int toAmount
    ) {
        this.member = member;
        this.exchangeRate = exchangeRate;

        this.fromPointType =
                exchangeRate.getFromPointType();

        this.toPointType =
                exchangeRate.getToPointType();

        this.rateFromAmount = rateFromAmount;
        this.rateToAmount = rateToAmount;

        this.fromAmount = fromAmount;
        this.toAmount = toAmount;

        this.status = PointExchangeStatus.PENDING;
        this.requestedAt = LocalDateTime.now();
    }

    public void approve(WorkspaceMember processedBy) {
        this.status = PointExchangeStatus.APPROVED;
        this.processedBy = processedBy;
        this.processedAt = LocalDateTime.now();
    }

    public void approveAutomatically() {
        this.status = PointExchangeStatus.APPROVED;
        this.processedAt = LocalDateTime.now();
    }

    public void reject(
            WorkspaceMember processedBy,
            String reason
    ) {
        this.status = PointExchangeStatus.REJECTED;
        this.processedBy = processedBy;
        this.rejectionReason = reason;
        this.processedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public WorkspaceMember getMember() {
        return member;
    }

    public PointExchangeRate getExchangeRate() {
        return exchangeRate;
    }

    public PointType getFromPointType() {
        return fromPointType;
    }

    public PointType getToPointType() {
        return toPointType;
    }

    public int getRateFromAmount() {
        return rateFromAmount;
    }

    public int getRateToAmount() {
        return rateToAmount;
    }

    public int getFromAmount() {
        return fromAmount;
    }

    public int getToAmount() {
        return toAmount;
    }

    public PointExchangeStatus getStatus() {
        return status;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public WorkspaceMember getProcessedBy() {
        return processedBy;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }
}