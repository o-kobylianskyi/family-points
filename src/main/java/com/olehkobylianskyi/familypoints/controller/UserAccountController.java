package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.dto.PasswordResetRequest;
import com.olehkobylianskyi.familypoints.dto.UserAccountCreateRequest;
import com.olehkobylianskyi.familypoints.dto.UserAccountResponse;
import com.olehkobylianskyi.familypoints.dto.UserAccountUpdateRequest;
import com.olehkobylianskyi.familypoints.service.UserAccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/workspaces/{workspaceId}")
public class UserAccountController {

    private final UserAccountService userAccountService;

    public UserAccountController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @GetMapping("/accounts")
    @PreAuthorize("hasAuthority('MANAGE_MEMBERS') && @workspaceSecurity.canAccessWorkspace(#workspaceId)")
    public List<UserAccountResponse> getAccounts(@PathVariable Long workspaceId) {
        return userAccountService.getAccounts(workspaceId)
                .stream()
                .map(UserAccountResponse::from)
                .toList();
    }

    @PostMapping("/members/{memberId}/account")
    @PreAuthorize("hasAuthority('MANAGE_MEMBERS') && @workspaceSecurity.canAccessWorkspace(#workspaceId)")
    public ResponseEntity<UserAccountResponse> createAccount(
            @PathVariable Long workspaceId,
            @PathVariable Long memberId,
            @Valid @RequestBody UserAccountCreateRequest request
    ) {
        return userAccountService.createAccount(workspaceId, memberId, request.getUsername(), request.getPassword())
                .map(UserAccountResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/members/{memberId}/account")
    @PreAuthorize("hasAuthority('MANAGE_MEMBERS') && @workspaceSecurity.canAccessWorkspace(#workspaceId)")
    public ResponseEntity<UserAccountResponse> updateAccount(
            @PathVariable Long workspaceId,
            @PathVariable Long memberId,
            @Valid @RequestBody UserAccountUpdateRequest request
    ) {
        return userAccountService.updateAccount(workspaceId, memberId, request.getUsername(), request.isEnabled())
                .map(UserAccountResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/members/{memberId}/account/password")
    @PreAuthorize("hasAuthority('MANAGE_MEMBERS') && @workspaceSecurity.canAccessWorkspace(#workspaceId)")
    public ResponseEntity<Void> resetPassword(
            @PathVariable Long workspaceId,
            @PathVariable Long memberId,
            @Valid @RequestBody PasswordResetRequest request
    ) {
        return userAccountService.resetPassword(workspaceId, memberId, request.getNewPassword())
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
