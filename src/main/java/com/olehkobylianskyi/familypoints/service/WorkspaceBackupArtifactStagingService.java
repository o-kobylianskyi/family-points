package com.olehkobylianskyi.familypoints.service;

import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.*;

/**
 * Internal, non-executable portable-backup staging workflow.
 *
 * An export comes from a locked tenant-scoped database snapshot. The encrypted
 * file is atomically stored and then read back by SHA-256. Authenticated archive
 * inspection proves that the bytes match the requested scope, but NOT that
 * restoration of the database will succeed. This workflow deliberately DOES NOT
 * promote the protective-backup registry to VERIFIED, enable any reset/restore
 * operation, or expose user-facing HTTP upload/download.
 *
 * Files written just before a failure can remain orphaned; a future registry
 * coordinator must reconcile them rather than assuming database rollback will
 * roll back filesystem changes.
 */
@Service
public class WorkspaceBackupArtifactStagingService {
    private final WorkspacePortableBackupExporter exporter;
    private final WorkspaceBackupLocalArtifactStore storage;
    private final ProtectiveBackupImportInspector inspector;

    public WorkspaceBackupArtifactStagingService(WorkspacePortableBackupExporter exporter,
            WorkspaceBackupLocalArtifactStore storage,
            ProtectiveBackupImportInspector inspector) {
        this.exporter=exporter;
        this.storage=storage;
        this.inspector=inspector;
    }

    public record Staged(UUID snapshotId,long workspaceId,String sourceDigest,
            String storageSha256,int storedBytes,Map<String,Integer> counts,
            String state) {}

    public Staged stage(long workspaceId,UUID snapshotId,UUID operationId,
            long expectedEpoch,Collection<String> selectedCategories,
            String schemaVersion,SecretKey key) {
        if(workspaceId<=0 || snapshotId==null || operationId==null
                || expectedEpoch<0 || selectedCategories==null
                || selectedCategories.isEmpty()
                || selectedCategories.stream().anyMatch(Objects::isNull)
                || selectedCategories.size()!=new HashSet<>(selectedCategories).size())
            throw new IllegalArgumentException("INVALID_BACKUP_STAGING_REQUEST");

        var ordered=new TreeSet<>(selectedCategories);
        var exported=exporter.export(workspaceId,snapshotId,operationId,
                expectedEpoch,ordered,schemaVersion,key);
        byte[] file=exported.encryptedFile();
        if(file==null || file.length==0)
            throw new IllegalStateException("EMPTY_ENCRYPTED_EXPORT");
        try {
            // Verify the export against the authenticated schema BEFORE storing.
            var proof=inspector.inspect(file,workspaceId,key);
            ensureExactExport(proof,snapshotId,expectedEpoch,schemaVersion,
                    ordered,exported.recordCounts());
            var stored=storage.store(workspaceId,snapshotId,file);
            byte[] reread=storage.read(workspaceId,snapshotId,stored.sha256());
            try {
                if(!Arrays.equals(file,reread))
                    throw new SecurityException("BACKUP_LOCAL_ROUNDTRIP_MISMATCH");
                var checked=inspector.inspect(reread,workspaceId,key);
                ensureExactExport(checked,snapshotId,expectedEpoch,schemaVersion,
                        ordered,exported.recordCounts());
            } finally {Arrays.fill(reread,(byte)0);}
            // Rehearsal and protected registry state are intentionally separate
            // and mandatory before a backup may be considered recoverable.
            return new Staged(snapshotId,workspaceId,exported.contentDigest(),
                    stored.sha256(),stored.byteCount(),
                    Map.copyOf(exported.recordCounts()),"STORED_UNVERIFIED");
        } finally {Arrays.fill(file,(byte)0);}
    }

    private static void ensureExactExport(
            ProtectiveBackupImportInspector.Inspection proof,UUID snapshotId,
            long expectedEpoch,String schemaVersion,Set<String> expected,
            Map<String,Integer> counts) {
        if(!proof.snapshotId().equals(snapshotId)
                || proof.sourceEpoch()!=expectedEpoch
                || !proof.schemaVersion().equals(schemaVersion)
                || !proof.availableCategories().equals(List.copyOf(expected))
                || !proof.counts().equals(counts))
            throw new SecurityException("BACKUP_STAGING_EXPORT_MISMATCH");
        // Inspector.restorable() remains false until a genuine restore engine
        // and full FK/financial protected-snapshot verification exist.
    }
}
