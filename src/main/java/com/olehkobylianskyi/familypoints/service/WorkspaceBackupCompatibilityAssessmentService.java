package com.olehkobylianskyi.familypoints.service;

import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * B-NEXT-01 internal, READ-ONLY version-neutral compatibility view.
 * Does not create a RESTORE permit, apply an archive, promote a registry row,
 * reveal ciphertext/storage keys, or expose an executable HTTP action.
 *
 * The V1 inspector and authenticated V2 inspector remain the sole sources of
 * truth for their own wire formats. An unknown version is NEVER guessed.
 */
@Service
public class WorkspaceBackupCompatibilityAssessmentService {
    public enum SourceKind { REGISTRY_METADATA, PORTABLE_V1, ORIGINAL_ANCESTRY_V2 }
    public enum Status { METADATA_ONLY, AUTHENTICATED_READ_ONLY, BLOCKED }

    public record Assessment(SourceKind kind,Status status,UUID snapshotId,
            long workspaceId,long sourceEpoch,SortedMap<String,Integer> categories,
            List<String> blockers,boolean restoreVerified,boolean executable) {
        public Assessment {
            categories=Collections.unmodifiableSortedMap(new TreeMap<>(categories));
            blockers=List.copyOf(blockers);
            if(executable) throw new SecurityException("BACKUP_COMPAT_EXECUTE_UNSUPPORTED");
        }
    }

    private final ProtectiveBackupImportInspector inspector;
    public WorkspaceBackupCompatibilityAssessmentService(ProtectiveBackupImportInspector inspector) {
        this.inspector=Objects.requireNonNull(inspector);
    }

    public Assessment registry(WorkspaceProtectiveBackupService.Backup backup,
            long authorizedWorkspace,OffsetDateTime observedAt) {
        if(backup==null||observedAt==null||authorizedWorkspace<=0
                ||backup.workspaceId()!=authorizedWorkspace)
            throw new SecurityException("BACKUP_COMPAT_REGISTRY_TENANT_INVALID");
        if(backup.snapshotId()==null || backup.operationId()==null
                ||backup.createdAt()==null||backup.expiresAt()==null
                ||backup.selectedCategories()==null
                ||backup.selectedCategories().isEmpty()
                ||!Set.of("FULL","SELECTIVE").contains(backup.resetMode())
                ||backup.dataEpoch()<0
                ||backup.state()==null)
            throw new SecurityException("BACKUP_COMPAT_REGISTRY_MALFORMED");
        if(backup.executable())
            throw new SecurityException("BACKUP_COMPAT_REGISTRY_EXECUTE_FLAG_INVALID");
        if(new HashSet<>(backup.selectedCategories()).size()!=backup.selectedCategories().size()
                ||backup.selectedCategories().stream().anyMatch(
                        n->n==null||!n.matches("[A-Z][A-Z0-9_]{0,79}")))
            throw new SecurityException("BACKUP_COMPAT_REGISTRY_CATEGORIES_INVALID");
        var counts=new TreeMap<String,Integer>();
        backup.selectedCategories().forEach(n->counts.put(n,-1)); // registry never proves row counts
        var reasons=new TreeSet<String>();
        reasons.add("REGISTRY_NOT_AUTHENTICATED_SOURCE");
        if(!backup.expiresAt().isAfter(observedAt))
            reasons.add("BACKUP_REGISTRY_EXPIRED");
        if(!"VERIFIED".equals(backup.state()))
            reasons.add("BACKUP_REGISTRY_NOT_VERIFIED");
        // Even VERIFIED metadata does NOT attest an original-owner grant or
        // make an executable Restore: source bytes must be inspected separately.
        reasons.add("RESTORE_EXECUTION_DISABLED");
        return new Assessment(SourceKind.REGISTRY_METADATA,Status.METADATA_ONLY,
                backup.snapshotId(),authorizedWorkspace,backup.dataEpoch(),
                counts,List.copyOf(reasons),backup.restoreVerified(),false);
    }

    /** Legacy V1 remains inspectable without implying a V2 identity/owner proof. */
    public Assessment inspectLegacyV1(byte[] encrypted,long workspace,SecretKey key) {
        if(workspace<=0||encrypted==null||key==null)
            throw new SecurityException("BACKUP_COMPAT_V1_INPUT_INVALID");
        var legacy=inspector.inspect(encrypted,workspace,key);
        if(legacy.workspaceId()!=workspace || legacy.snapshotId()==null
                ||legacy.sourceEpoch()<0)
            throw new SecurityException("BACKUP_COMPAT_V1_PROVENANCE_INVALID");
        var reasons=new TreeSet<>(legacy.blockers());
        reasons.add("LEGACY_V1_OWNER_PROOF_UNAVAILABLE");
        reasons.add("LEGACY_V1_NATIVE_REPLAY_UNSUPPORTED");
        reasons.add("RESTORE_EXECUTION_DISABLED");
        return new Assessment(SourceKind.PORTABLE_V1,Status.BLOCKED,
                legacy.snapshotId(),workspace,legacy.sourceEpoch(),
                new TreeMap<>(legacy.counts()),List.copyOf(reasons),false,false);
    }

    /** V2 is authenticated offline, but cannot itself authorize target Restore. */
    public Assessment inspectOriginalV2(byte[] encrypted,long sourceWorkspace,SecretKey key) {
        if(sourceWorkspace<=0||encrypted==null||key==null)
            throw new SecurityException("BACKUP_COMPAT_V2_INPUT_INVALID");
        var checked=inspector.inspectAncestryV2(encrypted,sourceWorkspace,key);
        if(checked.sourceWorkspaceId()!=sourceWorkspace||checked.snapshotId()==null
                ||checked.sourceEpoch()<0
                ||checked.verifiedCategoryCounts().size()!=8)
            throw new SecurityException("BACKUP_COMPAT_V2_PROVENANCE_INVALID");
        return new Assessment(SourceKind.ORIGINAL_ANCESTRY_V2,
                Status.AUTHENTICATED_READ_ONLY,checked.snapshotId(),
                sourceWorkspace,checked.sourceEpoch(),
                new TreeMap<>(checked.verifiedCategoryCounts()),
                List.of("TARGET_ORIGINAL_OWNER_AND_ONE_USE_PERMIT_REQUIRED",
                        "TARGET_MEMBER_MAPPING_REQUIRED",
                        "RESTORE_EXECUTION_DISABLED"),false,false);
    }
}
