package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.service.WorkNodeGroupService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** TASK folder groups; not member groups or catalog categories. */
@RestController
@RequestMapping("/workspaces/{workspaceId}/work-node-groups")
@PreAuthorize("@workspaceSecurity.canAccessWorkspace(#workspaceId)")
public class WorkNodeGroupController {
    private final WorkNodeGroupService groups;
    public WorkNodeGroupController(WorkNodeGroupService groups) { this.groups=groups; }

    @GetMapping
    public List<WorkNodeGroupService.Group> list(@PathVariable Long workspaceId) {
        return groups.list(workspaceId);
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@workspaceSecurity.canManageWorkspace(#workspaceId) and hasAuthority('CREATE_TASKS')")
    public WorkNodeGroupService.Group create(@PathVariable Long workspaceId,
            @RequestBody WorkNodeGroupService.Input request) {
        return groups.create(workspaceId,request);
    }
    @PutMapping("/{groupId}")
    @PreAuthorize("@workspaceSecurity.canManageWorkspace(#workspaceId) and hasAuthority('CREATE_TASKS')")
    public WorkNodeGroupService.Group update(@PathVariable Long workspaceId,
            @PathVariable Long groupId,@RequestBody WorkNodeGroupService.Input request) {
        return groups.update(workspaceId,groupId,request);
    }
    @DeleteMapping("/{groupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@workspaceSecurity.canManageWorkspace(#workspaceId) and hasAuthority('CREATE_TASKS')")
    public void deactivate(@PathVariable Long workspaceId,@PathVariable Long groupId) {
        groups.deactivate(workspaceId,groupId);
    }
}
