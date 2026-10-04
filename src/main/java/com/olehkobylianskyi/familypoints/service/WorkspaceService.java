package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.Workspace;
import com.olehkobylianskyi.familypoints.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final RoleCatalogBootstrapService roleCatalogBootstrapService;

    public WorkspaceService(WorkspaceRepository workspaceRepository, RoleCatalogBootstrapService roleCatalogBootstrapService) {
        this.workspaceRepository = workspaceRepository;
        this.roleCatalogBootstrapService = roleCatalogBootstrapService;
    }

    public Workspace createWorkspace(String name) {
        Workspace workspace = workspaceRepository.save(new Workspace(name));
        roleCatalogBootstrapService.ensureDefaults(workspace);
        return workspace;
    }

    public List<Workspace> getAllWorkspaces() {
        return workspaceRepository.findAll();
    }

    public Optional<Workspace> getWorkspaceById(Long id) {
        return workspaceRepository.findById(id);
    }

    public Optional<Workspace> updateWorkspace(Long id, String name) {
        return workspaceRepository.findById(id)
                .map(workspace -> {
                    workspace.setName(name);
                    return workspaceRepository.save(workspace);
                });
    }

    public boolean deleteWorkspace(Long id) {
        if (!workspaceRepository.existsById(id)) {
            return false;
        }

        workspaceRepository.deleteById(id);
        return true;
    }
}