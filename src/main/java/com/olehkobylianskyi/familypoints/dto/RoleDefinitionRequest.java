package com.olehkobylianskyi.familypoints.dto;

import com.olehkobylianskyi.familypoints.entity.RoleContextType;
import com.olehkobylianskyi.familypoints.entity.RoleVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RoleDefinitionRequest {
    @NotBlank @Size(max = 120)
    private String name;

    @Size(max = 500)
    private String description;

    private Long roleSetId;
    private RoleVisibility visibility = RoleVisibility.SHARED;
    private RoleContextType ownerContextType = RoleContextType.WORKSPACE;
    private Long ownerContextId;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getRoleSetId() { return roleSetId; }
    public void setRoleSetId(Long roleSetId) { this.roleSetId = roleSetId; }
    public RoleVisibility getVisibility() { return visibility; }
    public void setVisibility(RoleVisibility visibility) { this.visibility = visibility; }
    public RoleContextType getOwnerContextType() { return ownerContextType; }
    public void setOwnerContextType(RoleContextType ownerContextType) { this.ownerContextType = ownerContextType; }
    public Long getOwnerContextId() { return ownerContextId; }
    public void setOwnerContextId(Long ownerContextId) { this.ownerContextId = ownerContextId; }
}
