package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.dto.RoleSetResponse;
import com.olehkobylianskyi.familypoints.entity.RoleSet;
import com.olehkobylianskyi.familypoints.entity.Workspace;
import com.olehkobylianskyi.familypoints.exception.ResourceNotFoundException;
import com.olehkobylianskyi.familypoints.repository.GroupRoleRepository;
import com.olehkobylianskyi.familypoints.repository.RoleSetRepository;
import com.olehkobylianskyi.familypoints.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class RoleSetService {
    private final WorkspaceRepository workspaces;
    private final RoleSetRepository roleSets;
    private final GroupRoleRepository roles;
    private final RoleCatalogAuthorizationService authorization;

    public RoleSetService(WorkspaceRepository workspaces, RoleSetRepository roleSets, GroupRoleRepository roles,
                          RoleCatalogAuthorizationService authorization) {
        this.workspaces = workspaces;
        this.roleSets = roleSets;
        this.roles = roles;
        this.authorization = authorization;
    }

    @Transactional(readOnly = true)
    public List<RoleSetResponse> list(Long workspaceId) {
        authorization.requireWorkspace(workspaceId);
        workspace(workspaceId);
        return roleSets.findByWorkspaceIdAndActiveTrueOrderByNameAsc(workspaceId).stream()
                .map(set -> RoleSetResponse.from(set, roles.findByRoleSetIdOrderByNameAsc(set.getId())))
                .toList();
    }

    @Transactional
    public RoleSetResponse create(Long workspaceId, String name, String description) {
        authorization.requireManage(workspaceId);
        Workspace workspace = workspace(workspaceId);
        if (roleSets.existsByWorkspaceIdAndNameIgnoreCase(workspaceId, name.trim())) {
            throw new IllegalArgumentException("Role set already exists: " + name);
        }
        RoleSet set = roleSets.save(new RoleSet(workspace, name.trim(), blank(description)));
        return RoleSetResponse.from(set, List.of());
    }

    @Transactional
    public RoleSetResponse update(Long workspaceId, Long id, String name, String description) {
        authorization.requireManage(workspaceId);
        RoleSet set = roleSet(workspaceId, id);
        if (set.isSystemDefault()) throw new IllegalArgumentException("System role sets cannot be edited");
        set.setName(name.trim());
        set.setDescription(blank(description));
        return RoleSetResponse.from(roleSets.save(set), roles.findByRoleSetIdOrderByNameAsc(id));
    }

    @Transactional
    public void deactivate(Long workspaceId, Long id) {
        authorization.requireManage(workspaceId);
        RoleSet set = roleSet(workspaceId, id);
        if (set.isSystemDefault()) throw new IllegalArgumentException("System role sets cannot be deleted");
        roles.findByRoleSetIdOrderByNameAsc(id).forEach(role -> role.setRoleSet(null));
        set.setActive(false);
        roleSets.save(set);
    }

    private RoleSet roleSet(Long workspaceId, Long id) {
        return roleSets.findByIdAndWorkspaceId(id, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Role set not found: " + id));
    }

    private Workspace workspace(Long id) {
        return workspaces.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found: " + id));
    }

    private String blank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
