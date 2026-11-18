package com.olehkobylianskyi.familypoints.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Operator-only read-only orphan detection. No scheduler, HTTP exposure,
 * registry updates, file deletion or restoration authorization.
 */
@Service
public class WorkspaceBackupOrphanAuditService {
    private final JdbcTemplate db;
    private final WorkspaceBackupLocalArtifactStore storage;

    public WorkspaceBackupOrphanAuditService(JdbcTemplate db,WorkspaceBackupLocalArtifactStore storage) {
        this.db=db;
        this.storage=storage;
    }

    public record Finding(long workspaceId,String fileName,UUID snapshotId,
                          String finding,long byteCount) {}

    public List<Finding> audit(long workspaceId,int maxFiles) {
        if(workspaceId<=0 || maxFiles<1 || maxFiles>1000)
            throw new IllegalArgumentException("INVALID_BACKUP_AUDIT_REQUEST");
        var files=storage.inventory(workspaceId,maxFiles);
        var result=new ArrayList<Finding>();
        for(var file:files) {
            String reason;
            if(file.symbolicLink() || !file.regularFile() || file.snapshotId()==null) {
                reason="UNRECOGNIZED_OR_UNSAFE_FILE";
            } else {
                var rows=db.queryForList("""
                    SELECT state,storage_key,artifact_sha256
                    FROM workspace_protective_backups
                    WHERE workspace_id=? AND snapshot_id=?
                    """,workspaceId,file.snapshotId());
                if(rows.size()>1)
                    reason="DUPLICATE_REGISTRY_EVIDENCE";
                else if(rows.isEmpty())
                    reason="NO_REGISTRY_RECORD";
                else {
                    var row=rows.get(0);
                    String expected="local:v1:"+workspaceId+"/"+file.snapshotId()+".fpbackup";
                    if(!expected.equals(row.get("storage_key"))
                            || row.get("artifact_sha256")==null)
                        reason="REGISTRY_ARTIFACT_EVIDENCE_INCOMPLETE";
                    else {
                        String sha=(String)row.get("artifact_sha256");
                        if(!sha.matches("[0-9a-f]{64}"))
                            reason="REGISTRY_ARTIFACT_EVIDENCE_INCOMPLETE";
                        else {
                            try {
                                // Ciphertext digest and sealed envelope only: NO decryption,
                                // file deletion, restore promotion or data mutation.
                                storage.verifyStoredCiphertext(workspaceId,file.snapshotId(),sha);
                                reason="EXPIRED".equals(row.get("state"))
                                        ? "EXPIRED_REGISTRY_FILE_PRESENT"
                                        : "REGISTERED_CIPHERTEXT_INTACT";
                            } catch(SecurityException invalid) {
                                reason="CIPHERTEXT_INTEGRITY_FAILED";
                            } catch(IllegalStateException unreadable) {
                                reason="CIPHERTEXT_READ_FAILED";
                            }
                        }
                    }
                }
            }
            result.add(new Finding(workspaceId,file.fileName(),file.snapshotId(),
                    reason,file.byteCount()));
        }
        return List.copyOf(result);
    }
}
