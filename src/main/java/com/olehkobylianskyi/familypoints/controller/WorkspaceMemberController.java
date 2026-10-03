package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.dto.*;
import com.olehkobylianskyi.familypoints.service.WorkspaceMemberService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/workspaces/{workspaceId}/members")
public class WorkspaceMemberController {

    private final WorkspaceMemberService workspaceMemberService;

    public WorkspaceMemberController(
            WorkspaceMemberService workspaceMemberService
    ) {
        this.workspaceMemberService = workspaceMemberService;
    }

    @PostMapping
    @PreAuthorize(
            "hasAuthority('MANAGE_MEMBERS') && @workspaceSecurity.canAccessWorkspace(#workspaceId)"
    )
    public ResponseEntity<WorkspaceMemberResponse> createMember(
            @PathVariable Long workspaceId,
            @Valid @RequestBody WorkspaceMemberCreateRequest request
    ) {
        return workspaceMemberService
                .createMember(
                        workspaceId,
                        request.getName(),
                        request.getMemberType(),
                        request.getWorkspaceRoleId()
                )
                .map(WorkspaceMemberResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() ->
                        ResponseEntity.notFound().build()
                );
    }

    @GetMapping
    @PreAuthorize(
            "hasAuthority('VIEW_WORKSPACE') && @workspaceSecurity.canAccessWorkspace(#workspaceId)"
    )
    public List<WorkspaceMemberResponse> getMembers(
            @PathVariable Long workspaceId
    ) {
        return workspaceMemberService
                .getMembers(workspaceId)
                .stream()
                .map(WorkspaceMemberResponse::from)
                .toList();
    }

    @GetMapping("/{memberId}")
    @PreAuthorize(
            "hasAuthority('VIEW_WORKSPACE') && @workspaceSecurity.canAccessWorkspace(#workspaceId)"
    )
    public ResponseEntity<WorkspaceMemberResponse> getMember(
            @PathVariable Long workspaceId,
            @PathVariable Long memberId
    ) {
        return workspaceMemberService
                .getMember(workspaceId, memberId)
                .map(WorkspaceMemberResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() ->
                        ResponseEntity.notFound().build()
                );
    }

    @PutMapping("/{memberId}")
    @PreAuthorize(
            "hasAuthority('MANAGE_MEMBERS') && @workspaceSecurity.canAccessWorkspace(#workspaceId)"
    )
    public ResponseEntity<WorkspaceMemberResponse> updateMember(
            @PathVariable Long workspaceId,
            @PathVariable Long memberId,
            @Valid @RequestBody WorkspaceMemberUpdateRequest request
    ) {
        return workspaceMemberService
                .updateMember(
                        workspaceId,
                        memberId,
                        request.getName(),
                        request.getMemberType(),
                        request.getWorkspaceRoleId()
                )
                .map(WorkspaceMemberResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() ->
                        ResponseEntity.notFound().build()
                );
    }

    @DeleteMapping("/{memberId}")
    @PreAuthorize(
            "hasAuthority('MANAGE_MEMBERS') && @workspaceSecurity.canAccessWorkspace(#workspaceId)"
    )
    public ResponseEntity<Void> deleteMember(
            @PathVariable Long workspaceId,
            @PathVariable Long memberId
    ) {
        if (!workspaceMemberService.deleteMember(
                workspaceId,
                memberId
        )) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }
}