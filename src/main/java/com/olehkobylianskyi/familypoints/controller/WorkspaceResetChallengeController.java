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

import java.time.OffsetDateTime;
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
    private final com.olehkobylianskyi.familypoints.service.WorkspaceResetSnapshotService snapshots;
    private final com.olehkobylianskyi.familypoints.service.WorkspaceEpochTransactionGuard epochGuard;
    private final CurrentUserService current;
    private final JdbcTemplate db;
    private final PasswordEncoder passwordEncoder;
    private final ResetChallengeAttemptLimiter limiter;
    private final WorkspaceDataResetPreviewController preview;
    private final com.olehkobylianskyi.familypoints.service.WorkspaceResetProofService proofs;

    public WorkspaceResetChallengeController(CurrentUserService current, JdbcTemplate db,
                                              PasswordEncoder passwordEncoder, ResetChallengeAttemptLimiter limiter,
                                              WorkspaceDataResetPreviewController preview, com.olehkobylianskyi.familypoints.service.WorkspaceEpochTransactionGuard epochGuard, com.olehkobylianskyi.familypoints.service.WorkspaceResetSnapshotService snapshots, com.olehkobylianskyi.familypoints.service.WorkspaceResetProofService proofs) {
        this.proofs=proofs;
        this.epochGuard = epochGuard;
        this.snapshots = snapshots;
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

        epochGuard.requireUnfencedReauthTransaction(workspaceId);
        var account=current.getCurrentAccount();
        if (limiter.checkAndRecord(workspaceId, account.getId())>5)
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"RESET_REAUTH_RATE_LIMIT");
        epochGuard.requireCurrentRequestEpoch(workspaceId);
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

        // Recompute the current scoped content fingerprint under the family lock.
        // Snapshot freshness alone is not execution authorization; execute stays locked.
        String selectedJson=(String)operation.get("selected_categories_json");
        if (selectedJson==null || !selectedJson.matches("\\[(?:\"[A-Z_]+\"(?:,\"[A-Z_]+\")*)?\\]"))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Reset category snapshot invalid");
        var keys=new java.util.ArrayList<String>();
        var matcher=java.util.regex.Pattern.compile("\"([A-Z_]+)\"").matcher(selectedJson);
        while(matcher.find()) keys.add(matcher.group(1));
        if (!keys.equals(keys.stream().sorted().distinct().toList()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Reset category snapshot invalid");
        var latest=preview.preview(workspaceId);
        for (var key:keys) {
            if (!latest.categories().containsKey(key))
                throw new ResponseStatusException(HttpStatus.CONFLICT,"Reset category snapshot invalid");
        }
        if (!snapshots.digest(workspaceId,keys).equals(request.snapshotDigest()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"RESET_SNAPSHOT_STALE");

        // Internal service rechecks current credential/account/membership and binds the proof.
        // Secret remains internal until owner/deletion/proof transport policy is approved.
        var proof=proofs.issuePasswordChallenge(workspaceId,request.operationId(),account.getId(),
                current.getCurrentMemberId(),request.snapshotDigest(),request.password());
        return new ChallengeIssued(request.operationId(),proof.challengeId(),proof.expiresAt(),false);
    }

    @PostMapping("/execute")
    public void execute(@PathVariable Long workspaceId) {
        if (!current.belongsToWorkspace(workspaceId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Wrong workspace");
        throw new ResponseStatusException(HttpStatus.LOCKED,"RESET_EXECUTION_DISABLED");
    }

}
