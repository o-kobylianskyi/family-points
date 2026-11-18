package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.security.WorkspaceRestorePermitScopeV2;
import com.olehkobylianskyi.familypoints.security.WorkspaceRestoreSelectionClosureV2;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * B-B-08 test-only interop with C-C-09 exact binary canonical digest.
 * No actual owner identity, A artifact signature or durable grant exists.
 */
class DisposableRestoreScopeV2ConsumerTest {
    private static final Instant ISSUED=Instant.parse("2026-11-18T08:00:00Z");
    private static final Instant NOW=ISSUED.plusSeconds(30);
    private static final String MANIFEST="a".repeat(64);
    private static final String SNAPSHOT_DIGEST="b".repeat(64);
    private final DisposableRestoreScopeV2Consumer consumer=
            new DisposableRestoreScopeV2Consumer();

    private static DisposableRestorePlanFingerprint.Plan plan() {
        return new DisposableRestorePlanFingerprint.Plan(17,31,2,
                UUID.fromString("11111111-2222-4333-8444-555555555555"),
                MANIFEST,3,7,Map.of(4L,104L,3L,103L),
                List.of(
                    new DisposableRestorePlanFingerprint.Category("FAMILY_MEMBERS",2,true),
                    new DisposableRestorePlanFingerprint.Category("TASK_DEFINITIONS",1,true),
                    new DisposableRestorePlanFingerprint.Category("POINT_TYPES",1,true),
                    new DisposableRestorePlanFingerprint.Category("TASK_EXECUTIONS",1,true),
                    new DisposableRestorePlanFingerprint.Category("POINT_LEDGER",1,true),
                    new DisposableRestorePlanFingerprint.Category("REWARD_PURCHASES",0,false)),
                "FAIL_ON_CONFLICT");
    }

    private static List<WorkspaceRestoreSelectionClosureV2.Category> selection() {
        var list=new ArrayList<WorkspaceRestoreSelectionClosureV2.Category>();
        for(var c:plan().categories())list.add(
                new WorkspaceRestoreSelectionClosureV2.Category(c.code(),
                        c.selected()?WorkspaceRestoreSelectionClosureV2.Decision.RESTORE:
                                WorkspaceRestoreSelectionClosureV2.Decision.PRESERVE,
                        c.originalRowCount(),"c".repeat(64),"d".repeat(64),"e".repeat(64)));
        return list;
    }

    private static WorkspaceRestorePermitScopeV2.RestoreScope scope(
            DisposableRestorePlanFingerprint.Plan p,
            Collection<WorkspaceRestoreSelectionClosureV2.Category> cats) {
        var a=new WorkspaceRestorePermitScopeV2.SourceArchive(
                p.sourceWorkspace(),"VNext-"+p.schemaVersion(),p.snapshot(),
                p.sourceEpoch(),SNAPSHOT_DIGEST,p.sealedManifestSha256());
        return new WorkspaceRestorePermitScopeV2.RestoreScope(p.targetWorkspace(),
                7,8,UUID.randomUUID(),a,
                WorkspaceRestoreSelectionClosureV2.canonicalDigest(
                        a.schemaVersion(),p.sourceWorkspace(),p.targetWorkspace(),cats),
                WorkspaceRestorePermitScopeV2.canonicalMappingDigest(
                        p.sourceWorkspace(),p.targetWorkspace(),p.originalToTargetMembers()),
                p.targetExpectedEpoch(),ISSUED,ISSUED.plusSeconds(300));
    }

    @Test void exactCContractInteropAndOrderIndependence() {
        var p=plan();var c=selection();var issued=scope(p,c);
        var actual=consumer.verify(p,c,issued,issued,NOW);
        assertEquals(issued.memberMappingDigest(),actual.mappingDigest());
        assertEquals(issued.selectionClosureDigest(),actual.selectionDigest());
        assertEquals(WorkspaceRestorePermitScopeV2.canonicalScopeDigest(issued),
                actual.scopeDigest());
        Collections.reverse(c);
        assertEquals(actual,consumer.verify(p,c,issued,issued,NOW));
    }

    @Test void wrongSourceWorkspaceArchiveDigestAndEpochAreDenied() {
        var p=plan();var c=selection();var issued=scope(p,c);
        var wrongSource=new DisposableRestorePlanFingerprint.Plan(
                18,p.targetWorkspace(),p.schemaVersion(),p.snapshot(),
                p.sealedManifestSha256(),p.sourceEpoch(),p.targetExpectedEpoch(),
                p.originalToTargetMembers(),p.categories(),p.conflictPolicy());
        assertThrows(SecurityException.class,()->
                consumer.verify(wrongSource,c,issued,issued,NOW));
        var wrongManifest=new DisposableRestorePlanFingerprint.Plan(
                p.sourceWorkspace(),p.targetWorkspace(),p.schemaVersion(),p.snapshot(),
                "f".repeat(64),p.sourceEpoch(),p.targetExpectedEpoch(),
                p.originalToTargetMembers(),p.categories(),p.conflictPolicy());
        assertThrows(SecurityException.class,()->
                consumer.verify(wrongManifest,c,issued,issued,NOW));
        var wrongEpoch=new DisposableRestorePlanFingerprint.Plan(
                p.sourceWorkspace(),p.targetWorkspace(),p.schemaVersion(),p.snapshot(),
                p.sealedManifestSha256(),p.sourceEpoch(),8,
                p.originalToTargetMembers(),p.categories(),p.conflictPolicy());
        assertThrows(SecurityException.class,()->
                consumer.verify(wrongEpoch,c,issued,issued,NOW));
    }

    @Test void sameRoleMemberSwapAndSelectionDriftAreDenied() {
        var p=plan();var cats=selection();var issued=scope(p,cats);
        var swapped=new DisposableRestorePlanFingerprint.Plan(
                p.sourceWorkspace(),p.targetWorkspace(),p.schemaVersion(),p.snapshot(),
                p.sealedManifestSha256(),p.sourceEpoch(),p.targetExpectedEpoch(),
                Map.of(3L,104L,4L,103L),p.categories(),p.conflictPolicy());
        assertThrows(SecurityException.class,()->
                consumer.verify(swapped,cats,issued,issued,NOW));
        var changed=new ArrayList<>(cats);
        var previous=changed.get(0);
        changed.set(0,new WorkspaceRestoreSelectionClosureV2.Category(
                previous.categoryCode(),previous.decision(),previous.sourceRowCount()+1,
                previous.sourceCategoryDigest(),previous.dependencyClosureDigest(),
                previous.targetConflictPlanDigest()));
        var drifted=changed;
        assertThrows(SecurityException.class,()->
                consumer.verify(p,drifted,issued,issued,NOW));
        changed=new ArrayList<>(cats);
        previous=changed.get(0);
        changed.set(0,new WorkspaceRestoreSelectionClosureV2.Category(
                previous.categoryCode(),WorkspaceRestoreSelectionClosureV2.Decision.BLOCK,
                previous.sourceRowCount(),previous.sourceCategoryDigest(),
                previous.dependencyClosureDigest(),previous.targetConflictPlanDigest()));
        var blocked=changed;
        assertThrows(SecurityException.class,()->
                consumer.verify(p,blocked,issued,issued,NOW));
    }

    @Test void expiredOrUnissuedCandidateCannotAuthorize() {
        var p=plan();var cats=selection();var issued=scope(p,cats);
        var other=scope(p,cats);
        assertThrows(SecurityException.class,()->
                consumer.verify(p,cats,issued,other,NOW));
        assertThrows(SecurityException.class,()->
                consumer.verify(p,cats,issued,issued,ISSUED.plusSeconds(300)));
    }
}
