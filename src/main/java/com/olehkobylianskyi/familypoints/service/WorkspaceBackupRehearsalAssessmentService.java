package com.olehkobylianskyi.familypoints.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.*;

/**
 * Internal only: assess a persisted encrypted file against its V51 registry
 * provenance and the rollback-only PostgreSQL rehearsal. Not a restore
 * executor, not a VERIFIED promotion, not a production release gate.
 *
 * Important: WorkspaceDisposableRestoreRehearsal uses transactional temporary
 * tables on its configured database. It is NOT a fresh disposable database
 * created by this service. Never run this on production until isolated
 * environment guarantees are implemented and separately accepted.
 */
@Service
public final class WorkspaceBackupRehearsalAssessmentService {
    private static final Set<String> SUPPORTED=Set.of(
            "MEMBER_GROUPS","MEDIA_ASSETS","MEDIA_BINDINGS");
    private final JdbcTemplate db;
    private final WorkspaceBackupLocalArtifactStore storage;
    private final ProtectiveBackupImportInspector inspector;
    private final WorkspaceDisposableRestoreRehearsal rehearsal;

    public WorkspaceBackupRehearsalAssessmentService(JdbcTemplate db,
            WorkspaceBackupLocalArtifactStore storage,
            ProtectiveBackupImportInspector inspector,
            WorkspaceDisposableRestoreRehearsal rehearsal) {
        this.db=db;this.storage=storage;this.inspector=inspector;
        this.rehearsal=rehearsal;
    }

    public record Assessment(long workspaceId,UUID snapshotId,String sha256,
            int groups,int assets,int bindings,int mediaVariants,
            String evidenceState,boolean verifiedForRestore) {}

    public Assessment assess(long workspaceId,UUID snapshotId,SecretKey key) {
        if(workspaceId<=0 || snapshotId==null || key==null)
            throw new IllegalArgumentException("INVALID_BACKUP_REHEARSAL_REQUEST");
        var matches=db.queryForList("""
                SELECT snapshot_id,workspace_id,operation_id,source_digest,
                       state,encrypted,storage_key,artifact_sha256,artifact_bytes,
                       selected_categories_json,expires_at,restore_verified_at
                FROM workspace_protective_backups
                WHERE workspace_id=? AND snapshot_id=?
                """,workspaceId,snapshotId);
        if(matches.size()!=1)
            throw new SecurityException("BACKUP_REHEARSAL_SNAPSHOT_NOT_FOUND");
        var row=matches.get(0);
        if(!"BACKING_UP".equals(row.get("state"))
                || !Boolean.TRUE.equals(row.get("encrypted"))
                || !("local:v1:"+workspaceId+"/"+snapshotId+".fpbackup")
                    .equals(row.get("storage_key"))
                || row.get("artifact_sha256")==null
                || row.get("artifact_bytes")==null
                || row.get("restore_verified_at")!=null)
            throw new SecurityException("BACKUP_REHEARSAL_REGISTRY_NOT_STAGEABLE");
        if(!validExpiry(row.get("expires_at")))
            throw new SecurityException("BACKUP_REHEARSAL_EXPIRED");
        final List<String> selected;
        try {
            selected=new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                    (String)row.get("selected_categories_json"),
                    new com.fasterxml.jackson.core.type.TypeReference<List<String>>(){});
        } catch(Exception e) {
            throw new SecurityException("BACKUP_REHEARSAL_INVALID_CATEGORY_METADATA",e);
        }
        if(selected.size()!=SUPPORTED.size()
                || !new HashSet<>(selected).equals(SUPPORTED))
            throw new SecurityException("BACKUP_REHEARSAL_UNSUPPORTED_SCOPE");

        String sha=(String)row.get("artifact_sha256");
        byte[] file=storage.read(workspaceId,snapshotId,sha);
        try {
            if(file.length!=((Number)row.get("artifact_bytes")).intValue())
                throw new SecurityException("BACKUP_REHEARSAL_SIZE_MISMATCH");
            var proof=inspector.inspect(file,workspaceId,key);
            if(!snapshotId.equals(proof.snapshotId())
                    || !proof.availableCategories().equals(
                        selected.stream().sorted().toList()))
                throw new SecurityException("BACKUP_REHEARSAL_SCOPE_MISMATCH");
            var result=rehearsal.rehearse(file,workspaceId,key);
            if(!result.rolledBack() || result.workspaceId()!=workspaceId
                    || !snapshotId.equals(result.snapshotId())
                    || proof.counts().get("MEMBER_GROUPS")!=result.groups()
                    || proof.counts().get("MEDIA_ASSETS")!=result.assets()
                    || proof.counts().get("MEDIA_BINDINGS")!=result.bindings())
                throw new SecurityException("BACKUP_REHEARSAL_RESULT_MISMATCH");
            return new Assessment(workspaceId,snapshotId,sha,result.groups(),
                    result.assets(),result.bindings(),result.checkedVariants(),
                    "ROLLBACK_ONLY_REHEARSED",false);
        } finally {
            Arrays.fill(file,(byte)0);
        }
    }

    private static boolean validExpiry(Object expiry) {
        if(expiry instanceof java.time.OffsetDateTime dt)
            return dt.toInstant().isAfter(java.time.Instant.now());
        if(expiry instanceof java.sql.Timestamp ts)
            return ts.toInstant().isAfter(java.time.Instant.now());
        return false;
    }
}
