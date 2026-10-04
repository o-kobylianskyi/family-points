package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.dto.RoleDefinitionRequest;
import com.olehkobylianskyi.familypoints.dto.RoleDefinitionResponse;
import com.olehkobylianskyi.familypoints.service.RoleDefinitionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/workspaces/{workspaceId}/role-definitions")
public class RoleDefinitionController {
    private final RoleDefinitionService service;

    public RoleDefinitionController(RoleDefinitionService service) {
        this.service = service;
    }

    @GetMapping
    public List<RoleDefinitionResponse> list(@PathVariable Long workspaceId) {
        return service.list(workspaceId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoleDefinitionResponse create(
            @PathVariable Long workspaceId,
            @Valid @RequestBody RoleDefinitionRequest request
    ) {
        return service.create(workspaceId, request);
    }

    @PutMapping("/{roleId}")
    public RoleDefinitionResponse update(
            @PathVariable Long workspaceId,
            @PathVariable Long roleId,
            @Valid @RequestBody RoleDefinitionRequest request
    ) {
        return service.update(workspaceId, roleId, request);
    }

    @DeleteMapping("/{roleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long workspaceId, @PathVariable Long roleId) {
        service.deactivate(workspaceId, roleId);
    }
}
