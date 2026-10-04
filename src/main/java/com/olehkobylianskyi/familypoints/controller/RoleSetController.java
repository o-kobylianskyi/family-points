package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.dto.RoleSetRequest;
import com.olehkobylianskyi.familypoints.dto.RoleSetResponse;
import com.olehkobylianskyi.familypoints.service.RoleSetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/workspaces/{workspaceId}/role-sets")
public class RoleSetController {
    private final RoleSetService service;

    public RoleSetController(RoleSetService service) {
        this.service = service;
    }

    @GetMapping
    public List<RoleSetResponse> list(@PathVariable Long workspaceId) {
        return service.list(workspaceId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoleSetResponse create(@PathVariable Long workspaceId, @Valid @RequestBody RoleSetRequest request) {
        return service.create(workspaceId, request.getName(), request.getDescription());
    }

    @PutMapping("/{roleSetId}")
    public RoleSetResponse update(@PathVariable Long workspaceId, @PathVariable Long roleSetId, @Valid @RequestBody RoleSetRequest request) {
        return service.update(workspaceId, roleSetId, request.getName(), request.getDescription());
    }

    @DeleteMapping("/{roleSetId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long workspaceId, @PathVariable Long roleSetId) {
        service.deactivate(workspaceId, roleSetId);
    }
}
