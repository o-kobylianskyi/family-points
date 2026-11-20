package com.olehkobylianskyi.familypoints.security;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.time.Duration;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/** Deterministic nanosecond-precise 5-minute scope contract, no sleep. */
class WorkspaceRestorePermitExactBoundaryTest {
    private static WorkspaceRestorePermitScopeV2.RestoreScope scope(
            Instant start,Duration duration) {
        return new WorkspaceRestorePermitScopeV2.RestoreScope(12,13,14,
            UUID.fromString("11111111-2222-4333-8444-555555555555"),
            new WorkspaceRestorePermitScopeV2.SourceArchive(22,"FPB2",
                UUID.fromString("22222222-3333-4444-8555-666666666666"),0,
                "a".repeat(64),"b".repeat(64)),
            "c".repeat(64),"d".repeat(64),0,start,start.plus(duration));
    }
    @Test void exactFiveMinutesPermitsBeforeButNotAtExpiration() {
        Instant start=Instant.parse("2026-11-19T12:00:00.123456789Z");
        var valid=scope(start,Duration.ofMinutes(5));
        assertFalse(valid.matches(valid,start.minusNanos(1)));
        assertTrue(valid.matches(valid,start));
        assertTrue(valid.matches(valid,start.plus(Duration.ofMinutes(5)).minusNanos(1)));
        assertFalse(valid.matches(valid,start.plus(Duration.ofMinutes(5))));
        assertFalse(valid.matches(valid,start.plus(Duration.ofMinutes(5)).plusNanos(1)));
        assertThrows(IllegalArgumentException.class,
                ()->scope(start,Duration.ofMinutes(5).plusNanos(1)));
        assertThrows(IllegalArgumentException.class,
                ()->scope(start,Duration.ZERO));
    }
}
