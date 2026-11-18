package com.olehkobylianskyi.familypoints.security;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.olehkobylianskyi.familypoints.security.WorkspaceRestorePermitScopeV2.*;
import static com.olehkobylianskyi.familypoints.security.WorkspaceRestoreSelectionClosureV2.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Independently derived binary-format SHA-256 golden vectors.
 * A/B consumers MUST reproduce these bytes before permitting shared V2 wire.
 * This class itself is not an authenticated permit issuer or Restore executor.
 */
class WorkspaceRestorePermitCanonicalVectorsTest {
    private static final String A="a".repeat(64),B="b".repeat(64),
            C="c".repeat(64),D="d".repeat(64);
    private static final Instant ISSUED=Instant.parse("2026-11-18T12:00:00Z");
    private static final UUID SNAPSHOT=UUID.fromString("f1000000-0000-4000-8000-000000000001");
    private static final UUID OPERATION=UUID.fromString("f1000000-0000-4000-8000-000000000002");

    @Test void fixedMemberMapVectorIsStableAcrossLanguages() {
        assertEquals("690dafc3d70b893b8b394827044692b26f55b8dcee773d52e510d892c1bca8fc",
                canonicalMappingDigest(7,8, Map.of(11L,41L,12L,42L)));
    }

    @Test void fixedSelectionClosureVectorIncludesPreserveDecisions() {
        assertEquals("b008109bcb80fde5728b36393827c5afafc3780c0d8eb3616d0a4289acf0202b",
                WorkspaceRestoreSelectionClosureV2.canonicalDigest("VNext-2",7,8,List.of(
                        new Category("TASK_DEFINITIONS",Decision.RESTORE,11,A,B,C),
                        new Category("REWARD_PURCHASES",Decision.PRESERVE,8,B,C,D))));
    }

    @Test void fixedRestoreScopeVectorBindsArchiveAndTargetEpoch() {
        var archive=new SourceArchive(7,"VNext-2",SNAPSHOT,12,A,B);
        var scope=new RestoreScope(8,23,17,OPERATION,archive,C,D,4,
                ISSUED,ISSUED.plusSeconds(300));
        assertEquals("17f6d74e1eb83f826b455d81b2cbcf8b896ab9af1d639305ff3ed5e4bf916979",
                canonicalScopeDigest(scope));
    }
}
