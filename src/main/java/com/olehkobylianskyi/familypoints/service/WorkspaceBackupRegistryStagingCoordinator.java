package com.olehkobylianskyi.familypoints.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import javax.crypto.SecretKey;
import java.util.*;

/** Internal no-HTTP metadata-to-file staging; NEVER promotes VERIFIED or executes reset.
 * SQL rollback does not roll back file creation: orphan reconciliation is required.
 */
@Service
public final class WorkspaceBackupRegistryStagingCoordinator {
    private final WorkspaceProtectiveBackupService registry;
    private final WorkspaceBackupArtifactStagingService staging;
    private final WorkspaceBackupLocalArtifactStore storage;

    public WorkspaceBackupRegistryStagingCoordinator(WorkspaceProtectiveBackupService registry,
            WorkspaceBackupArtifactStagingService staging,
            WorkspaceBackupLocalArtifactStore storage) {
        this.registry=registry;this.staging=staging;this.storage=storage;
    }
    public record Result(WorkspaceProtectiveBackupService.Backup registryEntry,
            WorkspaceBackupArtifactStagingService.Staged artifact,boolean recoveryVerified) {}

    @Transactional
    public Result registerAndStage(long workspaceId,long memberId,
            WorkspaceProtectiveBackupService.Request request,long expectedEpoch,SecretKey key) {
        if(workspaceId<=0 || memberId<=0 || request==null || expectedEpoch<0
                || request.categories()==null)
            throw new IllegalArgumentException("INVALID_BACKUP_COORDINATION_REQUEST");
        if(!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive())
            throw new IllegalStateException("BACKUP_COORDINATION_TRANSACTION_REQUIRED");
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
        // A physical artifact outlives a failed DB transaction. Register
        // compensation before returning so commit failures cannot leave an
        // apparently referenced backup behind. If physical cleanup fails,
        // reconciliation must still detect the orphan; never fake VERIFIED.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if(status==STATUS_ROLLED_BACK) {
                    try {storage.deleteAuthorizedExpired(workspaceId,artifact.snapshotId());}
                    catch(RuntimeException ignored) {
                        // Leave the encrypted orphan for independently audited
                        // reconciliation; do not pretend cleanup succeeded.
                    }
                }
            }
        });
        if(!entry.snapshotId().equals(artifact.snapshotId())
                || !entry.sourceDigest().equals(artifact.sourceDigest())
                || !"STORED_UNVERIFIED".equals(artifact.state()))
            throw new SecurityException("BACKUP_REGISTRY_ARTIFACT_MISMATCH");
        return new Result(entry,artifact,false);
    }
}
