package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.security.WorkspaceRestorePermitScopeV2;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;

/**
 * Internal B-INT-02 RESTORE-specific owner proof and durable one-use scope.
 * No controller exposes this service; a permit never enables production Restore.
 * The actual replay caller MUST consume under its own transaction, family lock,
 * signed source verification and final epoch/receipt commit.
 */
@Service
public final class WorkspaceRestorePermitService {
    private final JdbcTemplate db;
    private final WorkspaceEpochTransactionGuard epochs;
    private final PasswordEncoder passwords;
    private final WorkspaceRestorePasswordLimiter limiter;
    private final ProtectiveBackupImportInspector inspector;

    public WorkspaceRestorePermitService(JdbcTemplate db,
            WorkspaceEpochTransactionGuard epochs,PasswordEncoder passwords,
            WorkspaceRestorePasswordLimiter limiter,ProtectiveBackupImportInspector inspector) {
        this.db=db;this.epochs=epochs;this.passwords=passwords;
        this.limiter=limiter;this.inspector=inspector;
    }

    /**
     * Production password verification, rather than a purported client boolean.
     * The signed archive and exact approved map are verified again here.
     * Legacy owner bindings without trusted registered provenance are denied.
     */
    @Transactional
    public void issuePassword(WorkspaceRestorePermitScopeV2.RestoreScope scope,
            byte[] sourceFile,SecretKey key,Map<Long,Long> reviewedMemberMap,
            String currentPassword) {
        Objects.requireNonNull(scope,"scope");
        limiter.requireAvailable(scope.targetWorkspaceId(),scope.authorizedOwnerAccountId());
        verifiedSource(scope,sourceFile,key,reviewedMemberMap);
        long epoch=epochs.requireCurrentEpoch(scope.targetWorkspaceId(),scope.targetExpectedEpoch());
        if(epoch!=scope.targetExpectedEpoch())
            throw new SecurityException("RESTORE_TARGET_EPOCH_CHANGED");
        Instant now=Instant.now();
        if(!scope.matches(scope,now) ||
                scope.issuedAt().isBefore(now.minusSeconds(15)) ||
                scope.issuedAt().isAfter(now.plusSeconds(1)))
            throw new SecurityException("RESTORE_PERMIT_ISSUANCE_NOT_CURRENT");
        String credential=requireOwnerPasswordHash(scope);
        if(currentPassword==null || !credential.startsWith("$2")
                || !passwords.matches(currentPassword,credential)) {
            limiter.recordFailure(scope.targetWorkspaceId(),scope.authorizedOwnerAccountId());
            throw new SecurityException("RESTORE_FRESH_PASSWORD_REQUIRED");
        }
        String digest=WorkspaceRestorePermitScopeV2.canonicalScopeDigest(scope);
        int inserted=db.update("""
            INSERT INTO workspace_restore_permits
              (operation_id,target_workspace_id,owner_account_id,owner_member_id,
               scope_digest,credential_fingerprint,target_expected_epoch,issued_at,expires_at)
            VALUES (?,?,?,?,?,?,?,?,?)
            ON CONFLICT (operation_id) DO NOTHING
            """,scope.operationId(),scope.targetWorkspaceId(),
                scope.authorizedOwnerAccountId(),scope.authorizedOwnerMemberId(),
                digest,credentialFingerprint(scope.authorizedOwnerAccountId(),credential),
                scope.targetExpectedEpoch(),
                java.sql.Timestamp.from(scope.issuedAt()),
                java.sql.Timestamp.from(scope.expiresAt()));
        if(inserted!=1)
            throw new SecurityException("RESTORE_PERMIT_OPERATION_ALREADY_EXISTS");
        limiter.clearSuccess(scope.targetWorkspaceId(),scope.authorizedOwnerAccountId());
    }

    /**
     * The target importer must call this AFTER family lock and source validation,
     * in the SAME transaction as FK/ledger writes, epoch increment and receipt.
     * A later rollback reverts consumption as well as data writes.
     */
    @Transactional(propagation=Propagation.MANDATORY)
    public void consumeInRestoreTransaction(WorkspaceRestorePermitScopeV2.RestoreScope scope,
            byte[] sourceFile,SecretKey key,Map<Long,Long> reviewedMemberMap) {
        Objects.requireNonNull(scope,"scope");
        epochs.requireCurrentEpoch(scope.targetWorkspaceId(),scope.targetExpectedEpoch());
        verifiedSource(scope,sourceFile,key,reviewedMemberMap);
        Instant now=Instant.now();
        if(!scope.matches(scope,now))
            throw new SecurityException("RESTORE_PERMIT_EXPIRED_OR_MISMATCH");
        String credential=requireOwnerPasswordHash(scope);
        var rows=db.queryForList("""
            SELECT target_workspace_id,owner_account_id,owner_member_id,
                   scope_digest,credential_fingerprint,target_expected_epoch,
                   consumed_at,issued_at,expires_at
            FROM workspace_restore_permits
            WHERE operation_id=? FOR UPDATE
            """,scope.operationId());
        if(rows.size()!=1)
            throw new SecurityException("RESTORE_PERMIT_NOT_FOUND");
        var saved=rows.get(0);
        if(!Objects.equals(((Number)saved.get("target_workspace_id")).longValue(),
                           scope.targetWorkspaceId())
                || !Objects.equals(((Number)saved.get("owner_account_id")).longValue(),
                           scope.authorizedOwnerAccountId())
                || !Objects.equals(((Number)saved.get("owner_member_id")).longValue(),
                           scope.authorizedOwnerMemberId())
                || !Objects.equals(((Number)saved.get("target_expected_epoch")).longValue(),
                           scope.targetExpectedEpoch())
                || saved.get("consumed_at")!=null
                || !digestEquals((String)saved.get("scope_digest"),
                           WorkspaceRestorePermitScopeV2.canonicalScopeDigest(scope))
                || !digestEquals((String)saved.get("credential_fingerprint"),
                           credentialFingerprint(scope.authorizedOwnerAccountId(),credential)))
            throw new SecurityException("RESTORE_PERMIT_BINDING_INVALID");
        int changed=db.update("""
            UPDATE workspace_restore_permits SET consumed_at=clock_timestamp()
            WHERE operation_id=? AND consumed_at IS NULL
              AND expires_at>clock_timestamp() AND issued_at<=clock_timestamp()
            """,scope.operationId());
        if(changed!=1)
            throw new SecurityException("RESTORE_PERMIT_USED_OR_EXPIRED");
    }

    private void verifiedSource(WorkspaceRestorePermitScopeV2.RestoreScope scope,
            byte[] sourceFile,SecretKey key,Map<Long,Long> reviewedMemberMap) {
        if(sourceFile==null||key==null||reviewedMemberMap==null)
            throw new SecurityException("RESTORE_SIGNED_SOURCE_REQUIRED");
        var proof=inspector.inspectAncestryV2(
                sourceFile,scope.source().sourceWorkspaceId(),key);
        if(!scope.source().equals(proof.verifiedSourceArchiveV2()))
            throw new SecurityException("RESTORE_AUTHENTICATED_ARCHIVE_MISMATCH");
        if(proof.verifiedCategoryCounts().size()!=8)
            throw new SecurityException("RESTORE_EIGHT_CATEGORY_SOURCE_REQUIRED");
        String expected=proof.canonicalMappingDigestForReviewedIds(
                scope.targetWorkspaceId(),reviewedMemberMap);
        if(!digestEquals(expected,scope.memberMappingDigest()))
            throw new SecurityException("RESTORE_MEMBER_BINDING_MISMATCH");
        // Signed source IDs are not identities in the target database. Require
        // every explicitly reviewed target member to exist within this family
        // before issuing/consuming a permission. Never compare numeric IDs to
        // source IDs or accept a member borrowed from another family.
        if(reviewedMemberMap.isEmpty() || reviewedMemberMap.size()>2000)
            throw new SecurityException("RESTORE_MEMBER_MAP_SIZE_INVALID");
        var targetMemberIds=new java.util.HashSet<Long>();
        for(Long targetId:reviewedMemberMap.values()) {
            if(targetId==null || targetId<=0 || !targetMemberIds.add(targetId))
                throw new SecurityException("RESTORE_MEMBER_TARGET_NOT_INJECTIVE");
        }
        var actualTargetMembers=new java.util.HashSet<Long>(db.queryForList("""
                SELECT id FROM family_members
                WHERE family_id=? AND active=true AND deleted_at IS NULL
                """,Long.class,scope.targetWorkspaceId()));
        if(!actualTargetMembers.containsAll(targetMemberIds))
            throw new SecurityException("RESTORE_MEMBER_TARGET_NOT_OWNED");
    }

    private String requireOwnerPasswordHash(WorkspaceRestorePermitScopeV2.RestoreScope scope) {
        var hashes=db.queryForList("""
            SELECT a.password_hash FROM workspace_original_owner_bindings o
            JOIN user_accounts a ON a.id=o.owner_account_id AND a.enabled=true
            JOIN account_workspace_memberships l ON l.account_id=a.id
                 AND l.workspace_id=o.workspace_id
                 AND l.family_member_id=o.owner_member_id
            JOIN family_members m ON m.id=o.owner_member_id
                 AND m.family_id=o.workspace_id
                 AND m.active=true AND m.deleted_at IS NULL
            WHERE o.workspace_id=? AND o.owner_account_id=?
                 AND o.owner_member_id=? AND o.provenance='REGISTRATION_VERIFIED'
            FOR SHARE OF a,l,m
            """,String.class,scope.targetWorkspaceId(),
            scope.authorizedOwnerAccountId(),scope.authorizedOwnerMemberId());
        if(hashes.size()!=1||hashes.get(0)==null)
            throw new SecurityException("RESTORE_ORIGINAL_OWNER_NOT_VERIFIED");
        return hashes.get(0);
    }

    private static String credentialFingerprint(long id,String hash) {
        return sha("family-points:restore:credential:v1|"+id+"|"+hash);
    }
    private static String sha(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        }catch(NoSuchAlgorithmException unavailable) { throw new IllegalStateException(unavailable); }
    }
    private static boolean digestEquals(String left,String right) {
        return left!=null&&right!=null&&MessageDigest.isEqual(
                left.getBytes(StandardCharsets.US_ASCII),
                right.getBytes(StandardCharsets.US_ASCII));
    }
}
