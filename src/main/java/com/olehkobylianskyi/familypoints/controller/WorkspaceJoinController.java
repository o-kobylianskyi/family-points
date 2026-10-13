package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.service.WorkspaceJoinService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workspace-joins")
public class WorkspaceJoinController {
    private final WorkspaceJoinService service;

    public WorkspaceJoinController(WorkspaceJoinService service) {
        this.service = service;
    }

    public record JoinRequest(@NotBlank String code) {}
    public record JoinCode(String code) {}
    public record ApproveRequest(@jakarta.validation.constraints.NotNull Long roleId,
                                 @NotBlank String memberName) {}

    @PostMapping("/requests")
    @PreAuthorize("isAuthenticated()")
    public Map<String, String> request(@Valid @RequestBody JoinRequest body) {
        service.requestJoin(body.code());
        return Map.of("status", "PENDING");
    }

    @PostMapping("/workspaces/{workspaceId}/join-code")
    @PreAuthorize("@workspaceSecurity.canManageMembers(#workspaceId)")
    public JoinCode rotate(@PathVariable Long workspaceId) {
        return new JoinCode(service.rotateJoinCode(workspaceId));
    }

    @DeleteMapping("/workspaces/{workspaceId}/join-code")
    @PreAuthorize("@workspaceSecurity.canManageMembers(#workspaceId)")
    public void disable(@PathVariable Long workspaceId) {
        service.disableJoinCode(workspaceId);
    }

    @GetMapping("/workspaces/{workspaceId}/requests")
    @PreAuthorize("@workspaceSecurity.canManageMembers(#workspaceId)")
    public List<Map<String, Object>> pending(@PathVariable Long workspaceId) {
        return service.pendingForManager(workspaceId);
    }

    @PostMapping("/workspaces/{workspaceId}/requests/{requestId}/approve")
    @PreAuthorize("@workspaceSecurity.canManageMembers(#workspaceId)")
    public Map<String, String> approve(@PathVariable Long workspaceId,
                                       @PathVariable Long requestId,
                                       @Valid @RequestBody ApproveRequest body) {
        service.approveRequest(workspaceId, requestId, body.roleId(), body.memberName());
        return Map.of("status", "APPROVED");
    }

    @PostMapping("/workspaces/{workspaceId}/requests/{requestId}/reject")
    @PreAuthorize("@workspaceSecurity.canManageMembers(#workspaceId)")
    public void reject(@PathVariable Long workspaceId, @PathVariable Long requestId) {
        service.rejectRequest(workspaceId, requestId);
    }
}
