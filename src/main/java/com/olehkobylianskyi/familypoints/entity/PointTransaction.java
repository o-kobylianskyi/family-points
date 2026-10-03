package com.olehkobylianskyi.familypoints.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "point_transactions")
public class PointTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private WorkspaceMember member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "point_type_id", nullable = false)
    private PointType pointType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PointTransactionSourceType sourceType;

    @Column(nullable = false)
    private int amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PointTransactionType type;

    @Column
    private Long sourceId;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected PointTransaction() {
    }

    public PointTransaction(
            WorkspaceMember member,
            PointType pointType,
            int amount,
            PointTransactionType type,
            PointTransactionSourceType sourceType,
            Long sourceId,
            String description
    ) {
        this.member = member;
        this.pointType = pointType;
        this.amount = amount;
        this.type = type;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public WorkspaceMember getMember() {
        return member;
    }

    public PointType getPointType() {
        return pointType;
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