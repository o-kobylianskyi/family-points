package com.olehkobylianskyi.familypoints.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.crypto.SecretKey;
import java.util.*;

/** Internal no-HTTP metadata-to-file staging; NEVER promotes VERIFIED or executes reset.
 * SQL rollback does not roll back file creation: orphan reconciliation is required.
 */
@Service
public final class WorkspaceBackupRegistryStagingCoordinator {
    private final WorkspaceProtectiveBackupService registry;
    private final WorkspaceBackupArtifactStagingService staging;

    public WorkspaceBackupRegistryStagingCoordinator(WorkspaceProtectiveBackupService registry,
            WorkspaceBackupArtifactStagingService staging) {
        this.registry=registry;this.staging=staging;
    }
    public record Result(WorkspaceProtectiveBackupService.Backup registryEntry,
            WorkspaceBackupArtifactStagingService.Staged artifact,boolean recoveryVerified) {}

    @Transactional
    public Result registerAndStage(long workspaceId,long memberId,
            WorkspaceProtectiveBackupService.Request request,long expectedEpoch,SecretKey key) {
        if(workspaceId<=0 || memberId<=0 || request==null || expectedEpoch<0
                || request.categories()==null)
            throw new IllegalArgumentException("INVALID_BACKUP_COORDINATION_REQUEST");
        var entry=registry.request(workspaceId,memberId,request);
        if(entry.workspaceId()!=workspaceId
                || !entry.operationId().equals(request.operationId())
                || entry.dataEpoch()!=expectedEpoch
                || !entry.sourceDigest().equals(request.sourceDigest())
                || !entry.selectedCategories().equals(
                        request.categories().stream().distinct().sorted().toList())
                || !"REQUESTED".equals(entry.state())
                || entry.restoreVerified() || entry.executable())
            throw new IllegalStateException("BACKUP_REGISTRY_NOT_STAGEABLE");
        var artifact=staging.stage(workspaceId,entry.snapshotId(),
                entry.operationId(),entry.dataEpoch(),entry.selectedCategories(),
                request.schemaVersion(),key);
        if(!entry.snapshotId().equals(artifact.snapshotId())
                || !entry.sourceDigest().equals(artifact.sourceDigest())
                || !"STORED_UNVERIFIED".equals(artifact.state()))
            throw new SecurityException("BACKUP_REGISTRY_ARTIFACT_MISMATCH");
        return new Result(entry,artifact,false);
    }
}
