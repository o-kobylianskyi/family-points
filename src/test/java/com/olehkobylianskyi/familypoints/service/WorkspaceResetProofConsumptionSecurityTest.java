package com.olehkobylianskyi.familypoints.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * C-C-03 synthetic negative proof tests. No real DB, HTTP or Reset execution.
 * The current operation remains CONFIRMED; consuming proof never deletes data.
 */
class WorkspaceResetProofConsumptionSecurityTest {
    private static final long WORKSPACE = 7L, ACCOUNT = 23L, MEMBER = 17L, EPOCH = 3L;
    private static final String DIGEST = "a".repeat(64);
    private static final String HASH = "$2a$existingLocalCredential";
    private static final String SECRET = "Z".repeat(43);

    private final JdbcTemplate db = mock(JdbcTemplate.class);
    private final WorkspaceEpochTransactionGuard guard = mock(WorkspaceEpochTransactionGuard.class);
    private final WorkspaceResetSnapshotService snapshots = mock(WorkspaceResetSnapshotService.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final WorkspaceResetProofService proof = new WorkspaceResetProofService(db, guard, snapshots, encoder);
    private final UUID operation = UUID.randomUUID();
    private final UUID challenge = UUID.randomUUID();

    private static String sha256(String input) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception error) {
            throw new AssertionError(error);
        }
    }

    private Map<String, Object> challengeRow() {
        Map<String, Object> row = new HashMap<>();
        row.put("account_id", ACCOUNT);
        row.put("verifier_hash", sha256(SECRET));
        row.put("consumed_at", null);
        row.put("data_epoch", EPOCH);
        row.put("snapshot_digest", DIGEST);
        row.put("requested_by_member_id", MEMBER);
        row.put("credential_fingerprint", sha256("reset-password-v1|" + ACCOUNT + "|" + HASH));
        row.put("fresh", true);
        return row;
    }

    @BeforeEach
    void confirmedOperationAndActiveCredentials() {
        when(guard.requireCurrentRequestOrLockedEpoch(WORKSPACE)).thenReturn(EPOCH);
        when(db.queryForList(contains("FROM workspace_reset_operations"),
                eq(WORKSPACE), eq(operation.toString())))
                .thenReturn(List.of(Map.of(
                        "requested_by_member_id", MEMBER, "snapshot_digest", DIGEST,
                        "status", "CONFIRMED", "selected_categories_json", "[]")));
        when(db.queryForList(contains("SELECT a.password_hash"), eq(String.class),
                eq(ACCOUNT), eq(WORKSPACE), eq(MEMBER)))
                .thenReturn(List.of(HASH));
        when(db.queryForList(contains("FROM workspace_reset_challenges"),
                eq(challenge), eq(WORKSPACE), eq(operation.toString())))
                .thenReturn(List.of(challengeRow()));
        when(db.update(contains("UPDATE workspace_reset_challenges"),
                eq(challenge), eq(WORKSPACE), eq(operation.toString())))
                .thenReturn(1);
    }

    private HttpStatus denied(UUID challengeId, long workspace, long account, long member,
                              String digest, String secret) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> proof.consume(workspace, operation, challengeId, account, member, digest, secret));
        return (HttpStatus) ex.getStatusCode();
    }

    @Test void validFreshProofCanBeMarkedConsumedButDoesNotExecuteReset() {
        proof.consume(WORKSPACE, operation, challenge, ACCOUNT, MEMBER, DIGEST, SECRET);
        verify(db).update(contains("UPDATE workspace_reset_challenges"),
                eq(challenge), eq(WORKSPACE), eq(operation.toString()));
        verify(db, never()).update(contains("DELETE FROM"), any(Object[].class));
    }

    @Test void spentProofCannotBeReused() {
        var row = challengeRow();
        row.put("consumed_at", java.time.OffsetDateTime.now());
        when(db.queryForList(contains("FROM workspace_reset_challenges"),
                eq(challenge), eq(WORKSPACE), eq(operation.toString()))).thenReturn(List.of(row));
        assertEquals(HttpStatus.CONFLICT, denied(challenge, WORKSPACE, ACCOUNT, MEMBER, DIGEST, SECRET));
        verify(db, never()).update(contains("UPDATE workspace_reset_challenges"), any(Object[].class));
    }

    @Test void expiredProofFailsClosed() {
        var row = challengeRow();
        row.put("fresh", false);
        when(db.queryForList(contains("FROM workspace_reset_challenges"),
                eq(challenge), eq(WORKSPACE), eq(operation.toString()))).thenReturn(List.of(row));
        assertEquals(HttpStatus.GONE, denied(challenge, WORKSPACE, ACCOUNT, MEMBER, DIGEST, SECRET));
        verify(db, never()).update(contains("UPDATE workspace_reset_challenges"), any(Object[].class));
    }

    @Test void priorEpochOrChangedCredentialCannotAuthorize() {
        var row = challengeRow();
        row.put("data_epoch", EPOCH - 1);
        when(db.queryForList(contains("FROM workspace_reset_challenges"),
                eq(challenge), eq(WORKSPACE), eq(operation.toString()))).thenReturn(List.of(row));
        assertEquals(HttpStatus.CONFLICT, denied(challenge, WORKSPACE, ACCOUNT, MEMBER, DIGEST, SECRET));
        row.put("data_epoch", EPOCH);
        row.put("credential_fingerprint", sha256("reset-password-v1|" + ACCOUNT + "|oldHash"));
        assertEquals(HttpStatus.CONFLICT, denied(challenge, WORKSPACE, ACCOUNT, MEMBER, DIGEST, SECRET));
        verify(db, never()).update(contains("UPDATE workspace_reset_challenges"), any(Object[].class));
    }

    @Test void wrongActorMemberDigestOrSecretCannotAuthorize() {
        var row = challengeRow();
        row.put("account_id", ACCOUNT + 1);
        when(db.queryForList(contains("FROM workspace_reset_challenges"),
                eq(challenge), eq(WORKSPACE), eq(operation.toString()))).thenReturn(List.of(row));
        assertEquals(HttpStatus.FORBIDDEN, denied(challenge, WORKSPACE, ACCOUNT, MEMBER, DIGEST, SECRET));
        row.put("account_id", ACCOUNT);
        row.put("requested_by_member_id", MEMBER + 1);
        assertEquals(HttpStatus.CONFLICT, denied(challenge, WORKSPACE, ACCOUNT, MEMBER, DIGEST, SECRET));
        row.put("requested_by_member_id", MEMBER);
        assertEquals(HttpStatus.FORBIDDEN, denied(challenge, WORKSPACE, ACCOUNT, MEMBER, DIGEST, "Y".repeat(43)));
        assertEquals(HttpStatus.BAD_REQUEST, denied(challenge, WORKSPACE, ACCOUNT, MEMBER, "not-a-digest", SECRET));
        verify(db, never()).update(contains("UPDATE workspace_reset_challenges"), any(Object[].class));
    }

    @Test void noProofForOtherWorkspaceOrOtherOperation() {
        when(guard.requireCurrentRequestOrLockedEpoch(WORKSPACE + 1)).thenReturn(EPOCH);
        assertEquals(HttpStatus.NOT_FOUND,
                denied(challenge, WORKSPACE + 1, ACCOUNT, MEMBER, DIGEST, SECRET));
        assertEquals(HttpStatus.NOT_FOUND,
                denied(UUID.randomUUID(), WORKSPACE, ACCOUNT, MEMBER, DIGEST, SECRET));
        verify(db, never()).update(contains("UPDATE workspace_reset_challenges"), any(Object[].class));
    }

    @Test void concurrentConsumptionLostUpdateMustFailClosed() {
        when(db.update(contains("UPDATE workspace_reset_challenges"),
                eq(challenge), eq(WORKSPACE), eq(operation.toString()))).thenReturn(0);
        assertEquals(HttpStatus.CONFLICT, denied(challenge, WORKSPACE, ACCOUNT, MEMBER, DIGEST, SECRET));
    }
}
