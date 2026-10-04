package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.dto.RoleDefinitionRequest;
import com.olehkobylianskyi.familypoints.dto.RoleDefinitionResponse;
import com.olehkobylianskyi.familypoints.entity.*;
import com.olehkobylianskyi.familypoints.exception.ResourceNotFoundException;
import com.olehkobylianskyi.familypoints.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoleDefinitionService {
    private final WorkspaceRepository workspaces;
    private final RoleDefinitionRepository roles;
    private final RoleSetRepository roleSets;
    private final MemberGroupRepository groups;

    public RoleDefinitionService(
            WorkspaceRepository workspaces,
            RoleDefinitionRepository roles,
            RoleSetRepository roleSets,
            MemberGroupRepository groups
    ) {
        this.workspaces = workspaces;
        this.roles = roles;
        this.roleSets = roleSets;
        this.groups = groups;
    }

    @Transactional(readOnly = true)
    public List<RoleDefinitionResponse> list(Long workspaceId) {
        workspace(workspaceId);
        return roles.findByWorkspaceIdAndActiveTrueOrderByNameAsc(workspaceId)
                .stream().map(RoleDefinitionResponse::from).toList();
    }

    @Transactional
    public RoleDefinitionResponse create(Long workspaceId, RoleDefinitionRequest request) {
        Workspace workspace = workspace(workspaceId);
        RoleContextType contextType = request.getOwnerContextType() == null
                ? RoleContextType.WORKSPACE : request.getOwnerContextType();
        Long contextId = normalizedContextId(workspaceId, contextType, request.getOwnerContextId());

        RoleDefinition role = new RoleDefinition(
                workspace,
                roleSet(workspaceId, request.getRoleSetId()),
                request.getName().trim(),
                blank(request.getDescription()),
                request.getVisibility() == null ? RoleVisibility.SHARED : request.getVisibility(),
                contextType,
                contextId
        );
        return RoleDefinitionResponse.from(roles.save(role));
    }

    @Transactional
    public RoleDefinitionResponse update(Long workspaceId, Long roleId, RoleDefinitionRequest request) {
        RoleDefinition role = role(workspaceId, roleId);
        if (role.isSystemDefault()) {
            throw new IllegalArgumentException("System role definitions cannot be edited");
        }

        RoleContextType contextType = request.getOwnerContextType() == null
                ? role.getOwnerContextType() : request.getOwnerContextType();
        Long contextId = normalizedContextId(workspaceId, contextType, request.getOwnerContextId());

        role.setName(request.getName().trim());
        role.setDescription(blank(request.getDescription()));
        role.setRoleSet(roleSet(workspaceId, request.getRoleSetId()));
        role.setVisibility(request.getVisibility() == null ? role.getVisibility() : request.getVisibility());
        role.setOwnerContext(contextType, contextId);

        return RoleDefinitionResponse.from(roles.save(role));
    }

    @Transactional
    public void deactivate(Long workspaceId, Long roleId) {
        RoleDefinition role = role(workspaceId, roleId);
        if (role.isSystemDefault()) {
            throw new IllegalArgumentException("System role definitions cannot be deleted");
        }
        role.setActive(false);
        roles.save(role);
    }

    private RoleSet roleSet(Long workspaceId, Long roleSetId) {
        if (roleSetId == null) return null;
        return roleSets.findByIdAndWorkspaceId(roleSetId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Role set not found: " + roleSetId));
    }

    private Long normalizedContextId(Long workspaceId, RoleContextType type, Long id) {
        if (type == RoleContextType.WORKSPACE) return null;
        if (id == null) throw new IllegalArgumentException("Context id is required for " + type);
        if (type == RoleContextType.MEMBER_GROUP) {
            groups.findByIdAndWorkspaceId(id, workspaceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Member group not found: " + id));
        }
        return id;
    }

    private RoleDefinition role(Long workspaceId, Long roleId) {
        return roles.findByIdAndWorkspaceId(roleId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Role definition not found: " + roleId));
    }

    private Workspace workspace(Long id) {
        return workspaces.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found: " + id));
    }

    private String blank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
