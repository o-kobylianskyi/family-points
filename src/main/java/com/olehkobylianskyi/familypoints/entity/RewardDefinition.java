package com.olehkobylianskyi.familypoints.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "reward_definitions")
public class RewardDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_id", nullable = false)
    private Workspace workspace;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "point_type_id", nullable = false)
    private PointType pointType;

    @Column(name = "price_amount", nullable = false)
    private int priceAmount;

    @Column(name = "minimum_reputation")
    private Integer minimumReputation;

    @Column(name = "requires_approval", nullable = false)
    private boolean requiresApproval;

    @Enumerated(EnumType.STRING)
    @Column(name = "acquisition_mode", nullable = false, length = 30)
    private RewardAcquisitionMode acquisitionMode = RewardAcquisitionMode.DIRECT;

    @ManyToMany
    @JoinTable(
            name = "reward_definition_categories",
            joinColumns = @JoinColumn(name = "reward_definition_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<RewardCategory> categories = new LinkedHashSet<>();

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected RewardDefinition() {}

    public RewardDefinition(
            Workspace workspace,
            String title,
            String description,
            PointType pointType,
            int priceAmount,
            Integer minimumReputation,
            boolean requiresApproval,
            RewardAcquisitionMode acquisitionMode
    ) {
        this.workspace = workspace;
        this.title = title;
        this.description = description;
        this.pointType = pointType;
        this.priceAmount = priceAmount;
        this.minimumReputation = minimumReputation;
        this.requiresApproval = requiresApproval;
        this.acquisitionMode = acquisitionMode == null ? RewardAcquisitionMode.DIRECT : acquisitionMode;
        this.active = true;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Workspace getWorkspace() { return workspace; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public PointType getPointType() { return pointType; }
    public int getPriceAmount() { return priceAmount; }
    public Integer getMinimumReputation() { return minimumReputation; }
    public boolean isRequiresApproval() { return requiresApproval; }
    public RewardAcquisitionMode getAcquisitionMode() { return acquisitionMode; }
    public Set<RewardCategory> getCategories() { return categories; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setPointType(PointType pointType) { this.pointType = pointType; }
    public void setPriceAmount(int priceAmount) { this.priceAmount = priceAmount; }
    public void setMinimumReputation(Integer minimumReputation) { this.minimumReputation = minimumReputation; }
    public void setRequiresApproval(boolean requiresApproval) { this.requiresApproval = requiresApproval; }
    public void setAcquisitionMode(RewardAcquisitionMode acquisitionMode) { this.acquisitionMode = acquisitionMode; }
    public void setCategories(Set<RewardCategory> categories) {
        this.categories.clear();
        if (categories != null) this.categories.addAll(categories);
    }
    public void setActive(boolean active) { this.active = active; }
}
