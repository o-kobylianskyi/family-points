package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.dto.*;
import com.olehkobylianskyi.familypoints.service.WorkspaceMemberService;
import com.olehkobylianskyi.familypoints.repository.AccountWorkspaceMembershipRepository;
import com.olehkobylianskyi.familypoints.repository.UserAccountRepository;
import org.springframework.jdbc.core.JdbcTemplate;
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
    private final JdbcTemplate db;

    public WorkspaceMemberController(
            WorkspaceMemberService workspaceMemberService,
            AccountWorkspaceMembershipRepository memberships,
            UserAccountRepository accounts,
            JdbcTemplate db
    ) {
        this.workspaceMemberService = workspaceMemberService;
        this.memberships = memberships;
        this.accounts = accounts;
        this.db = db;
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

        // Resolve workspace-linked accounts and legacy primary accounts without N+1 queries.
        // Contact email and access status are restricted to MANAGE_MEMBERS.
        Map<Long, String> accountEmails = new HashMap<>();
        Map<Long, String> accessStatuses = new HashMap<>();
        for (var membership : memberships.findAllByWorkspaceId(workspaceId)) {
            var account = membership.getAccount();
            Long memberId = membership.getWorkspaceMember().getId();
            accessStatuses.put(memberId, account.isEnabled() ? "ACTIVE" : "DISABLED");
            String email = account.getEmail();
            if (email != null && !email.isBlank()) accountEmails.put(memberId, email);
        }
        for (var account : accounts.findAllByWorkspaceMemberWorkspaceIdOrderByWorkspaceMemberIdAsc(workspaceId)) {
            Long memberId = account.getWorkspaceMember().getId();
            accessStatuses.putIfAbsent(memberId, account.isEnabled() ? "ACTIVE" : "DISABLED");
            String email = account.getEmail();
            if (email != null && !email.isBlank()) accountEmails.putIfAbsent(memberId, email);
        }

        // Only live, unconsumed, non-revoked invitations count as pending.
        // Contact addresses on invitations are not the same as accepted account emails.
        Map<Long, String> pendingEmails = new HashMap<>();
        db.query("""
                SELECT DISTINCT ON (member_id) member_id, invited_email
                FROM member_email_invitations
                WHERE workspace_id = ? AND consumed_at IS NULL AND revoked_at IS NULL
                  AND expires_at > CURRENT_TIMESTAMP
                ORDER BY member_id, created_at DESC, id DESC
                """, rs -> {
                    pendingEmails.put(rs.getLong("member_id"), rs.getString("invited_email"));
                }, workspaceId);

        return members.stream().map(member -> {
            Long id = member.getId();
            boolean hasAccount = accessStatuses.containsKey(id);
            String pendingEmail = hasAccount ? null : pendingEmails.get(id);
            String status = hasAccount ? accessStatuses.get(id)
                    : pendingEmail != null ? "PENDING_INVITATION" : "NO_ACCOUNT";
            return WorkspaceMemberResponse.from(member, accountEmails.get(id), pendingEmail, status);
        }).toList();
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