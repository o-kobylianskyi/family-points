package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.dto.*;
import com.olehkobylianskyi.familypoints.service.WorkspaceMemberService;
import com.olehkobylianskyi.familypoints.repository.AccountWorkspaceMembershipRepository;
import com.olehkobylianskyi.familypoints.repository.UserAccountRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.HashMap;
import java.util.Map;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/workspaces/{workspaceId}/members")
public class WorkspaceMemberController {

    private final WorkspaceMemberService workspaceMemberService;
    private final AccountWorkspaceMembershipRepository memberships;
    private final UserAccountRepository accounts;

    public WorkspaceMemberController(
            WorkspaceMemberService workspaceMemberService,
            AccountWorkspaceMembershipRepository memberships,
            UserAccountRepository accounts
    ) {
        this.workspaceMemberService = workspaceMemberService;
        this.memberships = memberships;
        this.accounts = accounts;
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
        var members = workspaceMemberService.getMembers(workspaceId);
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        // Email addresses are account contact data. VIEW_WORKSPACE alone must not expose them.
        boolean canManage = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(grant -> "MANAGE_MEMBERS".equals(grant.getAuthority()));
        if (!canManage) {
            return members.stream().map(WorkspaceMemberResponse::from).toList();
        }

        // Fetch account emails in two batches (new membership model + legacy primary members).
        // An outstanding invitation is NOT an account email until the invite is accepted.
        Map<Long, String> accountEmails = new HashMap<>();
        for (var membership : memberships.findAllByWorkspaceId(workspaceId)) {
            var email = membership.getAccount().getEmail();
            if (email != null && !email.isBlank()) {
                accountEmails.put(membership.getWorkspaceMember().getId(), email);
            }
        }
        for (var account : accounts.findAllByWorkspaceMemberWorkspaceIdOrderByWorkspaceMemberIdAsc(workspaceId)) {
            var email = account.getEmail();
            if (email != null && !email.isBlank()) {
                accountEmails.putIfAbsent(account.getWorkspaceMember().getId(), email);
            }
        }
        return members.stream()
                .map(member -> WorkspaceMemberResponse.from(member, accountEmails.get(member.getId())))
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