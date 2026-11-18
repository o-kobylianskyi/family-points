package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.security.WorkspaceDestructivePermitContract;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Backend B / B-B-07. TEST ONLY: deterministic typed restore-consumer scope
 * fingerprint. This is NOT a signed manifest, identity assertion, credential,
 * durable approval, complete selection dependency validator or Restore API.
 */
final class DisposableRestorePlanFingerprint {
    record Category(String code,long originalRowCount,boolean selected) {}
    record Plan(long sourceWorkspace,long targetWorkspace,int schemaVersion,UUID snapshot,
                String sealedManifestSha256,long sourceEpoch,long targetExpectedEpoch,
                Map<Long,Long> originalToTargetMembers,List<Category> categories,
                String conflictPolicy) {}
    record Fingerprints(String memberMappingDigest,String selectionDigest,
                        String planDigest) {}
    private static final Set<String> ALLOWED_POLICIES=Set.of("FAIL_ON_CONFLICT");
    private static final Set<String> ALLOWED_CATEGORIES=Set.of(
            "PRIVATE_TEMPLATES","MEMBER_GROUPS","TASK_DEFINITIONS",
            "REWARD_DEFINITIONS","TASK_EXECUTIONS","REWARD_PURCHASES",
            "TASK_SCHEDULE_SLOTS","REWARD_REQUESTS","REWARD_REQUIREMENTS",
            "REWARD_CATEGORIES","REWARD_OBLIGATIONS",
            "REWARD_OBLIGATION_BLOCKED_CATEGORIES",
            "REWARD_OBLIGATION_BLOCKED_REWARDS",
            "REWARD_DEFINITION_CATEGORIES","WORK_NODES","MEDIA_BINDINGS",
            "MEDIA_ASSETS","GROUP_POINT_LEDGER","POINT_TYPES",
            "POINT_EXCHANGE_RATES","POINT_EXCHANGE_REQUESTS",
            "POINT_LEDGER","REPUTATION_LEDGER","FAMILY_MEMBERS");

    Fingerprints calculate(Plan plan) {
        if(plan==null || plan.sourceWorkspace()<=0||plan.targetWorkspace()<=0
                ||plan.schemaVersion()<=0 ||plan.snapshot()==null
                ||plan.sourceEpoch()<0||plan.targetExpectedEpoch()<0
                ||!digestValid(plan.sealedManifestSha256())
                ||!ALLOWED_POLICIES.contains(plan.conflictPolicy())
                ||plan.originalToTargetMembers()==null
                ||plan.originalToTargetMembers().isEmpty()
                ||plan.categories()==null||plan.categories().isEmpty())
            throw new SecurityException("RESTORE_PLAN_INVALID");
        var sorted=new TreeMap<Long,Long>();
        var dest=new HashSet<Long>();
        for(var member:plan.originalToTargetMembers().entrySet()) {
            if(member.getKey()==null||member.getKey()<=0||member.getValue()==null
                    ||member.getValue()<=0||!dest.add(member.getValue()))
                throw new SecurityException("RESTORE_PLAN_MEMBER_MAP_INVALID");
            sorted.put(member.getKey(),member.getValue());
        }
        var cats=new TreeMap<String,Category>();
        int selected=0;
        for(Category cat:plan.categories()) {
            if(cat==null||!ALLOWED_CATEGORIES.contains(cat.code())
                    ||cat.originalRowCount()<0
                    ||cats.putIfAbsent(cat.code(),cat)!=null)
                throw new SecurityException("RESTORE_PLAN_CATEGORY_INVALID");
            if(cat.selected())selected++;
        }
        if(selected==0)
            throw new SecurityException("RESTORE_PLAN_EMPTY_SELECTION");
        // A vNext hidden source member ancestry is mandatory for every
        // category that references an original source person. This does not
        // authorize creating that person in the target workspace.
        if(plan.schemaVersion()<2)
            throw new SecurityException("RESTORE_PLAN_LEGACY_NOT_RESTORABLE");
        Set<String> needsMember=Set.of("TASK_EXECUTIONS","REWARD_PURCHASES",
                "REWARD_REQUESTS","REWARD_OBLIGATIONS","POINT_LEDGER",
                "REPUTATION_LEDGER","POINT_EXCHANGE_REQUESTS","MEDIA_BINDINGS");
        if(needsMember.stream().anyMatch(c->selected(cats,c))
                && !selected(cats,"FAMILY_MEMBERS"))
            throw new SecurityException("RESTORE_PLAN_MEMBER_CATALOG_REQUIRED");
        if((selected(cats,"POINT_LEDGER")||selected(cats,"REWARD_PURCHASES")
                ||selected(cats,"POINT_EXCHANGE_REQUESTS")
                ||selected(cats,"POINT_EXCHANGE_RATES")
                ||selected(cats,"GROUP_POINT_LEDGER"))
                && !selected(cats,"POINT_TYPES"))
            throw new SecurityException("RESTORE_PLAN_POINT_TYPES_REQUIRED");
        if(selected(cats,"TASK_EXECUTIONS")&&!selected(cats,"TASK_DEFINITIONS"))
            throw new SecurityException("RESTORE_PLAN_TASK_DEFINITIONS_REQUIRED");
        if(selected(cats,"REWARD_PURCHASES")&&!selected(cats,"REWARD_DEFINITIONS"))
            throw new SecurityException("RESTORE_PLAN_REWARD_DEFINITIONS_REQUIRED");
        StringBuilder members=new StringBuilder("FP-RESTORE-MEMBERS-V2")
                .append('|').append(plan.sourceWorkspace())
                .append('|').append(plan.targetWorkspace())
                .append('|').append(sorted.size());
        for(var mapping:sorted.entrySet())
            members.append('|').append(mapping.getKey()).append(':').append(mapping.getValue());
        String mappingDigest=sha(members.toString());
        var categories=new StringBuilder("FP-RESTORE-SELECTION-V2")
                .append('|').append(plan.sourceWorkspace())
                .append('|').append(plan.targetWorkspace())
                .append('|').append(plan.schemaVersion())
                .append('|').append(plan.conflictPolicy())
                .append('|').append(cats.size());
        for(var cat:cats.values())categories.append('|').append(cat.code())
                .append(':').append(cat.originalRowCount())
                .append(':').append(cat.selected()?'1':'0');
        String selectionDigest=sha(categories.toString());
        String entire="FP-RESTORE-PLAN-V2|"+plan.sourceWorkspace()+'|'
                +plan.targetWorkspace()+'|'+plan.schemaVersion()+'|'+plan.snapshot()
                +'|'+plan.sealedManifestSha256()+'|'+plan.sourceEpoch()+'|'
                +plan.targetExpectedEpoch()+'|'+mappingDigest+'|'+selectionDigest;
        return new Fingerprints(mappingDigest,selectionDigest,sha(entire));
    }

    /**
     * Compares digests with C's immutable action-specific scope descriptor.
     * This does NOT prove authenticated issuer/owner, actual token possession,
     * signed source identity or one-time DB consumption.
     */
    void requireMatchingScope(Plan plan,Fingerprints expected,
            WorkspaceDestructivePermitContract.Scope candidate,
            WorkspaceDestructivePermitContract.Scope issued,java.time.Instant now) {
        Fingerprints actual=calculate(plan);
        if(!actual.equals(expected)||candidate==null||issued==null
                ||issued.action()!=WorkspaceDestructivePermitContract.Action.RESTORE
                ||!issued.matches(candidate,now)
                ||candidate.workspaceId()!=plan.targetWorkspace()
                ||!candidate.snapshotId().equals(plan.snapshot())
                ||!candidate.snapshotDigest().equals(plan.sealedManifestSha256())
                ||!candidate.selectionDigest().equals(actual.selectionDigest())
                ||!candidate.memberMappingDigest().equals(actual.memberMappingDigest())
                ||candidate.dataEpoch()!=plan.targetExpectedEpoch())
            throw new SecurityException("RESTORE_PLAN_SCOPE_MISMATCH");
    }

    private static boolean selected(Map<String,Category> cats,String code) {
        Category c=cats.get(code);
        return c!=null&&c.selected();
    }
    private static boolean digestValid(String v) {
        return v!=null&&v.matches("[0-9a-f]{64}");
    }
    private static String sha(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        }catch(NoSuchAlgorithmException ex){throw new IllegalStateException(ex);}
    }
}
