package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.security.WorkspaceDestructivePermitContract;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Test-only canonical RESTORE scope / integrity contract; no permit minted. */
class DisposableRestorePlanFingerprintTest {
    private final DisposableRestorePlanFingerprint verifier=new DisposableRestorePlanFingerprint();
    private static final UUID SNAPSHOT=UUID.fromString("e14a4b37-78f1-4c0f-9a36-3115e28b702d");
    private static final String MANIFEST="a".repeat(64);
    private static final Instant ISSUED=Instant.parse("2026-11-18T08:00:00Z");
    private static final Instant NOW=ISSUED.plusSeconds(30);

    private static DisposableRestorePlanFingerprint.Plan plan(
            Map<Long,Long> mappings,List<DisposableRestorePlanFingerprint.Category> categories) {
        return new DisposableRestorePlanFingerprint.Plan(17,31,2,SNAPSHOT,MANIFEST,7,4,
                mappings,categories,"FAIL_ON_CONFLICT");
    }
    private static List<DisposableRestorePlanFingerprint.Category> categories() {
        return List.of(new DisposableRestorePlanFingerprint.Category("POINT_TYPES",2,true),
                new DisposableRestorePlanFingerprint.Category("TASK_EXECUTIONS",5,true),
                new DisposableRestorePlanFingerprint.Category("POINT_LEDGER",9,true),
                new DisposableRestorePlanFingerprint.Category("REWARD_PURCHASES",3,false));
    }
    private static DisposableRestorePlanFingerprint.Plan normal() {
        return plan(Map.of(3L,103L,4L,104L),categories());
    }
    private static WorkspaceDestructivePermitContract.Scope scope(
            DisposableRestorePlanFingerprint.Plan plan,
            DisposableRestorePlanFingerprint.Fingerprints hash) {
        return new WorkspaceDestructivePermitContract.Scope(
                WorkspaceDestructivePermitContract.Action.RESTORE,
                UUID.randomUUID(),plan.targetWorkspace(),8,103,plan.snapshot(),
                plan.sealedManifestSha256(),hash.selectionDigest(),
                hash.memberMappingDigest(),plan.targetExpectedEpoch(),
                ISSUED,ISSUED.plusSeconds(300));
    }
    @Test void sortedMapAndCategoryOrderDoNotChangeCanonicalBytes() {
        var a=normal();
        var permuted=new LinkedHashMap<Long,Long>();
        permuted.put(4L,104L);
        permuted.put(3L,103L);
        var categories=new ArrayList<>(categories());
        Collections.reverse(categories);
        assertEquals(verifier.calculate(a),verifier.calculate(plan(permuted,categories)));
    }
    @Test void changingEveryProtectedDimensionChangesPlanDigest() {
        var p=normal();
        var a=verifier.calculate(p);
        var changed=List.of(
                new DisposableRestorePlanFingerprint.Plan(18,31,2,SNAPSHOT,MANIFEST,7,4,p.originalToTargetMembers(),p.categories(),p.conflictPolicy()),
                new DisposableRestorePlanFingerprint.Plan(17,32,2,SNAPSHOT,MANIFEST,7,4,p.originalToTargetMembers(),p.categories(),p.conflictPolicy()),
                new DisposableRestorePlanFingerprint.Plan(17,31,3,SNAPSHOT,MANIFEST,7,4,p.originalToTargetMembers(),p.categories(),p.conflictPolicy()),
                new DisposableRestorePlanFingerprint.Plan(17,31,2,UUID.randomUUID(),MANIFEST,7,4,p.originalToTargetMembers(),p.categories(),p.conflictPolicy()),
                new DisposableRestorePlanFingerprint.Plan(17,31,2,SNAPSHOT,"b".repeat(64),7,4,p.originalToTargetMembers(),p.categories(),p.conflictPolicy()),
                new DisposableRestorePlanFingerprint.Plan(17,31,2,SNAPSHOT,MANIFEST,8,4,p.originalToTargetMembers(),p.categories(),p.conflictPolicy()),
                new DisposableRestorePlanFingerprint.Plan(17,31,2,SNAPSHOT,MANIFEST,7,5,p.originalToTargetMembers(),p.categories(),p.conflictPolicy()),
                plan(Map.of(3L,104L,4L,103L),p.categories()),
                plan(p.originalToTargetMembers(),List.of(
                        new DisposableRestorePlanFingerprint.Category("POINT_TYPES",2,true),
                        new DisposableRestorePlanFingerprint.Category("TASK_EXECUTIONS",5,true),
                        new DisposableRestorePlanFingerprint.Category("POINT_LEDGER",10,true)))
        );
        for(var change:changed)assertNotEquals(a.planDigest(),
                verifier.calculate(change).planDigest(),change.toString());
    }
    @Test void forgedOrIncompleteScopeAndExpiredOrWrongActionDenied() {
        var p=normal();
        var hash=verifier.calculate(p);
        var issued=scope(p,hash);
        verifier.requireMatchingScope(p,hash,issued,issued,NOW);
        var other=scope(p,hash);
        assertThrows(SecurityException.class,()->verifier.requireMatchingScope(
                p,hash,other,issued,NOW));
        assertThrows(SecurityException.class,()->verifier.requireMatchingScope(
                p,hash,issued,issued,ISSUED.plusSeconds(300)));
        var wrongHash=new DisposableRestorePlanFingerprint.Fingerprints(
                "f".repeat(64),hash.selectionDigest(),hash.planDigest());
        assertThrows(SecurityException.class,()->verifier.requireMatchingScope(
                p,wrongHash,issued,issued,NOW));
        var reset=new WorkspaceDestructivePermitContract.Scope(
                WorkspaceDestructivePermitContract.Action.RESET,issued.operationId(),
                issued.workspaceId(),issued.ownerAccountId(),issued.ownerMemberId(),
                issued.snapshotId(),issued.snapshotDigest(),issued.selectionDigest(),
                null,issued.dataEpoch(),issued.issuedAt(),issued.expiresAt());
        assertThrows(SecurityException.class,()->verifier.requireMatchingScope(
                p,hash,reset,reset,NOW));
    }
    @Test void duplicateIdsValuesUnknownCategoriesAndEmptySelectionDenied() {
        var duplicate=plan(Map.of(3L,103L,4L,103L),categories());
        assertThrows(SecurityException.class,()->verifier.calculate(duplicate));
        var unknown=plan(Map.of(3L,103L),List.of(
                new DisposableRestorePlanFingerprint.Category("PASSWORDS",1,true)));
        assertThrows(SecurityException.class,()->verifier.calculate(unknown));
        var empty=plan(Map.of(3L,103L),List.of(
                new DisposableRestorePlanFingerprint.Category("POINT_LEDGER",1,false)));
        assertThrows(SecurityException.class,()->verifier.calculate(empty));
        var repeated=plan(Map.of(3L,103L),List.of(
                new DisposableRestorePlanFingerprint.Category("POINT_LEDGER",1,true),
                new DisposableRestorePlanFingerprint.Category("POINT_LEDGER",1,true)));
        assertThrows(SecurityException.class,()->verifier.calculate(repeated));
        var negative=plan(Map.of(3L,103L),List.of(
                new DisposableRestorePlanFingerprint.Category("POINT_LEDGER",-1,true)));
        assertThrows(SecurityException.class,()->verifier.calculate(negative));
    }
}
