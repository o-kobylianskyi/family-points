package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.*;
import com.olehkobylianskyi.familypoints.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Workspace initialization for the signup wizard.
 * This service does not grant an account access or expose a public registration route.
 */
@Service
public class FamilyWorkspaceProvisioningService {
    private final WorkspaceRepository workspaces;
    private final WorkspaceRoleService roles;
    private final WorkspaceRoleRepository roleRepository;
    private final WorkspaceMemberRepository members;
    private final EconomyService economy;

    public FamilyWorkspaceProvisioningService(WorkspaceRepository workspaces,
            WorkspaceRoleService roles, WorkspaceRoleRepository roleRepository,
            WorkspaceMemberRepository members, EconomyService economy) {
        this.workspaces = workspaces;
        this.roles = roles;
        this.roleRepository = roleRepository;
        this.members = members;
        this.economy = economy;
    }

    public record ProvisionedFamily(Long workspaceId, Long ownerMemberId) {}

    /** Call only from an authorized, transactional registration or create-family operation. */
    @Transactional
    public ProvisionedFamily provision(String familyName, String ownerName) {
        if (familyName == null || familyName.isBlank() || familyName.length() > 100
                || ownerName == null || ownerName.isBlank() || ownerName.length() > 100) {
            throw new IllegalArgumentException("Invalid family or administrator name");
        }
        Workspace workspace = workspaces.save(new Workspace(familyName.trim()));
        roles.createDefaultRoles(workspace.getId());
        WorkspaceRole admin = roleRepository.findByWorkspaceIdAndCode(
                workspace.getId(), DefaultWorkspaceRoles.FAMILY_ADMIN).orElseThrow();
        WorkspaceMember owner = members.save(new WorkspaceMember(
                ownerName.trim(), WorkspaceMemberType.PARENT, admin, workspace));
        economy.getOrCreateSettings(workspace.getId());
        return new ProvisionedFamily(workspace.getId(), owner.getId());
    }
}
