package com.olehkobylianskyi.familypoints.dto;

import com.olehkobylianskyi.familypoints.entity.Workspace;

public class WorkspaceResponse {

    private final Long id;
    private final String name;

    public WorkspaceResponse(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public static WorkspaceResponse from(Workspace workspace) {
        return new WorkspaceResponse(
                workspace.getId(),
                workspace.getName()
        );
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}