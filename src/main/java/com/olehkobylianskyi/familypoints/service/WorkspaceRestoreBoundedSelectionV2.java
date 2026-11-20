package com.olehkobylianskyi.familypoints.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.olehkobylianskyi.familypoints.security.WorkspaceRestoreSelectionClosureV2;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Reuses C's existing binary selection closure algorithm for the one bounded
 * eight-category operation. Roles/members are verified identity dependencies
 * and intentionally NOT overwritten. Six finance/task categories are replayed.
 * Any other category must be an explicit BLOCK at source export.
 */
public final class WorkspaceRestoreBoundedSelectionV2 {
    private WorkspaceRestoreBoundedSelectionV2() {}
    private static final Set<String> REPLAY=Set.of("POINT_TYPES",
            "TASK_DEFINITIONS","REWARD_DEFINITIONS","TASK_EXECUTIONS",
            "REWARD_PURCHASES","POINT_LEDGER");

    public static String fromVerifiedSource(ObjectNode source,long sourceWorkspace,
            UUID snapshot,long sourceEpoch,long targetWorkspace,long targetEpoch) {
        if(source==null||sourceWorkspace<=0||targetWorkspace<=0||targetEpoch<0)
            throw new SecurityException("RESTORE_SELECTION_SOURCE_INVALID");
        var proof=PortableMemberAncestryV2.verify(source,sourceWorkspace,
                snapshot,sourceEpoch);
        if(proof.categories().size()!=8)
            throw new SecurityException("RESTORE_SELECTION_NOT_EIGHT_CATEGORY");
        var choices=new ArrayList<WorkspaceRestoreSelectionClosureV2.Category>();
        for(var entry:proof.categories().entrySet()) {
            String category=entry.getKey();
            if(!REPLAY.contains(category)
                    &&!Set.of("FAMILY_ROLES","FAMILY_MEMBERS").contains(category))
                throw new SecurityException("RESTORE_SELECTION_UNSUPPORTED_CATEGORY");
            var validated=entry.getValue();
            String conflictDigest=ProtectiveBackupArtifactCodec.sha((
                    "family-points:restore:target-empty:v2|"+targetWorkspace+"|"
                    +targetEpoch+"|"+category).getBytes(StandardCharsets.UTF_8));
            choices.add(new WorkspaceRestoreSelectionClosureV2.Category(
                    category,REPLAY.contains(category)
                        ?WorkspaceRestoreSelectionClosureV2.Decision.RESTORE
                        :WorkspaceRestoreSelectionClosureV2.Decision.PRESERVE,
                    validated.rows(),validated.contentSha256(),
                    validated.dependencyClosureSha256(),conflictDigest));
        }
        return WorkspaceRestoreSelectionClosureV2.canonicalDigest(
                PortableManifestRootV2.SCHEMA,sourceWorkspace,targetWorkspace,choices);
    }
}
