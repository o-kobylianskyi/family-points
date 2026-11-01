package com.olehkobylianskyi.familypoints.service;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * BKL-BACKUP-001 metadata registry only. No export, verified promotion or
 * restore capability exists yet. Never treat REQUESTED as a usable backup.
 */
@Service
public class WorkspaceProtectiveBackupService {
    private final JdbcTemplate db;
    private final WorkspaceEpochTransactionGuard epoch;
    public WorkspaceProtectiveBackupService(JdbcTemplate db, WorkspaceEpochTransactionGuard epoch) {
        this.db=db; this.epoch=epoch;
    }
    public record Request(UUID operationId, String resetMode, List<String> categories,
                          String sourceDigest, String schemaVersion) {}
    public record Backup(UUID snapshotId, long workspaceId, UUID operationId, long dataEpoch,
                         String resetMode, List<String> selectedCategories, String sourceDigest,
                         String state, OffsetDateTime createdAt, OffsetDateTime expiresAt,
                         boolean restoreVerified, boolean executable) {}

    @Transactional
    public Backup request(long workspaceId,long memberId,Request request) {
        if(request==null || request.operationId()==null
                || !Set.of("FULL","SELECTIVE").contains(request.resetMode())
                || request.categories()==null || request.categories().stream().anyMatch(Objects::isNull)
                || request.sourceDigest()==null || !request.sourceDigest().matches("[0-9a-f]{64}")
                || request.schemaVersion()==null || request.schemaVersion().isBlank()
                || request.schemaVersion().length()>80)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"INVALID_BACKUP_REQUEST");
        var selected=request.categories().stream().distinct().sorted().toList();
        if("SELECTIVE".equals(request.resetMode()) && selected.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"EMPTY_BACKUP_SELECTION");
        long currentEpoch=epoch.requireCurrentRequestEpoch(workspaceId);
        String selectedJson;
        try {selectedJson=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(selected);}
        catch(Exception e){throw new IllegalStateException(e);}
        var existing=db.queryForList("""
            SELECT snapshot_id,requested_by_member_id,data_epoch,reset_mode,selected_categories_json,
                   source_digest,schema_version FROM workspace_protective_backups
            WHERE workspace_id=? AND operation_id=? FOR UPDATE
            """,workspaceId,request.operationId());
        if(!existing.isEmpty()) {
            var old=existing.get(0);
            if(((Number)old.get("requested_by_member_id")).longValue()!=memberId
                || ((Number)old.get("data_epoch")).longValue()!=currentEpoch
                || !request.resetMode().equals(old.get("reset_mode"))
                || !selectedJson.equals(old.get("selected_categories_json"))
                || !request.sourceDigest().equals(old.get("source_digest"))
                || !request.schemaVersion().equals(old.get("schema_version")))
                throw new ResponseStatusException(HttpStatus.CONFLICT,"BACKUP_OPERATION_CONFLICT");
            return get(workspaceId,(UUID)old.get("snapshot_id"));
        }
        var owner=db.queryForObject("""
            SELECT count(*) FROM family_members WHERE id=? AND family_id=?
            """,Integer.class,memberId,workspaceId);
        if(owner==null || owner!=1) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"BACKUP_MEMBER_MISMATCH");
        UUID id=UUID.randomUUID();
        db.update("""
            INSERT INTO workspace_protective_backups
                (snapshot_id,workspace_id,operation_id,requested_by_member_id,data_epoch,
                 reset_mode,selected_categories_json,source_digest,schema_version)
            VALUES (?,?,?,?,?,?,?,?,?)
            """,id,workspaceId,request.operationId(),memberId,currentEpoch,request.resetMode(),
                selectedJson,request.sourceDigest(),request.schemaVersion());
        return get(workspaceId,id);
    }

    /**
     * Family-scoped metadata listing, never exposes storage keys, encryption
     * secrets or backup bytes. Expired REQUESTED backups are not usable.
     */
    @Transactional(readOnly=true)
    public List<Backup> list(long workspaceId,int limit) {
        if(limit<1 || limit>100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"INVALID_BACKUP_LIST_LIMIT");
        var snapshots=db.queryForList("""
            SELECT snapshot_id FROM workspace_protective_backups
            WHERE workspace_id=? ORDER BY created_at DESC,id DESC LIMIT ?
            """,UUID.class,workspaceId,limit);
        return snapshots.stream().map(id->get(workspaceId,id)).toList();
    }

    /**
     * Internal metadata-only expiry sweep. Does NOT delete encrypted objects:
     * the independent storage lifecycle worker must verify remote deletion.
     * Never expires RESET_COMPLETED records or claims backup data is destroyed.
     */
    @Transactional
    public int expireUnverifiedMetadata() {
        return db.update("""
            UPDATE workspace_protective_backups SET state='EXPIRED',
                failure_code='RETENTION_WINDOW_EXPIRED'
            WHERE expires_at<CURRENT_TIMESTAMP
              AND state IN ('REQUESTED','BACKING_UP','FAILED','BLOCKED')
            """);
    }

    @Transactional(readOnly=true)
    public Backup get(long workspaceId,UUID snapshotId) {
        var rows=db.query("""
            SELECT snapshot_id,workspace_id,operation_id,data_epoch,reset_mode,
                   selected_categories_json,source_digest,state,created_at,expires_at,
                   restore_verified_at FROM workspace_protective_backups
            WHERE workspace_id=? AND snapshot_id=?
            """,(rs,i)->{
                List<String> selected;
                try {selected=new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                        rs.getString("selected_categories_json"),
                        new com.fasterxml.jackson.core.type.TypeReference<List<String>>(){});}
                catch(Exception ex){throw new IllegalStateException(ex);}
                return new Backup(rs.getObject("snapshot_id",UUID.class),rs.getLong("workspace_id"),
                    rs.getObject("operation_id",UUID.class),rs.getLong("data_epoch"),
                    rs.getString("reset_mode"),selected,rs.getString("source_digest"),
                    rs.getString("state"),rs.getObject("created_at",OffsetDateTime.class),
                    rs.getObject("expires_at",OffsetDateTime.class),
                    rs.getObject("restore_verified_at")!=null,false);
            },workspaceId,snapshotId);
        if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"BACKUP_NOT_FOUND");
        return rows.get(0);
    }
}
