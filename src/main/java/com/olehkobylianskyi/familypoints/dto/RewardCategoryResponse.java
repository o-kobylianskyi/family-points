package com.olehkobylianskyi.familypoints.dto;

import com.olehkobylianskyi.familypoints.entity.RewardCategory;

public class RewardCategoryResponse {
    private final Long id;
    private final String name;
    private final boolean active;
    private final int sortOrder;

    public RewardCategoryResponse(RewardCategory category) {
        this.id = category.getId();
        this.name = category.getName();
        this.active = category.isActive();
        this.sortOrder = category.getSortOrder();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
    public int getSortOrder() { return sortOrder; }
}
