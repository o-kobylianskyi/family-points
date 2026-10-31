package com.olehkobylianskyi.familypoints.service;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * Internal password proof lifecycle, NOT permission to delete family data.
 * Caller owns persistent attempt limiting BEFORE acquiring the family fence.
 * Future executor must separately authorize owner/policy, then consume in the
 * SAME transaction as all domain changes, audit and epoch increment.
 * No HTTP endpoint exposes the secret or invokes consumption while execute is disabled.
 */
@Service
public class WorkspaceResetProofService {
    private final JdbcTemplate db;
    private final WorkspaceEpochTransactionGuard guard;
    private final WorkspaceResetSnapshotService snapshots;
    private final PasswordEncoder passwords;
    private final SecureRandom random = new SecureRandom();

    public WorkspaceResetProofService(JdbcTemplate db, WorkspaceEpochTransactionGuard guard,
                                      WorkspaceResetSnapshotService snapshots, PasswordEncoder passwords) {
        this.db=db; this.guard=guard; this.snapshots=snapshots; this.passwords=passwords;
    }

    /** Never serialize or log this internal value; current HTTP DTO drops secret. */
    public static final class IssuedProof {
        private final UUID challengeId;
        private final OffsetDateTime expiresAt;
        private final String secret;
        private IssuedProof(UUID challengeId, OffsetDateTime expiresAt, String secret) {
            this.challengeId=challengeId; this.expiresAt=expiresAt; this.secret=secret;
        }
        public UUID challengeId() { return challengeId; }
        public OffsetDateTime expiresAt() { return expiresAt; }
        public String secret() { return secret; }
        @Override public String toString() { return "IssuedProof[redacted]"; }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public IssuedProof issuePasswordChallenge(long workspaceId, UUID operationId, long accountId,
                                              long memberId, String digest, String password) {
        validateContext(operationId,digest);
        long epoch=guard.requireCurrentRequestOrLockedEpoch(workspaceId);
        requireOperation(workspaceId,operationId,memberId,digest);
        String credential=currentCredential(workspaceId,accountId,memberId);
        if (password == null || !credential.startsWith("$2") || !passwords.matches(password,credential))
            throw failure(HttpStatus.FORBIDDEN,"RESET_REAUTH_REQUIRED");
        byte[] bytes=new byte[32]; random.nextBytes(bytes);
        String secret=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        UUID challengeId=UUID.randomUUID();
        var expires=db.queryForObject("SELECT clock_timestamp() + interval '5 minutes'",
                (row,index) -> row.getObject(1,OffsetDateTime.class));
        int inserted=db.update("""
                INSERT INTO workspace_reset_challenges
                (id,workspace_id,operation_id,account_id,verifier_hash,expires_at,
                 data_epoch,snapshot_digest,requested_by_member_id,credential_fingerprint)
                VALUES (?,?,?::uuid,?,?,?,?,?,?,?)
                ON CONFLICT (workspace_id,operation_id) DO NOTHING
                """,challengeId,workspaceId,operationId.toString(),accountId,sha256(secret),expires,
                epoch,digest,memberId,credentialFingerprint(accountId,credential));
        if (inserted!=1) throw failure(HttpStatus.CONFLICT,"RESET_CHALLENGE_ALREADY_ISSUED");
        return new IssuedProof(challengeId,expires,secret);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void consume(long workspaceId, UUID operationId, UUID challengeId, long accountId,
                        long memberId, String digest, String secret) {
        validateContext(operationId,digest);
        if (challengeId==null || secret==null || !secret.matches("[A-Za-z0-9_-]{43}"))
            throw failure(HttpStatus.FORBIDDEN,"RESET_REAUTH_PROOF_INVALID");
        long epoch=guard.requireCurrentRequestOrLockedEpoch(workspaceId);
        // Lock order: family -> operation -> current account SHARE -> challenge.
        requireOperation(workspaceId,operationId,memberId,digest);
        String credential=currentCredential(workspaceId,accountId,memberId);
        var rows=db.queryForList("""
                SELECT account_id,verifier_hash,consumed_at,data_epoch,snapshot_digest,
                       requested_by_member_id,credential_fingerprint,expires_at>clock_timestamp() AS fresh
                FROM workspace_reset_challenges
                WHERE id=? AND workspace_id=? AND operation_id=?::uuid FOR UPDATE
                """,challengeId,workspaceId,operationId.toString());
        if (rows.size()!=1) throw failure(HttpStatus.NOT_FOUND,"RESET_CHALLENGE_NOT_FOUND");
        var row=rows.get(0);
        if (((Number)row.get("account_id")).longValue()!=accountId)
            throw failure(HttpStatus.FORBIDDEN,"RESET_REAUTH_PROOF_INVALID");
        if (row.get("data_epoch")==null || row.get("requested_by_member_id")==null
                || row.get("snapshot_digest")==null || row.get("credential_fingerprint")==null)
            throw failure(HttpStatus.CONFLICT,"RESET_FRESH_CHALLENGE_REQUIRED");
        if (((Number)row.get("requested_by_member_id")).longValue()!=memberId
                || ((Number)row.get("data_epoch")).longValue()!=epoch
                || !digest.equals(row.get("snapshot_digest"))
                || !credentialFingerprint(accountId,credential).equals(row.get("credential_fingerprint")))
            throw failure(HttpStatus.CONFLICT,"RESET_CHALLENGE_BINDING_CHANGED");
        if (row.get("consumed_at")!=null) throw failure(HttpStatus.CONFLICT,"RESET_CHALLENGE_CONSUMED");
        if (!Boolean.TRUE.equals(row.get("fresh"))) throw failure(HttpStatus.GONE,"RESET_CHALLENGE_EXPIRED");
        if (!MessageDigest.isEqual(sha256(secret).getBytes(StandardCharsets.US_ASCII),
                ((String)row.get("verifier_hash")).getBytes(StandardCharsets.US_ASCII)))
            throw failure(HttpStatus.FORBIDDEN,"RESET_REAUTH_PROOF_INVALID");
        int updated=db.update("""
                UPDATE workspace_reset_challenges SET consumed_at=CURRENT_TIMESTAMP
                WHERE id=? AND workspace_id=? AND operation_id=?::uuid
                  AND consumed_at IS NULL AND expires_at>clock_timestamp()
                """,challengeId,workspaceId,operationId.toString());
        if (updated!=1) throw failure(HttpStatus.CONFLICT,"RESET_CHALLENGE_NOT_CURRENT");
    }

    private String currentCredential(long workspaceId,long accountId,long memberId) {
        // Shared account lock permits independent family proof reads but prevents
        // credential/disable changes until this transaction commits or rolls back.
        var credentials=db.queryForList("""
                SELECT a.password_hash FROM user_accounts a
                JOIN account_workspace_memberships l ON l.account_id=a.id
                JOIN family_members m ON m.id=l.family_member_id AND m.family_id=l.workspace_id
                WHERE a.id=? AND a.enabled=true AND l.workspace_id=? AND m.id=?
                  AND m.active=true AND m.deleted_at IS NULL FOR SHARE OF a
                """,String.class,accountId,workspaceId,memberId);
        if (credentials.size()!=1 || credentials.get(0)==null)
            throw failure(HttpStatus.FORBIDDEN,"RESET_ACTOR_NOT_ACTIVE");
        return credentials.get(0);
    }

    private void requireOperation(long workspaceId,UUID operationId,long memberId,String digest) {
        var operations=db.queryForList("""
                SELECT requested_by_member_id,snapshot_digest,status,selected_categories_json
                FROM workspace_reset_operations
                WHERE workspace_id=? AND operation_id=?::uuid FOR UPDATE
                """,workspaceId,operationId.toString());
        if (operations.size()!=1) throw failure(HttpStatus.NOT_FOUND,"RESET_OPERATION_NOT_FOUND");
        var op=operations.get(0);
        if (((Number)op.get("requested_by_member_id")).longValue()!=memberId)
            throw failure(HttpStatus.FORBIDDEN,"RESET_OPERATION_ACTOR_MISMATCH");
        if (!"CONFIRMED".equals(op.get("status")) || !digest.equals(op.get("snapshot_digest")))
            throw failure(HttpStatus.CONFLICT,"RESET_OPERATION_NOT_CURRENT");
        snapshots.requireStoredSnapshot(workspaceId,(String)op.get("selected_categories_json"),digest);
    }

    private static void validateContext(UUID operationId,String digest) {
        if (operationId==null || digest==null || !digest.matches("[0-9a-f]{64}"))
            throw failure(HttpStatus.BAD_REQUEST,"RESET_PROOF_CONTEXT_INVALID");
    }
    private static String credentialFingerprint(long accountId,String credential) {
        return sha256("reset-password-v1|"+accountId+"|"+credential);
    }
    private static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
    private static ResponseStatusException failure(HttpStatus status,String reason) {
        return new ResponseStatusException(status,reason);
    }
}
