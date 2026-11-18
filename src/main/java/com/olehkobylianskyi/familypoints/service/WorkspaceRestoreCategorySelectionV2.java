package com.olehkobylianskyi.familypoints.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.olehkobylianskyi.familypoints.security.WorkspaceRestoreSelectionClosureV2;
import com.olehkobylianskyi.familypoints.security.WorkspaceRestorePermitScopeV2;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

/**
 * Bind owner review to the exact authenticated category contents and original
 * parent closure; no caller-supplied category digest or mutable source DB.
 */
@Component
public final class WorkspaceRestoreCategorySelectionV2 {
    private static final ObjectMapper JSON=new ObjectMapper();
    private static final Set<String> CORE=Set.of(
            "POINT_TYPES","TASK_DEFINITIONS","REWARD_DEFINITIONS",
            "TASK_EXECUTIONS","REWARD_PURCHASES","POINT_LEDGER");
    private final ProtectiveBackupPortableFile files;
    private final ProtectiveBackupArtifactCodec codec;
    public WorkspaceRestoreCategorySelectionV2(ProtectiveBackupPortableFile files,
            ProtectiveBackupArtifactCodec codec) {
        this.files=files;this.codec=codec;
    }

    public String requireCanonical(WorkspaceRestorePermitScopeV2.RestoreScope scope,
            byte[] file,SecretKey key) {
        String expected=calculateCanonical(scope,file,key);
        if(!expected.equals(scope.selectionClosureDigest()))
            throw new SecurityException("RESTORE_SELECTION_SCOPE_NOT_AUTHENTICATED");
        return expected;
    }

    public String calculateCanonical(WorkspaceRestorePermitScopeV2.RestoreScope scope,
            byte[] file,SecretKey key) {
        var archive=files.decode(file);
        if(archive.manifest().workspaceId()!=scope.source().sourceWorkspaceId()
                ||!archive.manifest().snapshotId().equals(scope.source().snapshotId())
                ||!PortableManifestRootV2.SCHEMA.equals(archive.manifest().schemaVersion()))
            throw new SecurityException("RESTORE_SELECTION_ARCHIVE_SCOPE");
        byte[] clear=codec.open(archive,scope.source().snapshotId(),
                scope.source().sourceWorkspaceId(),key);
        try {
            var parsed=JSON.readTree(clear);
            if(!(parsed instanceof ObjectNode root))
                throw new SecurityException("RESTORE_SELECTION_ARCHIVE_INVALID");
            var checked=PortableMemberAncestryV2.verify(root,
                    scope.source().sourceWorkspaceId(),scope.source().snapshotId(),
                    scope.source().sourceSnapshotEpoch());
            if(checked.categories().size()!=8
                    ||!checked.manifestRootDigest().equals(scope.source().manifestRootDigest())
                    ||!checked.sourceDigest().equals(archive.manifest().sourceDigest())
                    ||!archive.manifest().contentSha256().equals(scope.source().snapshotDigest()))
                throw new SecurityException("RESTORE_SELECTION_SIGNED_DIGEST_MISMATCH");
            var categories=new ArrayList<WorkspaceRestoreSelectionClosureV2.Category>();
            checked.categories().forEach((name,category)->{
                var choice=CORE.contains(name)
                        ?WorkspaceRestoreSelectionClosureV2.Decision.RESTORE
                        :WorkspaceRestoreSelectionClosureV2.Decision.PRESERVE;
                String conflict=ProtectiveBackupArtifactCodec.sha((
                        "family-points:restore:target-empty:v2|"+scope.targetWorkspaceId()
                        +"|"+scope.targetExpectedEpoch()+"|"+name
                        ).getBytes(StandardCharsets.UTF_8));
                categories.add(new WorkspaceRestoreSelectionClosureV2.Category(
                        name,choice,category.rows(),category.contentSha256(),
                        category.dependencyClosureSha256(),conflict));
            });
            String digest=WorkspaceRestoreSelectionClosureV2.canonicalDigest(
                    PortableManifestRootV2.SCHEMA,scope.source().sourceWorkspaceId(),
                    scope.targetWorkspaceId(),categories);
            return digest;
        } catch(java.io.IOException malformed) {
            throw new SecurityException("RESTORE_SELECTION_JSON_INVALID",malformed);
        } finally {Arrays.fill(clear,(byte)0);}
    }
}
