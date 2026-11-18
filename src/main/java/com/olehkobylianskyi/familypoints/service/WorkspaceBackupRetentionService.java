package com.olehkobylianskyi.familypoints.service;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * BKL-002 internal physical retention coordinator, not a scheduled job or
 * HTTP endpoint. Both lease acquisition and cleanup lock the same snapshot
 * database row to serialize lease vs delete. This class never verifies a
 * backup or authorizes a reset/restore operation.
 *
 * Retention is intentionally SQL-first. Marking EXPIRED is a durable revocation
 * of restorable status; this service never unlinks ciphertext in the SQL
 * transaction. Physical purge is deliberately disabled and requires a
 * separately reviewed recovery-safe finalizer and release approval.
 *
 * SQL rollback therefore leaves BOTH registry state and ciphertext intact.
 * Retries select only pre-expiry states; EXPIRED is terminal for leasing.
 */
@Service
public class WorkspaceBackupRetentionService {
    private final JdbcTemplate db;
    private final WorkspaceBackupLocalArtifactStore store;
    private static final Set<String> DELETABLE=Set.of(
            "REQUESTED","BACKING_UP","VERIFIED","FAILED","BLOCKED");
    public WorkspaceBackupRetentionService(JdbcTemplate db,
            WorkspaceBackupLocalArtifactStore store) {
        this.db=db;this.store=store;
    }

    /** Internal short-lived lease. No lease is issued for unverified bytes. */
    @Transactional
    public UUID acquireVerifiedLease(long workspaceId,UUID snapshotId,Duration duration) {
        if(workspaceId<=0 || snapshotId==null || duration==null
                || duration.isZero() || duration.isNegative()
                || duration.compareTo(Duration.ofMinutes(15))>0)
            throw new IllegalArgumentException("INVALID_BACKUP_LEASE");
        var rows=db.queryForList("""
            SELECT state,expires_at,restore_verified_at,artifact_sha256
            FROM workspace_protective_backups
            WHERE workspace_id=? AND snapshot_id=?
            FOR UPDATE
            """,workspaceId,snapshotId);
        if(rows.size()!=1 || !"VERIFIED".equals(rows.get(0).get("state"))
                || rows.get(0).get("restore_verified_at")==null
                || rows.get(0).get("artifact_sha256")==null
                || !unexpired(rows.get(0).get("expires_at")))
            throw new ResponseStatusException(HttpStatus.LOCKED,"BACKUP_NOT_RESTORABLE");
        UUID leaseId=UUID.randomUUID();
        int count=db.update("""
            INSERT INTO workspace_backup_restore_leases
                (lease_id,snapshot_id,workspace_id,expires_at)
            VALUES (?,?,?,CURRENT_TIMESTAMP + (? * INTERVAL '1 second'))
            """,leaseId,snapshotId,workspaceId,duration.toSeconds());
        if(count!=1)throw new IllegalStateException("BACKUP_LEASE_INSERT_FAILED");
        return leaseId;
    }

    private static boolean unexpired(Object expires) {
        if(expires instanceof OffsetDateTime value)
            return value.toInstant().isAfter(java.time.Instant.now());
        if(expires instanceof java.sql.Timestamp value)
            return value.toInstant().isAfter(java.time.Instant.now());
        throw new SecurityException("BACKUP_EXPIRY_TYPE_UNEXPECTED");
    }

    @Transactional
    public boolean release(long workspaceId,UUID snapshotId,UUID leaseId) {
        if(workspaceId<=0 || snapshotId==null || leaseId==null)
            throw new IllegalArgumentException("INVALID_BACKUP_LEASE_RELEASE");
        return db.update("""
            DELETE FROM workspace_backup_restore_leases
            WHERE workspace_id=? AND snapshot_id=? AND lease_id=?
            """,workspaceId,snapshotId,leaseId)==1;
    }

    /**
     * Internal bounded expiry transition, not physical cleanup. This commits
     * the non-restorable state before ANY future separately authorized unlink.
     * Repeated calls are idempotent and distinct workers use SKIP LOCKED.
     * No scheduled auto-run or physical purge is enabled.
     */
    @Transactional
    public int expireEligible(int maxRecords) {
        if(maxRecords<1 || maxRecords>100)
            throw new IllegalArgumentException("INVALID_BACKUP_EXPIRY_LIMIT");
        var expired=db.queryForList("""
            SELECT p.workspace_id,p.snapshot_id,p.storage_key,p.artifact_sha256,p.state
            FROM workspace_protective_backups p
            WHERE p.expires_at<CURRENT_TIMESTAMP
              AND p.state IN ('REQUESTED','BACKING_UP','VERIFIED','FAILED','BLOCKED')
              AND NOT EXISTS (
                SELECT 1 FROM workspace_backup_restore_leases l
                WHERE l.snapshot_id=p.snapshot_id
                  AND l.workspace_id=p.workspace_id
                  AND l.expires_at>CURRENT_TIMESTAMP
              )
            ORDER BY p.expires_at,p.id
            FOR UPDATE OF p SKIP LOCKED
            LIMIT ?
            """,maxRecords);
        int count=0;
        for(var row:expired) {
            long workspaceId=((Number)row.get("workspace_id")).longValue();
            UUID snapshotId=(UUID)row.get("snapshot_id");
            String state=(String)row.get("state");
            String storageKey=(String)row.get("storage_key");
            if(!DELETABLE.contains(state))continue;
            if(storageKey!=null) {
                String expected="local:v1:"+workspaceId+"/"+snapshotId+".fpbackup";
                if(!expected.equals(storageKey))
                    throw new SecurityException("BACKUP_RETENTION_STORAGE_KEY_MISMATCH");
                if(row.get("artifact_sha256")==null)
                    throw new SecurityException("BACKUP_RETENTION_MISSING_ARTIFACT_EVIDENCE");
                // Fail closed on missing/tampered ciphertext BEFORE changing SQL.
                // This is read-only; failed verification rolls the transaction back.
                store.verifyStoredCiphertext(workspaceId,snapshotId,
                        (String)row.get("artifact_sha256"));
            }
            int changed=db.update("""
                UPDATE workspace_protective_backups SET state='EXPIRED',
                    failure_code='RETENTION_WINDOW_EXPIRED',finished_at=CURRENT_TIMESTAMP
                WHERE workspace_id=? AND snapshot_id=? AND state=?
                  AND expires_at<CURRENT_TIMESTAMP
                  AND NOT EXISTS (
                    SELECT 1 FROM workspace_backup_restore_leases l
                    WHERE l.snapshot_id=? AND l.workspace_id=?
                      AND l.expires_at>CURRENT_TIMESTAMP)
                """,workspaceId,snapshotId,state,snapshotId,workspaceId);
            if(changed!=1)throw new IllegalStateException("BACKUP_RETENTION_CONCURRENT_CHANGE");
            count++;
        }
        return count;
    }

    /**
     * Read-only reconciliation for a future separate, explicitly authorized
     * physical finalizer. Never unlinks bytes, never changes backup state.
     * An EXPIRED row is not leaseable, even when its file is still retained.
     */
    @Transactional(readOnly=true)
    public List<ExpiredArtifact> expiredArtifactInventory(int maxRecords) {
        if(maxRecords<1 || maxRecords>100)
            throw new IllegalArgumentException("INVALID_BACKUP_EXPIRY_LIMIT");
        return db.query("""
            SELECT workspace_id,snapshot_id,storage_key,artifact_sha256
            FROM workspace_protective_backups
            WHERE state='EXPIRED' AND storage_key IS NOT NULL
            ORDER BY finished_at,id LIMIT ?
            """,(rs,i)->new ExpiredArtifact(rs.getLong("workspace_id"),
                    rs.getObject("snapshot_id",UUID.class),
                    rs.getString("storage_key"),rs.getString("artifact_sha256")),
                maxRecords);
    }

    public record ExpiredArtifact(long workspaceId,UUID snapshotId,
                                  String storageKey,String artifactSha256) {}

    /**
     * RET-02 non-destructive reconciliation preview. Safe to retry after restart:
     * no SQL changes, no filesystem writes, no physical purge and no scheduler.
     * Reports only metadata/status codes, never personal data or ciphertext.
     * The operator must separately authorize any future destructive finalizer.
     */
    @Transactional(readOnly=true)
    public List<ExpiredArtifactCheck> inspectExpiredArtifacts(int maxRecords) {
        List<ExpiredArtifact> candidates=expiredArtifactInventory(maxRecords);
        List<ExpiredArtifactCheck> results=new ArrayList<>(candidates.size());
        for(var candidate:candidates) {
            String expectedKey="local:v1:"+candidate.workspaceId()+"/"
                    +candidate.snapshotId()+".fpbackup";
            String status;
            if(!expectedKey.equals(candidate.storageKey())) {
                status="BLOCK_STORAGE_KEY";
            } else if(candidate.artifactSha256()==null
                    || !candidate.artifactSha256().matches("[0-9a-f]{64}")) {
                status="BLOCK_DIGEST";
            } else {
                try {
                    store.verifyStoredCiphertext(candidate.workspaceId(),
                            candidate.snapshotId(),candidate.artifactSha256());
                    status="RETAINED_VALID";
                } catch(SecurityException | IllegalArgumentException ex) {
                    status="BLOCK_INTEGRITY_OR_MISSING";
                } catch(IllegalStateException ex) {
                    status="BLOCK_STORAGE_UNAVAILABLE";
                }
            }
            results.add(new ExpiredArtifactCheck(candidate.workspaceId(),
                    candidate.snapshotId(),status));
        }
        return List.copyOf(results);
    }

    public record ExpiredArtifactCheck(long workspaceId,UUID snapshotId,
                                       String status) {}
}
