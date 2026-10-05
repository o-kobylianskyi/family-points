package com.olehkobylianskyi.familypoints.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reputation_transactions")
public class ReputationTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private WorkspaceMember member;

    @Column(nullable = false)
    private int amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 40)
    private ReputationTransactionSourceType sourceType;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(length = 255)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected ReputationTransaction() {}

    public ReputationTransaction(
            WorkspaceMember member,
            int amount,
            ReputationTransactionSourceType sourceType,
            Long sourceId,
            String description
    ) {
        this.member = member;
        this.amount = amount;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public WorkspaceMember getMember() { return member; }
    public int getAmount() { return amount; }
    public ReputationTransactionSourceType getSourceType() { return sourceType; }
    public Long getSourceId() { return sourceId; }
    public String getDescription() { return description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
