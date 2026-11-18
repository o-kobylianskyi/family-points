package com.olehkobylianskyi.familypoints;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.olehkobylianskyi.familypoints.security.WorkspaceRestorePermitScopeV2;
import com.olehkobylianskyi.familypoints.security.WorkspaceRestoreSelectionClosureV2;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Test-only bounded source graph + exact immutable C V2 descriptor. */
class DisposableRestoreVerifiedGraphGateTest {
    private static final ObjectMapper JSON=new ObjectMapper();
    private static final Instant AT=Instant.parse("2026-11-18T08:00:30Z");
    private static final String SOURCE="""
       {
         "FAMILY_MEMBERS":[
           {"id":3,"family_id":17,"family_role_id":6,"member_type":"PARENT",
            "active":true,"deleted_at":null}],
         "TASK_DEFINITIONS":[{"id":11,"family_id":17}],
         "TASK_EXECUTIONS":[{"id":21,"member_id":3,"task_definition_id":11}],
         "REWARD_DEFINITIONS":[{"id":31,"family_id":17}],
         "REWARD_PURCHASES":[{"id":41,"member_id":3,"reward_definition_id":31,
                              "point_type_id":51,"price_amount":4}],
         "POINT_TYPES":[{"id":51,"family_id":17,"code":"POINTS"}],
         "POINT_LEDGER":[
           {"id":61,"member_id":3,"point_type_id":51,"amount":5,"type":"EARN",
            "source_type":"TASK","source_id":21},
           {"id":62,"member_id":3,"point_type_id":51,"amount":-4,"type":"SPEND",
            "source_type":"REWARD_PURCHASE","source_id":41},
           {"id":63,"member_id":3,"point_type_id":51,"amount":2,"type":"EARN",
            "source_type":"MANUAL","source_id":null}]
       }
       """;
    private static final DisposableRestoreVerifiedGraphGate GATE=
            new DisposableRestoreVerifiedGraphGate();
    private static ObjectNode json() throws Exception {return (ObjectNode)JSON.readTree(SOURCE);}
    private static DisposableRestoreFinancialDependencyPreflight.Mapping mapping() {
        return new DisposableRestoreFinancialDependencyPreflight.Mapping(
                Map.of(3L,103L),Map.of(51L,151L),Map.of(11L,111L),
                Map.of(31L,131L),Map.of(41L,141L));
    }
    private static DisposableRestorePlanFingerprint.Plan plan() {
        var c=new ArrayList<DisposableRestorePlanFingerprint.Category>();
        for(String name:List.of("FAMILY_MEMBERS","TASK_DEFINITIONS","TASK_EXECUTIONS",
                "REWARD_DEFINITIONS","REWARD_PURCHASES","POINT_TYPES","POINT_LEDGER")) {
            long size=switch(name) {
                case "POINT_LEDGER"->3L;
                default->1L;
            };
            c.add(new DisposableRestorePlanFingerprint.Category(name,size,true));
        }
        return new DisposableRestorePlanFingerprint.Plan(17,31,2,
                UUID.fromString("11111111-2222-4333-8444-555555555555"),
                "a".repeat(64),4,0,Map.of(3L,103L),c,"FAIL_ON_CONFLICT");
    }
    private static List<WorkspaceRestoreSelectionClosureV2.Category> categories() {
        var categories=new ArrayList<WorkspaceRestoreSelectionClosureV2.Category>();
        for(var c:plan().categories())
            categories.add(new WorkspaceRestoreSelectionClosureV2.Category(
                    c.code(),WorkspaceRestoreSelectionClosureV2.Decision.RESTORE,
                    c.originalRowCount(),"b".repeat(64),"c".repeat(64),"d".repeat(64)));
        return categories;
    }
    private static WorkspaceRestorePermitScopeV2.RestoreScope scope(
            DisposableRestorePlanFingerprint.Plan p,
            Collection<WorkspaceRestoreSelectionClosureV2.Category> c) {
        var a=new WorkspaceRestorePermitScopeV2.SourceArchive(
                p.sourceWorkspace(),"VNext-2",p.snapshot(),p.sourceEpoch(),
                "e".repeat(64),p.sealedManifestSha256());
        return new WorkspaceRestorePermitScopeV2.RestoreScope(
                p.targetWorkspace(),7,8,UUID.randomUUID(),a,
                WorkspaceRestoreSelectionClosureV2.canonicalDigest(
                        a.schemaVersion(),p.sourceWorkspace(),p.targetWorkspace(),c),
                WorkspaceRestorePermitScopeV2.canonicalMappingDigest(
                        p.sourceWorkspace(),p.targetWorkspace(),p.originalToTargetMembers()),
                p.targetExpectedEpoch(),AT.minusSeconds(30),AT.plusSeconds(270));
    }

    @Test void exactScopeAndEveryOriginalParentMustBePresent() throws Exception {
        var p=plan();var categories=categories();var issued=scope(p,categories);
        var result=GATE.verify(json(),p,mapping(),categories,issued,issued,AT);
        assertEquals(1,result.sourceMembers());
        assertEquals(3,result.ledgerRows());
        assertEquals(Map.of("103:151",3L),result.mappedBalances());
    }

    @Test void silentMemberRosterRemovalOrSourceTaskParentDriftDenied()
            throws Exception {
        var p=plan();var c=categories();var s=scope(p,c);
        var removed=json();
        removed.remove("FAMILY_MEMBERS");
        assertThrows(SecurityException.class,()->GATE.verify(
                removed,p,mapping(),c,s,s,AT));
        var foreign=json();
        ((ObjectNode)foreign.path("FAMILY_MEMBERS").get(0))
                .put("family_id",18);
        assertThrows(SecurityException.class,()->GATE.verify(
                foreign,p,mapping(),c,s,s,AT));
        var dangling=json();
        ((ObjectNode)dangling.path("TASK_EXECUTIONS").get(0))
                .put("task_definition_id",999);
        assertThrows(SecurityException.class,()->GATE.verify(
                dangling,p,mapping(),c,s,s,AT));
    }

    @Test void alteredCountsSelectionAndUnapprovedMappingDenied() throws Exception {
        var p=plan();var categories=categories();var s=scope(p,categories);
        var drifted=new ArrayList<>(categories);
        var first=drifted.get(0);
        drifted.set(0,new WorkspaceRestoreSelectionClosureV2.Category(
                first.categoryCode(),first.decision(),first.sourceRowCount()+1,
                first.sourceCategoryDigest(),first.dependencyClosureDigest(),
                first.targetConflictPlanDigest()));
        assertThrows(SecurityException.class,()->GATE.verify(
                json(),p,mapping(),drifted,s,s,AT));
        var partial=new ArrayList<>(categories);
        var item=partial.get(0);
        partial.set(0,new WorkspaceRestoreSelectionClosureV2.Category(
                item.categoryCode(),WorkspaceRestoreSelectionClosureV2.Decision.PRESERVE,
                item.sourceRowCount(),item.sourceCategoryDigest(),
                item.dependencyClosureDigest(),item.targetConflictPlanDigest()));
        var attempted=scope(p,partial);
        assertThrows(SecurityException.class,()->GATE.verify(
                json(),p,mapping(),partial,attempted,attempted,AT));
        var wrong=new DisposableRestoreFinancialDependencyPreflight.Mapping(
                Map.of(3L,999L),mapping().currencies(),mapping().tasks(),
                mapping().rewards(),mapping().purchases());
        assertThrows(SecurityException.class,()->GATE.verify(
                json(),p,wrong,categories,s,s,AT));
    }

    @Test void pointSourceCannotReferenceUnselectedPurchaseOrUnsupportedRefund()
            throws Exception {
        var p=plan();var c=categories();var s=scope(p,c);
        var orphan=json();
        ((ObjectNode)orphan.path("POINT_LEDGER").get(1)).put("source_id",999);
        assertThrows(SecurityException.class,()->GATE.verify(
                orphan,p,mapping(),c,s,s,AT));
        var refund=json();
        ((ObjectNode)refund.path("POINT_LEDGER").get(1))
                .put("source_type","REFUND");
        assertThrows(SecurityException.class,()->GATE.verify(
                refund,p,mapping(),c,s,s,AT));
    }
}
