package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.*;
import com.olehkobylianskyi.familypoints.repository.RoleDefinitionRepository;
import com.olehkobylianskyi.familypoints.repository.RoleSetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoleCatalogBootstrapService {
    private final RoleSetRepository roleSets;
    private final RoleDefinitionRepository roles;

    public RoleCatalogBootstrapService(RoleSetRepository roleSets, RoleDefinitionRepository roles) {
        this.roleSets = roleSets;
        this.roles = roles;
    }

    @Transactional
    public void ensureDefaults(Workspace workspace) {
        RoleSet management = ensureSet(workspace, "MANAGEMENT", "Management", "Default management roles");
        RoleSet execution = ensureSet(workspace, "EXECUTION", "Execution", "Default execution roles");
        RoleSet control = ensureSet(workspace, "CONTROL", "Control", "Default control roles");

        ensureRole(workspace, management, "LEADER", "Leader");
        ensureRole(workspace, management, "DEPUTY", "Deputy");
        ensureRole(workspace, management, "SENIOR", "Senior");
        ensureRole(workspace, execution, "EXECUTOR", "Executor");
        ensureRole(workspace, execution, "ASSISTANT", "Assistant");
        ensureRole(workspace, control, "REVIEWER", "Reviewer");
        ensureRole(workspace, control, "OBSERVER", "Observer");
    }

    private RoleSet ensureSet(Workspace workspace, String code, String name, String description) {
        return roleSets.findByWorkspaceIdAndSystemCode(workspace.getId(), code).orElseGet(() -> {
            RoleSet set = new RoleSet(workspace, name, description);
            set.setSystemCode(code);
            set.setVisibility(RoleVisibility.SHARED);
            set.setOwnerContext(RoleContextType.WORKSPACE, null);
            set.setSystemDefault(true);
            return roleSets.save(set);
        });
    }

    private void ensureRole(Workspace workspace, RoleSet set, String code, String name) {
        if (roles.findByWorkspaceIdAndSystemCode(workspace.getId(), code).isPresent()) return;
        RoleDefinition role = new RoleDefinition(workspace, set, name, null, RoleVisibility.SHARED, RoleContextType.WORKSPACE, null);
        role.setSystemCode(code);
        role.setSystemDefault(true);
        roles.save(role);
    }
}
