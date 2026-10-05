package com.olehkobylianskyi.familypoints.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "reward_categories",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_reward_category_family_name",
                columnNames = {"family_id", "name"}
        )
)
public class RewardCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_id", nullable = false)
    private Workspace workspace;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected RewardCategory() {}

    public RewardCategory(Workspace workspace, String name, int sortOrder) {
        this.workspace = workspace;
        this.name = name;
        this.sortOrder = sortOrder;
        this.active = true;
    }

    public Long getId() { return id; }
    public Workspace getWorkspace() { return workspace; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
    public int getSortOrder() { return sortOrder; }

    public void setName(String name) { this.name = name; }
    public void setActive(boolean active) { this.active = active; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
