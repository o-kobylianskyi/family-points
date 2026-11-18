package com.olehkobylianskyi.familypoints.security;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static com.olehkobylianskyi.familypoints.security.WorkspaceRestorePermitScopeV2.*;
import static org.junit.jupiter.api.Assertions.*;

class WorkspaceRestorePermitScopeV2Test {
    private static final Instant TIME=Instant.parse("2026-11-18T12:00:00Z");
    private static final UUID SNAP=UUID.fromString("f1000000-0000-4000-8000-000000000001");
    private static final UUID OP=UUID.fromString("f1000000-0000-4000-8000-000000000002");
    private static final String A="a".repeat(64), B="b".repeat(64), C="c".repeat(64), D="d".repeat(64);
    private SourceArchive archive() {
        return new SourceArchive(7,"VNext-2",SNAP,12,A,B);
    }
    private RestoreScope scope() {
        return new RestoreScope(8,23,17,OP,archive(),C,D,4,TIME,TIME.plusSeconds(300));
    }
    private RestoreScope withSource(SourceArchive src) {
        var s=scope();
        return new RestoreScope(s.targetWorkspaceId(),s.authorizedOwnerAccountId(),s.authorizedOwnerMemberId(),
                s.operationId(),src,s.selectionClosureDigest(),s.memberMappingDigest(),
                s.targetExpectedEpoch(),s.issuedAt(),s.expiresAt());
    }

    @Test void sortedMappingPairsHashIdenticallyInAnyIterationOrder() {
        Map<Long,Long> forward=new LinkedHashMap<>();
        forward.put(11L,41L);forward.put(12L,42L);forward.put(13L,43L);
        Map<Long,Long> reverse=new LinkedHashMap<>();
        reverse.put(13L,43L);reverse.put(12L,42L);reverse.put(11L,41L);
        String digest=canonicalMappingDigest(7,8,forward);
        assertEquals(digest,canonicalMappingDigest(7,8,reverse));
        assertEquals(64,digest.length());
        assertTrue(digest.matches("[0-9a-f]{64}"));
    }

    @Test void mappingHashBindsBothTenantsAndExactPairs() {
        var map=Map.of(11L,41L,12L,42L);
        String original=canonicalMappingDigest(7,8,map);
        assertNotEquals(original,canonicalMappingDigest(8,8,map));
        assertNotEquals(original,canonicalMappingDigest(7,9,map));
        assertNotEquals(original,canonicalMappingDigest(7,8,Map.of(11L,42L,12L,41L)));
        assertNotEquals(original,canonicalMappingDigest(7,8,Map.of(11L,41L)));
    }

    @Test void duplicatedTargetInvalidOrEmptyMappingFailsClosed() {
        assertThrows(IllegalArgumentException.class,
                ()->canonicalMappingDigest(7,8,Map.of(11L,41L,12L,41L)));
        assertThrows(IllegalArgumentException.class,
                ()->canonicalMappingDigest(7,8,Map.of()));
        assertThrows(IllegalArgumentException.class,
                ()->canonicalMappingDigest(0,8,Map.of(11L,41L)));
        var withNull=new HashMap<Long,Long>();
        withNull.put(null,41L);
        assertThrows(IllegalArgumentException.class,()->canonicalMappingDigest(7,8,withNull));
        assertThrows(IllegalArgumentException.class,
                ()->canonicalMappingDigest(7,8,Map.of(0L,41L)));
    }

    @Test void scopeHashBindsManifestSchemaAndSourceEpochDistinctFromTargetEpoch() {
        String digest=canonicalScopeDigest(scope());
        assertNotEquals(digest,canonicalScopeDigest(withSource(
                new SourceArchive(99,"VNext-2",SNAP,12,A,B))));
        assertNotEquals(digest,canonicalScopeDigest(withSource(
                new SourceArchive(7,"VNext-3",SNAP,12,A,B))));
        assertNotEquals(digest,canonicalScopeDigest(withSource(
                new SourceArchive(7,"VNext-2",SNAP,13,A,B))));
        assertNotEquals(digest,canonicalScopeDigest(withSource(
                new SourceArchive(7,"VNext-2",SNAP,12,A,D))));
        var s=scope();
        assertNotEquals(digest,canonicalScopeDigest(new RestoreScope(8,23,17,OP,
                archive(),C,D,5,TIME,TIME.plusSeconds(300))));
        assertTrue(digest.matches("[0-9a-f]{64}"));
    }

    @Test void scopeHashBindsOwnerOperationSelectionAndReviewedMemberMapping() {
        var s=scope();
        String digest=canonicalScopeDigest(s);
        assertNotEquals(digest,canonicalScopeDigest(new RestoreScope(8,24,17,OP,archive(),C,D,4,
                TIME,TIME.plusSeconds(300))));
        assertNotEquals(digest,canonicalScopeDigest(new RestoreScope(8,23,18,OP,archive(),C,D,4,
                TIME,TIME.plusSeconds(300))));
        assertNotEquals(digest,canonicalScopeDigest(new RestoreScope(8,23,17,UUID.randomUUID(),
                archive(),C,D,4,TIME,TIME.plusSeconds(300))));
        assertNotEquals(digest,canonicalScopeDigest(new RestoreScope(8,23,17,OP,
                archive(),D,D,4,TIME,TIME.plusSeconds(300))));
        assertNotEquals(digest,canonicalScopeDigest(new RestoreScope(8,23,17,OP,
                archive(),C,A,4,TIME,TIME.plusSeconds(300))));
    }

    @Test void onlyUnexpiredExactScopeMatchesAndToStringRedacts() {
        var s=scope();
        assertTrue(s.matches(s,TIME));
        assertTrue(s.matches(s,TIME.plusSeconds(299)));
        assertFalse(s.matches(s,TIME.minusSeconds(1)));
        assertFalse(s.matches(s,TIME.plusSeconds(300)));
        assertFalse(s.matches(s,null));
        assertFalse(s.matches(null,TIME));
        assertFalse(s.toString().contains(s.selectionClosureDigest()));
        assertFalse(s.toString().contains(s.memberMappingDigest()));
        assertFalse(s.toString().contains("23"));
    }

    @Test void unknownSchemaMalformedDigestAndOverlongTtlAreRejected() {
        assertThrows(IllegalArgumentException.class,()->new SourceArchive(7,"",SNAP,0,A,B));
        assertThrows(IllegalArgumentException.class,()->new SourceArchive(7,"V2",SNAP,-1,A,B));
        assertThrows(IllegalArgumentException.class,()->new SourceArchive(7,"V2",SNAP,0,"no",B));
        assertThrows(IllegalArgumentException.class,()->new RestoreScope(8,23,17,OP,archive(),
                C,D,4,TIME,TIME.plusSeconds(301)));
        assertThrows(IllegalArgumentException.class,()->new RestoreScope(8,23,17,OP,archive(),
                C,D,4,TIME,TIME));
    }
}
