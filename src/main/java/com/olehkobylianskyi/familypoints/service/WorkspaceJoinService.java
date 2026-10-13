package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/** DEV foundation; endpoints use authenticated identities and opaque workspace join codes. */
@Service
public class WorkspaceJoinService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final JdbcTemplate jdbc;
    private final CurrentUserService currentUser;

    public WorkspaceJoinService(JdbcTemplate jdbc, CurrentUserService currentUser) {
        this.jdbc = jdbc;
        this.currentUser = currentUser;
    }

    private static String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String newCode() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    /** Code is returned once: only its hash is retained. Rotation invalidates the old code. */
    @Transactional
    public String rotateJoinCode(Long workspaceId) {
        String code = newCode();
        jdbc.update("""
                INSERT INTO workspace_join_codes (workspace_id, code_hash, enabled)
                VALUES (?, ?, true)
                ON CONFLICT (workspace_id) DO UPDATE
                SET code_hash = EXCLUDED.code_hash, enabled = true, rotated_at = CURRENT_TIMESTAMP
                """, workspaceId, digest(code));
        return code;
    }

    @Transactional
    public void disableJoinCode(Long workspaceId) {
        jdbc.update("UPDATE workspace_join_codes SET enabled = false WHERE workspace_id = ?", workspaceId);
    }

    /**
     * A shareable code NEVER grants workspace membership. It creates only a manager-reviewed request.
     * Failed or unknown codes deliberately return the same generic message.
     */
    @Transactional
    public void requestJoin(String code) {
        if (code == null || !code.matches("[0-9a-fA-F]{48}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or disabled join code");
        }
        Long accountId = currentUser.getCurrentAccount().getId();
        List<Long> ids = jdbc.queryForList("""
                SELECT workspace_id FROM workspace_join_codes
                WHERE code_hash = ? AND enabled = true
                """, Long.class, digest(code.toLowerCase()));
        if (ids.size() != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or disabled join code");
        }
        Long workspaceId = ids.get(0);
        Integer exists = jdbc.queryForObject("""
                SELECT count(*) FROM account_workspace_memberships
                WHERE account_id = ? AND workspace_id = ?
                """, Integer.class, accountId, workspaceId);
        if (exists != null && exists > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account already belongs to workspace");
        }
        try {
            jdbc.update("""
                    INSERT INTO workspace_join_requests(workspace_id, requester_account_id)
                    VALUES (?, ?) ON CONFLICT DO NOTHING
                    """, workspaceId, accountId);
        } catch (DuplicateKeyException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Join request already exists");
        }
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> pendingForManager(Long workspaceId) {
        return jdbc.queryForList("""
                SELECT id, requester_account_id AS "requesterAccountId", requested_at AS "requestedAt"
                FROM workspace_join_requests
                WHERE workspace_id = ? AND status = 'PENDING'
                ORDER BY requested_at ASC
                """, workspaceId);
    }

    /**
     * Approval requires mapping to a workspace-local member profile and role.
     * Never assign a default membership automatically here.
     */
    @Transactional
    public void rejectRequest(Long workspaceId, Long requestId) {
        int changed = jdbc.update("""
                UPDATE workspace_join_requests
                SET status = 'REJECTED', reviewed_at = CURRENT_TIMESTAMP, reviewed_by_account_id = ?
                WHERE workspace_id = ? AND id = ? AND status = 'PENDING'
                """, currentUser.getCurrentAccount().getId(), workspaceId, requestId);
        if (changed != 1) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pending request not found");
    }
}
