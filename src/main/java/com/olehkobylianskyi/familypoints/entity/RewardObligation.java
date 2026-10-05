package com.olehkobylianskyi.familypoints.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "reward_obligations")
public class RewardObligation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private WorkspaceMember member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reward_purchase_id", nullable = false)
    private RewardPurchase rewardPurchase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requirement_id")
    private RewardRequirement requirement;

    @Column(nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RewardObligationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "blocking_mode", nullable = false, length = 30)
    private RewardBlockingMode blockingMode;

    @Column(name = "due_at")
    private LocalDateTime dueAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @ManyToMany
    @JoinTable(
            name = "reward_obligation_blocked_categories",
            joinColumns = @JoinColumn(name = "obligation_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<RewardCategory> blockedCategories = new LinkedHashSet<>();

    @ManyToMany
    @JoinTable(
            name = "reward_obligation_blocked_rewards",
            joinColumns = @JoinColumn(name = "obligation_id"),
            inverseJoinColumns = @JoinColumn(name = "reward_definition_id")
    )
    private Set<RewardDefinition> blockedRewards = new LinkedHashSet<>();

    protected RewardObligation() {}

    public RewardObligation(
            Workspace workspace,
            WorkspaceMember member,
            RewardPurchase rewardPurchase,
            RewardRequirement requirement,
            String title,
            RewardBlockingMode blockingMode,
            LocalDateTime dueAt
    ) {
        this.workspace = workspace;
        this.member = member;
        this.rewardPurchase = rewardPurchase;
        this.requirement = requirement;
        this.title = title;
        this.status = RewardObligationStatus.OPEN;
        this.blockingMode = blockingMode == null ? RewardBlockingMode.NONE : blockingMode;
        this.dueAt = dueAt;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Workspace getWorkspace() { return workspace; }
    public WorkspaceMember getMember() { return member; }
    public RewardPurchase getRewardPurchase() { return rewardPurchase; }
    public RewardRequirement getRequirement() { return requirement; }
    public String getTitle() { return title; }
    public RewardObligationStatus getStatus() { return status; }
    public RewardBlockingMode getBlockingMode() { return blockingMode; }
    public LocalDateTime getDueAt() { return dueAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public Set<RewardCategory> getBlockedCategories() { return blockedCategories; }
    public Set<RewardDefinition> getBlockedRewards() { return blockedRewards; }

    public void complete() {
        this.status = RewardObligationStatus.COMPLETED;
        this.resolvedAt = LocalDateTime.now();
    }
}
