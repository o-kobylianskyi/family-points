package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.security.WorkspaceRestorePermitScopeV2;
import com.olehkobylianskyi.familypoints.security.WorkspaceRestoreSelectionClosureV2;
import java.time.Instant;
import java.util.*;

/**
 * Backend B / B-B-08: TEST ONLY cross-contract adapter to Backend C's canonical
 * binary RESTORE V2 digests. Caller-provided scopes are NOT authenticated
 * owner grants; source-category digests are NOT authenticated A manifest data.
 */
final class DisposableRestoreScopeV2Consumer {
    record CheckedScope(String mappingDigest,String selectionDigest,String scopeDigest) {}

    CheckedScope verify(DisposableRestorePlanFingerprint.Plan plan,
            Collection<WorkspaceRestoreSelectionClosureV2.Category> selections,
            WorkspaceRestorePermitScopeV2.RestoreScope purportedIssued,
            WorkspaceRestorePermitScopeV2.RestoreScope attempted,Instant now) {
        new DisposableRestorePlanFingerprint().calculate(plan);
        if(purportedIssued==null||attempted==null||now==null
                ||!purportedIssued.matches(attempted,now))
            throw new SecurityException("RESTORE_SCOPE_V2_NOT_MATCHING");
        var archive=attempted.source();
        if(archive.sourceWorkspaceId()!=plan.sourceWorkspace()
                ||!archive.schemaVersion().equals("VNext-"+plan.schemaVersion())
                ||!archive.snapshotId().equals(plan.snapshot())
                ||archive.sourceSnapshotEpoch()!=plan.sourceEpoch()
                ||!archive.manifestRootDigest().equals(plan.sealedManifestSha256())
                ||attempted.targetWorkspaceId()!=plan.targetWorkspace()
                ||attempted.targetExpectedEpoch()!=plan.targetExpectedEpoch())
            throw new SecurityException("RESTORE_SCOPE_V2_SOURCE_OR_EPOCH_MISMATCH");
        var manifestCategories=new TreeMap<String,DisposableRestorePlanFingerprint.Category>();
        for(var p:plan.categories())
            if(manifestCategories.putIfAbsent(p.code(),p)!=null)
                throw new SecurityException("RESTORE_SCOPE_V2_CATEGORY_DUPLICATE");
        var supplied=new HashSet<String>();
        if(selections==null||selections.size()!=manifestCategories.size())
            throw new SecurityException("RESTORE_SCOPE_V2_CATEGORY_COUNT");
        for(var entry:selections) {
            if(entry==null||!supplied.add(entry.categoryCode()))
                throw new SecurityException("RESTORE_SCOPE_V2_CATEGORY_DUPLICATE");
            var p=manifestCategories.get(entry.categoryCode());
            if(p==null||entry.sourceRowCount()!=p.originalRowCount()
                    ||entry.decision()==WorkspaceRestoreSelectionClosureV2.Decision.BLOCK
                    ||p.selected()!=(entry.decision()==WorkspaceRestoreSelectionClosureV2.Decision.RESTORE))
                throw new SecurityException("RESTORE_SCOPE_V2_SELECTION_CONFLICT");
        }
        String mapping=WorkspaceRestorePermitScopeV2.canonicalMappingDigest(
                plan.sourceWorkspace(),plan.targetWorkspace(),
                plan.originalToTargetMembers());
        String categories=WorkspaceRestoreSelectionClosureV2.canonicalDigest(
                archive.schemaVersion(),plan.sourceWorkspace(),plan.targetWorkspace(),
                selections);
        if(!mapping.equals(attempted.memberMappingDigest())
                ||!categories.equals(attempted.selectionClosureDigest()))
            throw new SecurityException("RESTORE_SCOPE_V2_DIGEST_MISMATCH");
        return new CheckedScope(mapping,categories,
                WorkspaceRestorePermitScopeV2.canonicalScopeDigest(attempted));
    }
}
