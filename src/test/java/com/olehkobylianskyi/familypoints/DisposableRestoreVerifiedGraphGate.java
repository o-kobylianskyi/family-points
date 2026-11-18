package com.olehkobylianskyi.familypoints;

import com.fasterxml.jackson.databind.JsonNode;
import com.olehkobylianskyi.familypoints.security.WorkspaceRestorePermitScopeV2;
import com.olehkobylianskyi.familypoints.security.WorkspaceRestoreSelectionClosureV2;
import java.time.Instant;
import java.util.*;

/**
 * Backend B / B-B-08. TEST ONLY: one pre-write gate combines exact C V2 scope,
 * selected category counts and bounded per-row original-family source graph.
 * There is NO signed original manifest/AES proof or real C issued owner grant.
 * Source categories and digests here are caller supplied test data.
 */
final class DisposableRestoreVerifiedGraphGate {
    record Result(String scopeDigest,int sourceMembers,int ledgerRows,
                  Map<String,Long> mappedBalances) {}
    private static final Set<String> BOUNDED=Set.of(
            "FAMILY_MEMBERS","TASK_DEFINITIONS","TASK_EXECUTIONS",
            "REWARD_DEFINITIONS","REWARD_PURCHASES","POINT_TYPES","POINT_LEDGER");
    private final DisposableRestoreScopeV2Consumer scopeVerifier=
            new DisposableRestoreScopeV2Consumer();
    private final DisposableRestoreRowClosureVerifier rowVerifier=
            new DisposableRestoreRowClosureVerifier();

    Result verify(JsonNode source,DisposableRestorePlanFingerprint.Plan plan,
            DisposableRestoreFinancialDependencyPreflight.Mapping mapping,
            Collection<WorkspaceRestoreSelectionClosureV2.Category> categories,
            WorkspaceRestorePermitScopeV2.RestoreScope purportedIssued,
            WorkspaceRestorePermitScopeV2.RestoreScope attempted,
            Instant now) {
        var fingerprint=scopeVerifier.verify(plan,categories,purportedIssued,attempted,now);
        if(source==null||!source.isObject()||mapping==null||categories==null)
            throw new SecurityException("RESTORE_GRAPH_SOURCE_INVALID");
        // Narrow FULL-graph rehearsal only. Do not silently process subset when
        // an excluded purchase could be referenced from restored point ledger.
        var considered=new HashMap<String,WorkspaceRestoreSelectionClosureV2.Category>();
        for(var category:categories)
            considered.put(category.categoryCode(),category);
        for(String required:BOUNDED) {
            var declared=considered.get(required);
            var rows=source.path(required);
            if(declared==null ||declared.decision()!=
                    WorkspaceRestoreSelectionClosureV2.Decision.RESTORE
                    ||!rows.isArray()||declared.sourceRowCount()!=rows.size())
                throw new SecurityException("RESTORE_GRAPH_REQUIRED_CATEGORY_OR_COUNT:"+required);
        }
        if(!plan.originalToTargetMembers().equals(mapping.members()))
            throw new SecurityException("RESTORE_GRAPH_PLAN_MAPPING_DRIFT");
        // Bounded original source graph: strict membership, task, purchase,
        // currency and ledger FK ancestry. Finance D still owns other events.
        var closure=rowVerifier.verify(source,plan.sourceWorkspace(),mapping);
        return new Result(fingerprint.scopeDigest(),closure.originalMembers(),
                closure.ledgerRows(),closure.mappedBalances());
    }
}
