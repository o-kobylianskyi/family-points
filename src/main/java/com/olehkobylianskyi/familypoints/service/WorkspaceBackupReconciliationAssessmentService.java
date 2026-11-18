package com.olehkobylianskyi.familypoints.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Read-only operator triage AFTER process restart. A finding is never a delete
 * authorization: filesystem and registry can change immediately after the scan.
 * No HTTP route, scheduled sweeper, SQL mutation, restoration or auto-cleanup.
 */
@Service
public class WorkspaceBackupReconciliationAssessmentService {
    public static final Duration MIN_REVIEW_AGE=Duration.ofHours(24);
    private final JdbcTemplate db;
    private final WorkspaceBackupLocalArtifactStore storage;
    private final Clock clock;

    @Autowired
    public WorkspaceBackupReconciliationAssessmentService(
            JdbcTemplate db,WorkspaceBackupLocalArtifactStore storage) {
        this(db,storage,Clock.systemUTC());
    }

    /** Explicit clock for deterministic test-only fixtures. */
    public WorkspaceBackupReconciliationAssessmentService(
            JdbcTemplate db,WorkspaceBackupLocalArtifactStore storage,Clock clock) {
        this.db=Objects.requireNonNull(db);
        this.storage=Objects.requireNonNull(storage);
        this.clock=Objects.requireNonNull(clock);
    }

    public record Assessment(long workspaceId,String fileName,UUID snapshotId,
                             String classification,Instant observedModifiedAt,
                             boolean hasActiveLease,boolean deleteAuthorized) {}

    /**
     * Candidate means "independent operator should investigate", NEVER "safe to
     * delete". Review candidates still need approved DB lease/registry locks and
     * fresh inventory revalidation in a future independently authorized cleanup.
     */
    public List<Assessment> assess(long workspaceId,int maxFiles) {
        if(workspaceId<=0 || maxFiles<1 || maxFiles>1000)
            throw new IllegalArgumentException("INVALID_BACKUP_RECONCILIATION_REQUEST");
        Instant now=clock.instant();
        var findings=new ArrayList<Assessment>();
        for(var file:storage.inventory(workspaceId,maxFiles)) {
            String classification;
            boolean activeLease=false;
            Instant modified=file.modifiedAt();
            if(file.symbolicLink() || !file.regularFile()) {
                classification="UNSAFE_FILE_HOLD";
            } else if(modified==null) {
                classification="UNKNOWN_FILE_AGE_HOLD";
            } else if(modified.isAfter(now)) {
                classification="FUTURE_FILE_TIMESTAMP_HOLD";
            } else if(Duration.between(modified,now).compareTo(MIN_REVIEW_AGE)<0) {
                classification="RECENT_FILE_HOLD";
            } else if(file.snapshotId()==null) {
                // Only our exact temporary-upload pattern is a recognizable
                // crash remnant; arbitrary files never become review candidates.
                classification=file.fileName().matches("\\.upload-[A-Za-z0-9-]+\\.tmp")
                        ? "STALE_TEMP_UPLOAD_REVIEW_ONLY" : "UNRECOGNIZED_FILE_HOLD";
            } else if(!file.fileName().equals(file.snapshotId()+".fpbackup")) {
                classification="UNRECOGNIZED_FILE_HOLD";
            } else {
                var rows=db.queryForList("""
                    SELECT p.state, p.storage_key, p.artifact_sha256,
                           EXISTS (
                               SELECT 1 FROM workspace_backup_restore_leases l
                               WHERE l.workspace_id=p.workspace_id
                                 AND l.snapshot_id=p.snapshot_id
                                 AND l.expires_at>CURRENT_TIMESTAMP
                           ) AS active_lease
                    FROM workspace_protective_backups p
                    WHERE p.workspace_id=? AND p.snapshot_id=?
                    """,workspaceId,file.snapshotId());
                if(rows.size()>1) {
                    classification="DUPLICATE_REGISTRY_HOLD";
                } else if(rows.isEmpty()) {
                    // Foreign-key constraints prohibit a legitimate lease with
                    // no registry row; concurrent transactions still require
                    // revalidation under locks before any destructive action.
                    classification="UNREGISTERED_FILE_REVIEW_ONLY";
                } else {
                    var row=rows.get(0);
                    activeLease=Boolean.TRUE.equals(row.get("active_lease"));
                    if(activeLease) {
                        classification="ACTIVE_RESTORE_LEASE_HOLD";
                    } else if("REQUESTED".equals(row.get("state"))
                            || "BACKING_UP".equals(row.get("state"))) {
                        classification="IN_PROGRESS_REGISTRY_HOLD";
                    } else {
                        // Even EXPIRED remains protected: only approved
                        // retention coordinator may decide physical cleanup.
                        classification="REGISTRY_OWNED_FILE_HOLD";
                    }
                }
            }
            findings.add(new Assessment(workspaceId,file.fileName(),file.snapshotId(),
                    classification,modified,activeLease,false));
        }
        return List.copyOf(findings);
    }
}
