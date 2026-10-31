package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.dto.LoginResponse;
import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import com.olehkobylianskyi.familypoints.security.JwtService;
import com.olehkobylianskyi.familypoints.security.WorkspaceSessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/** Issue a workspace-scoped access token only after revalidating live membership. */
@RestController
@RequestMapping("/api/me/workspaces")
public class WorkspaceTokenController {
    private final CurrentUserService currentUser;
    private final WorkspaceSessionService sessions;
    private final JwtService jwt;

    public WorkspaceTokenController(CurrentUserService currentUser,
                                    WorkspaceSessionService sessions, JwtService jwt) {
        this.currentUser = currentUser;
        this.sessions = sessions;
        this.jwt = jwt;
    }

    @PostMapping("/{workspaceId}/activate")
    @Transactional(readOnly = true)
    public ResponseEntity<LoginResponse> activate(@PathVariable Long workspaceId,
                                                  Authentication authentication) {
        var account = currentUser.getCurrentAccount();
        sessions.requireSession(account, workspaceId);
        UserDetails principal = (UserDetails) authentication.getPrincipal();
        String token = jwt.generateWorkspaceAccessToken(principal, workspaceId);
        return ResponseEntity.ok(new LoginResponse(
                token, "Bearer", jwt.getAccessTokenExpirationSeconds()));
    }
}
