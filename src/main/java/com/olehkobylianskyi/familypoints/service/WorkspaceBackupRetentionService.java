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
 * Filesystem deletion cannot participate in SQL rollback. If commit fails
 * after a physical deletion, a later sweep detects the missing file and
 * expires the registry; a restore always checks the actual file and digest.
 * Use only an independently approved single-worker run after testing.
 */
@Service
public final class WorkspaceBackupRetentionService {
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
                || !((OffsetDateTime)rows.get(0).get("expires_at"))
                    .isAfter(OffsetDateTime.now()))
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
     * Internal bounded cleanup invoked explicitly by an operator/coordinator.
     * No scheduled auto-run; do not enable on production before recovery QA.
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
                // Physical deletion precedes SQL expiry and is idempotent.
                store.deleteAuthorizedExpired(workspaceId,snapshotId);
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
}
