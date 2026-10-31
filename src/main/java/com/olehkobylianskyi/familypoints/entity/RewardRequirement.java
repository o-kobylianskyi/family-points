package com.olehkobylianskyi.familypoints.entity;

import jakarta.persistence.*;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "reward_requirements")
public class RewardRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reward_definition_id")
    private RewardDefinition rewardDefinition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reward_request_id")
    private RewardRequest rewardRequest;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RewardRequirementPhase phase;

    @Enumerated(EnumType.STRING)
    @Column(name = "requirement_type", nullable = false, length = 40)
    private RewardRequirementType requirementType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_definition_id")
    private TaskDefinition taskDefinition;

    @Enumerated(EnumType.STRING)
    @Column(name = "time_scope", length = 30)
    private RewardRequirementTimeScope timeScope;

    @Column(name = "window_value")
    private Integer windowValue;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private boolean required = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "blocking_mode", nullable = false, length = 30)
    private RewardBlockingMode blockingMode = RewardBlockingMode.NONE;

    @ManyToMany
    @JoinTable(
            name = "reward_requirement_blocked_categories",
            joinColumns = @JoinColumn(name = "requirement_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<RewardCategory> blockedCategories = new LinkedHashSet<>();

    @ManyToMany
    @JoinTable(
            name = "reward_requirement_blocked_rewards",
            joinColumns = @JoinColumn(name = "requirement_id"),
            inverseJoinColumns = @JoinColumn(name = "reward_definition_id")
    )
    private Set<RewardDefinition> blockedRewards = new LinkedHashSet<>();

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected RewardRequirement() {}

    public RewardRequirement(
            Workspace workspace,
            RewardDefinition rewardDefinition,
            RewardRequest rewardRequest,
            RewardRequirementPhase phase,
            RewardRequirementType requirementType,
            TaskDefinition taskDefinition,
            RewardRequirementTimeScope timeScope,
            Integer windowValue,
            String description,
            boolean required,
            RewardBlockingMode blockingMode,
            int sortOrder
    ) {
        this.workspace = workspace;
        this.rewardDefinition = rewardDefinition;
        this.rewardRequest = rewardRequest;
        this.phase = phase;
        this.requirementType = requirementType;
        this.taskDefinition = taskDefinition;
        this.timeScope = timeScope;
        this.windowValue = windowValue;
        this.description = description;
        this.required = required;
        this.blockingMode = blockingMode == null ? RewardBlockingMode.NONE : blockingMode;
        this.sortOrder = sortOrder;
    }

    public Long getId() { return id; }
    public Workspace getWorkspace() { return workspace; }
    public RewardDefinition getRewardDefinition() { return rewardDefinition; }
    public RewardRequest getRewardRequest() { return rewardRequest; }
    public RewardRequirementPhase getPhase() { return phase; }
    public RewardRequirementType getRequirementType() { return requirementType; }
    public TaskDefinition getTaskDefinition() { return taskDefinition; }
    public RewardRequirementTimeScope getTimeScope() { return timeScope; }
    public Integer getWindowValue() { return windowValue; }
    public String getDescription() { return description; }
    public boolean isRequired() { return required; }
    public RewardBlockingMode getBlockingMode() { return blockingMode; }
    public Set<RewardCategory> getBlockedCategories() { return blockedCategories; }
    public Set<RewardDefinition> getBlockedRewards() { return blockedRewards; }
    public int getSortOrder() { return sortOrder; }
}
