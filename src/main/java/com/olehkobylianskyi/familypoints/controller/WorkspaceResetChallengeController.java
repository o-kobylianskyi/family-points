package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import com.olehkobylianskyi.familypoints.service.ResetChallengeAttemptLimiter;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.UUID;

/**
 * BKL-002: password-reauthenticated challenge issuance only.
 * Execute stays locked until FK closure, epoch-atomic writes, rollback and
 * cross-family concurrency acceptance are complete.
 */
@RestController
@RequestMapping("/workspaces/{workspaceId}/data-reset")
@PreAuthorize("@workspaceSecurity.canManageWorkspace(#workspaceId)")
public class WorkspaceResetChallengeController {
    private final CurrentUserService current;
    private final JdbcTemplate db;
    private final PasswordEncoder passwordEncoder;
    private final ResetChallengeAttemptLimiter limiter;
    private final WorkspaceDataResetPreviewController preview;
    private final SecureRandom random = new SecureRandom();

    public WorkspaceResetChallengeController(CurrentUserService current, JdbcTemplate db,
                                              PasswordEncoder passwordEncoder, ResetChallengeAttemptLimiter limiter,
                                              WorkspaceDataResetPreviewController preview) {
        this.current=current;
        this.db=db;
        this.passwordEncoder=passwordEncoder;
        this.limiter=limiter;
        this.preview=preview;
    }

    public record ChallengeRequest(UUID operationId, String snapshotDigest, String password) {}
    public record ChallengeIssued(UUID operationId, UUID challengeId, OffsetDateTime expiresAt,
                                  boolean executionEnabled) {}

    @PostMapping("/challenge")
    @Transactional
    public ChallengeIssued challenge(@PathVariable Long workspaceId, @RequestBody ChallengeRequest request) {
        if (!current.belongsToWorkspace(workspaceId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong workspace");
        if (request == null || request.operationId() == null ||
                request.snapshotDigest() == null || !request.snapshotDigest().matches("[0-9a-f]{64}") ||
                request.password() == null || request.password().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid challenge request");

        var account=current.getCurrentAccount();
        if (limiter.checkAndRecord(workspaceId, account.getId())>5)
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"RESET_REAUTH_RATE_LIMIT");
        // Google-only accounts without a local password require a separate fresh OAuth reauth.
        if (account.getPasswordHash() == null ||
                !account.getPasswordHash().startsWith("$2") ||
                !passwordEncoder.matches(request.password(),account.getPasswordHash()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Fresh reauthentication required");

        var rows=db.queryForList("""
                SELECT requested_by_member_id,snapshot_digest,status,selected_categories_json
                FROM workspace_reset_operations
                WHERE workspace_id=? AND operation_id=?::uuid FOR UPDATE
                """,workspaceId,request.operationId().toString());
        if (rows.isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Reset operation not found");
        var operation=rows.get(0);
        if (((Number)operation.get("requested_by_member_id")).longValue()!=current.getCurrentMemberId())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Reset operation belongs to another member");
        if (!"CONFIRMED".equals(operation.get("status")) ||
                !request.snapshotDigest().equals(operation.get("snapshot_digest")))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Reset operation is not confirmed/current");

        // Recompute the original preview fingerprint against the current scoped
        // inventory. Equal row counts are not a full MVCC snapshot; execute stays locked.
        String selectedJson=(String)operation.get("selected_categories_json");
        if (selectedJson==null || !selectedJson.matches("\\[(?:\"[A-Z_]+\"(?:,\"[A-Z_]+\")*)?\\]"))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Reset category snapshot invalid");
        var keys=new java.util.ArrayList<String>();
        var matcher=java.util.regex.Pattern.compile("\"([A-Z_]+)\"").matcher(selectedJson);
        while(matcher.find()) keys.add(matcher.group(1));
        if (!keys.equals(keys.stream().sorted().distinct().toList()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Reset category snapshot invalid");
        var latest=preview.preview(workspaceId);
        var fingerprint=new StringBuilder("reset-v1|").append(workspaceId).append('|');
        for (var key:keys) {
            var category=latest.categories().get(key);
            if (category==null)
                throw new ResponseStatusException(HttpStatus.CONFLICT,"Reset category snapshot invalid");
            fingerprint.append(key).append('=').append(category.recordCount()).append(';');
        }
        if (!sha256(fingerprint.toString().getBytes(StandardCharsets.UTF_8))
                .equals(request.snapshotDigest()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"RESET_SNAPSHOT_STALE");

        // A random verifier is stored only as a digest; no secret is disclosed
        // until a separately reviewed execute proof protocol exists.
        byte[] secret=new byte[32];
        random.nextBytes(secret);
        var challengeId=UUID.randomUUID();
        var expiresAt=OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(5);
        int inserted=db.update("""
                INSERT INTO workspace_reset_challenges
                  (id,workspace_id,operation_id,account_id,verifier_hash,expires_at)
                VALUES (?, ?, ?::uuid, ?, ?, ?)
                ON CONFLICT (workspace_id,operation_id) DO NOTHING
                """,challengeId,workspaceId,request.operationId().toString(),account.getId(),
                sha256(secret),expiresAt);
        if(inserted!=1)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Reset challenge already issued");
        return new ChallengeIssued(request.operationId(),challengeId,expiresAt,false);
    }

    @PostMapping("/execute")
    public void execute(@PathVariable Long workspaceId) {
        if (!current.belongsToWorkspace(workspaceId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Wrong workspace");
        throw new ResponseStatusException(HttpStatus.LOCKED,"RESET_EXECUTION_DISABLED");
    }

    private static String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
