package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.dto.WorkspaceRoleResponse;
import com.olehkobylianskyi.familypoints.service.WorkspaceRoleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/workspaces/{workspaceId}/roles")
public class WorkspaceRoleController {

    private final WorkspaceRoleService workspaceRoleService;

    public WorkspaceRoleController(WorkspaceRoleService workspaceRoleService) {
        this.workspaceRoleService = workspaceRoleService;
    }

    @PostMapping("/defaults")
    public ResponseEntity<Void> createDefaultRoles(
            @PathVariable Long workspaceId
    ) {
        workspaceRoleService.createDefaultRoles(workspaceId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<WorkspaceRoleResponse> getRoles(
            @PathVariable Long workspaceId
    ) {
        return workspaceRoleService.getRoles(workspaceId)
                .stream()
                .map(WorkspaceRoleResponse::from)
                .toList();
    }

    @GetMapping("/{roleId}")
    public WorkspaceRoleResponse getRole(
            @PathVariable Long workspaceId,
            @PathVariable Long roleId
    ) {
        return WorkspaceRoleResponse.from(
                workspaceRoleService.getRole(workspaceId, roleId)
        );
    }
}