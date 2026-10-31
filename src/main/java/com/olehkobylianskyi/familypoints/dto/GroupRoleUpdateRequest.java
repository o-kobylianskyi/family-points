package com.olehkobylianskyi.familypoints.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class GroupRoleUpdateRequest {
    @NotBlank @Size(max = 100) private String name;
    @Size(max = 500) private String description;
    private Long roleSetId;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getRoleSetId() { return roleSetId; }
    public void setRoleSetId(Long roleSetId) { this.roleSetId = roleSetId; }
}
