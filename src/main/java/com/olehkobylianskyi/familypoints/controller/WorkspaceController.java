package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.dto.*;
import com.olehkobylianskyi.familypoints.entity.Workspace;
import com.olehkobylianskyi.familypoints.service.WorkspaceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/workspaces")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @PostMapping
    public WorkspaceResponse createWorkspace(
            @Valid @RequestBody WorkspaceCreateRequest request
    ) {
        Workspace workspace =
                workspaceService.createWorkspace(request.getName());

        return WorkspaceResponse.from(workspace);
    }

    @GetMapping
    public List<WorkspaceResponse> getAllWorkspaces() {
        return workspaceService.getAllWorkspaces()
                .stream()
                .map(WorkspaceResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkspaceResponse> getWorkspace(
            @PathVariable Long id
    ) {
        return workspaceService.getWorkspaceById(id)
                .map(WorkspaceResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() ->
                        ResponseEntity.notFound().build()
                );
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkspaceResponse> updateWorkspace(
            @PathVariable Long id,
            @Valid @RequestBody WorkspaceUpdateRequest request
    ) {
        return workspaceService
                .updateWorkspace(id, request.getName())
                .map(WorkspaceResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() ->
                        ResponseEntity.notFound().build()
                );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorkspace(
            @PathVariable Long id
    ) {
        if (!workspaceService.deleteWorkspace(id)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }
}