package com.olehkobylianskyi.familypoints.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Complements orphan disk inventory with the reverse discrepancy: a registry
 * row that names an unavailable or altered encrypted file. Pure read-only audit;
 * never changes registry state, deletes files or enables restore.
 */
@Service
public class WorkspaceBackupRegistryPresenceAuditService {
    private final JdbcTemplate db;
    private final WorkspaceBackupLocalArtifactStore storage;

    public WorkspaceBackupRegistryPresenceAuditService(
            JdbcTemplate db,WorkspaceBackupLocalArtifactStore storage) {
        this.db=db;
        this.storage=storage;
    }

    public record Finding(long workspaceId,UUID snapshotId,String registryState,
                          String classification) {}

    public List<Finding> audit(long workspaceId,int maxRows) {
        if(workspaceId<=0 || maxRows<1 || maxRows>1000)
            throw new IllegalArgumentException("INVALID_BACKUP_REGISTRY_PRESENCE_REQUEST");
        // max+1 explicitly distinguishes complete results from partial listings.
        var records=db.queryForList("""
                SELECT snapshot_id,state,storage_key,artifact_sha256
                FROM workspace_protective_backups
                WHERE workspace_id=?
                ORDER BY id
                LIMIT ?
                """,workspaceId,maxRows+1);
        if(records.size()>maxRows)
            throw new IllegalStateException("BACKUP_REGISTRY_AUDIT_LIMIT_EXCEEDED");

        var out=new ArrayList<Finding>();
        for(var record:records) {
            UUID id=(UUID)record.get("snapshot_id");
            String state=(String)record.get("state");
            String key=(String)record.get("storage_key");
            String sha=(String)record.get("artifact_sha256");
            String classification;
            if(id==null) {
                throw new SecurityException("BACKUP_REGISTRY_SNAPSHOT_ID_MISSING");
            } else if(key==null && sha==null) {
                classification="NOT_STAGED_IN_REGISTRY";
            } else if(!("local:v1:"+workspaceId+"/"+id+".fpbackup").equals(key)
                    || sha==null || !sha.matches("[0-9a-f]{64}")) {
                classification="INCOMPLETE_REGISTRY_EVIDENCE";
            } else {
                try {
                    storage.verifyStoredCiphertext(workspaceId,id,sha);
                    // A concurrent writer may commit after the first registry
                    // SELECT but before ciphertext verification completes.
                    // Re-observe exact source identity/evidence. This still
                    // is NOT a durable permission: it merely flags observed drift.
                    var fresh=db.queryForList("""
                            SELECT state,storage_key,artifact_sha256
                            FROM workspace_protective_backups
                            WHERE workspace_id=? AND snapshot_id=?
                            """,workspaceId,id);
                    classification=fresh.size()!=1
                            || !Objects.equals(state,fresh.get(0).get("state"))
                            || !Objects.equals(key,fresh.get(0).get("storage_key"))
                            || !Objects.equals(sha,fresh.get(0).get("artifact_sha256"))
                            ? "REGISTRY_CHANGED_DURING_AUDIT"
                            : "REGISTRY_CIPHERTEXT_PRESENT";
                } catch(SecurityException | IllegalArgumentException invalid) {
                    classification="REGISTRY_CIPHERTEXT_INVALID";
                } catch(IllegalStateException unavailable) {
                    classification="REGISTRY_CIPHERTEXT_UNAVAILABLE";
                }
            }
            out.add(new Finding(workspaceId,id,state,classification));
        }
        return List.copyOf(out);
    }
}
