package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.Workspace;
import com.olehkobylianskyi.familypoints.entity.WorkspacePermission;
import com.olehkobylianskyi.familypoints.entity.WorkspaceRole;
import com.olehkobylianskyi.familypoints.exception.ResourceNotFoundException;
import com.olehkobylianskyi.familypoints.repository.WorkspaceRepository;
import com.olehkobylianskyi.familypoints.repository.WorkspaceRoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class WorkspaceRoleService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceRoleRepository workspaceRoleRepository;

    public WorkspaceRoleService(
            WorkspaceRepository workspaceRepository,
            WorkspaceRoleRepository workspaceRoleRepository
    ) {
        this.workspaceRepository = workspaceRepository;
        this.workspaceRoleRepository = workspaceRoleRepository;
    }

    @Transactional
    public void createDefaultRoles(Long workspaceId) {

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Workspace not found: " + workspaceId
                        )
                );

        createIfMissing(
                workspace,
                DefaultWorkspaceRoles.FAMILY_ADMIN,
                "Workspace administrator",
                DefaultWorkspaceRoles.familyAdminPermissions()
        );

        createIfMissing(
                workspace,
                DefaultWorkspaceRoles.PARENT,
                "Parent",
                DefaultWorkspaceRoles.parentPermissions()
        );

        createIfMissing(
                workspace,
                DefaultWorkspaceRoles.CHILD,
                "Child",
                DefaultWorkspaceRoles.childPermissions()
        );
    }

    private void createIfMissing(
            Workspace workspace,
            String code,
            String name,
            Set<WorkspacePermission> permissions
    ) {
        if (workspaceRoleRepository.existsByWorkspaceIdAndCode(
                workspace.getId(),
                code
        )) {
            return;
        }

        WorkspaceRole role = new WorkspaceRole(
                workspace,
                code,
                name,
                true
        );

        role.setPermissions(permissions);

        workspaceRoleRepository.save(role);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceRole> getRoles(Long workspaceId) {

        if (!workspaceRepository.existsById(workspaceId)) {
            throw new ResourceNotFoundException(
                    "Workspace not found: " + workspaceId
            );
        }

        return workspaceRoleRepository.findByWorkspaceIdOrderByIdAsc(
                workspaceId
        );
    }

    @Transactional(readOnly = true)
    public WorkspaceRole getRole(
            Long workspaceId,
            Long roleId
    ) {
        return workspaceRoleRepository
                .findByIdAndWorkspaceId(roleId, workspaceId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Workspace role not found: " + roleId
                        )
                );
    }

    @Transactional(readOnly = true)
    public WorkspaceRole getRoleByCode(
            Long workspaceId,
            String code
    ) {
        return workspaceRoleRepository
                .findByWorkspaceIdAndCode(workspaceId, code)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Workspace role not found: " + code
                        )
                );
    }
}