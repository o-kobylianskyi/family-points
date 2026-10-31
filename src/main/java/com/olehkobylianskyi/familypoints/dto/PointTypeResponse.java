package com.olehkobylianskyi.familypoints.dto;

import com.olehkobylianskyi.familypoints.entity.PointType;

public class PointTypeResponse {

    private final Long id;
    private final String code;
    private final String name;
    private final boolean active;
    private final int sortOrder;

    public PointTypeResponse(
            Long id,
            String code,
            String name,
            boolean active,
            int sortOrder
    ) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.active = active;
        this.sortOrder = sortOrder;
    }

    public static PointTypeResponse from(PointType pointType) {
        return new PointTypeResponse(
                pointType.getId(),
                pointType.getCode(),
                pointType.getName(),
                pointType.isActive(),
                pointType.getSortOrder()
        );
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public boolean isActive() {
        return active;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}