package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

@Service
public class WorkspaceInvitationService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final JdbcTemplate jdbc;
    private final CurrentUserService user;

    public WorkspaceInvitationService(JdbcTemplate jdbc, CurrentUserService user) {
        this.jdbc = jdbc;
        this.user = user;
    }

    public record Invitation(String token, OffsetDateTime expiresAt) {}
    public record Preview(String workspaceName, OffsetDateTime expiresAt) {}

    private static String token() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Transactional
    public Invitation issue(Long workspaceId) {
        String secret = token();
        OffsetDateTime expires = OffsetDateTime.now().plusDays(7);
        jdbc.update("""
                INSERT INTO workspace_invitations
                  (workspace_id, created_by_account_id, token_hash, expires_at)
                VALUES (?, ?, ?, ?)
                """, workspaceId, user.getCurrentAccount().getId(), hash(secret), expires);
        return new Invitation(secret, expires);
    }

    @Transactional(readOnly = true)
    public Preview preview(String secret) {
        if (secret == null || !secret.matches("[A-Za-z0-9_-]{43}")) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Invitation unavailable");
        }
        List<Preview> found = jdbc.query("""
                SELECT f.name, i.expires_at
                FROM workspace_invitations i JOIN families f ON f.id=i.workspace_id
                WHERE i.token_hash=? AND i.revoked_at IS NULL
                  AND i.expires_at>CURRENT_TIMESTAMP AND i.use_count<i.max_uses
                """, (rs, n) -> new Preview(rs.getString(1),
                        rs.getObject(2, OffsetDateTime.class)), hash(secret));
        if (found.size()!=1) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Invitation unavailable");
        }
        return found.get(0);
    }

    public record PendingContext(String handle, OffsetDateTime expiresAt) {}

    /**
     * Exchanges the one-use invitation secret for an opaque short-lived handle.
     * Does not consume the invitation or create a workspace membership.
     */
    @Transactional
    public PendingContext begin(String invitationToken) {
        preview(invitationToken);
        String handle = token();
        OffsetDateTime expires = OffsetDateTime.now().plusHours(2);
        int count = jdbc.update("""
                INSERT INTO pending_invitation_sessions(handle_hash, invitation_id, expires_at)
                SELECT ?, i.id, ? FROM workspace_invitations i
                WHERE i.token_hash=? AND i.revoked_at IS NULL
                  AND i.expires_at>CURRENT_TIMESTAMP AND i.use_count<i.max_uses
                """, hash(handle), expires, hash(invitationToken));
        if (count != 1) throw unavailable();
        return new PendingContext(handle, expires);
    }

    /** Validates both the stored session and the underlying invitation on resume. */
    @Transactional(readOnly = true)
    public Preview resume(String handle) {
        if (handle == null || !handle.matches("[A-Za-z0-9_-]{43}")) throw unavailable();
        List<Preview> found = jdbc.query("""
                SELECT f.name, i.expires_at
                FROM pending_invitation_sessions p
                JOIN workspace_invitations i ON p.invitation_id=i.id
                JOIN families f ON i.workspace_id=f.id
                WHERE p.handle_hash=? AND p.consumed_at IS NULL
                  AND p.expires_at>CURRENT_TIMESTAMP AND i.revoked_at IS NULL
                  AND i.expires_at>CURRENT_TIMESTAMP AND i.use_count<i.max_uses
                """, (rs, n) -> new Preview(rs.getString(1),
                        rs.getObject(2, OffsetDateTime.class)), hash(handle));
        if (found.size()!=1) throw unavailable();
        return found.get(0);
    }

    private static org.springframework.web.server.ResponseStatusException unavailable() {
        return new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Invitation unavailable");
    }

    @Transactional
    public void revoke(Long workspaceId, Long invitationId) {
        int changed = jdbc.update("""
                UPDATE workspace_invitations SET revoked_at=CURRENT_TIMESTAMP
                WHERE workspace_id=? AND id=? AND revoked_at IS NULL
                """, workspaceId, invitationId);
        if (changed!=1) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Invitation not found");
        }
    }
}
