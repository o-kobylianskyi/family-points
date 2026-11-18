package com.olehkobylianskyi.familypoints.security;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;

import static com.olehkobylianskyi.familypoints.security.WorkspaceDestructivePermitContract.*;
import static org.junit.jupiter.api.Assertions.*;

class WorkspaceDestructivePermitContractTest {
    private static final Instant ISSUED = Instant.parse("2026-11-18T09:00:00Z");
    private static final String S = "a".repeat(64), C = "b".repeat(64), M = "c".repeat(64);
    private static final UUID OP = UUID.fromString("d3000000-0000-4000-8000-000000000001");
    private static final UUID SNAPSHOT = UUID.fromString("d3000000-0000-4000-8000-000000000002");

    private Scope scope(Action action) {
        return new Scope(action, OP, 7L, 23L, 17L, SNAPSHOT, S, C,
                action == Action.RESTORE ? M : null, 3L, ISSUED, ISSUED.plusSeconds(300));
    }

    private Scope mutate(Scope old, Action action, UUID operation, long workspace, long account,
                         long member, UUID snapshot, String snapshotHash, String selectionHash,
                         String memberMapHash, long epoch, Instant issued, Instant expires) {
        return new Scope(action, operation, workspace, account, member, snapshot,
                snapshotHash, selectionHash, memberMapHash, epoch, issued, expires);
    }

    @Test void exactResetScopeAllowedOnlyWithinShortValidityWindow() {
        Scope reset=scope(Action.RESET);
        assertTrue(reset.matches(reset, ISSUED));
        assertTrue(reset.matches(reset, ISSUED.plusSeconds(299)));
        assertFalse(reset.matches(reset, ISSUED.minusNanos(1)));
        assertFalse(reset.matches(reset, ISSUED.plusSeconds(300)));
        assertFalse(reset.matches(null, ISSUED));
        assertFalse(reset.matches(reset, null));
    }

    @Test void restoreApprovalNeverMatchesResetAndRestoreRequiresMappingProof() {
        Scope restore=scope(Action.RESTORE), reset=scope(Action.RESET);
        assertFalse(reset.matches(restore, ISSUED.plusSeconds(10)));
        assertFalse(restore.matches(reset, ISSUED.plusSeconds(10)));
        assertThrows(IllegalArgumentException.class, () -> mutate(
                restore, Action.RESTORE, OP, 7, 23, 17, SNAPSHOT, S, C, null, 3,
                ISSUED, ISSUED.plusSeconds(60)));
        assertThrows(IllegalArgumentException.class, () -> mutate(
                reset, Action.RESET, OP, 7, 23, 17, SNAPSHOT, S, C, M, 3,
                ISSUED, ISSUED.plusSeconds(60)));
    }

    @Test void ownerTenantSnapshotMappingAndEpochMustAllMatch() {
        Scope base=scope(Action.RESTORE);
        Instant now=ISSUED.plusSeconds(1);
        assertFalse(base.matches(mutate(base, Action.RESTORE, OP, 8, 23, 17, SNAPSHOT, S, C, M, 3,
                ISSUED, ISSUED.plusSeconds(300)), now));
        assertFalse(base.matches(mutate(base, Action.RESTORE, OP, 7, 24, 17, SNAPSHOT, S, C, M, 3,
                ISSUED, ISSUED.plusSeconds(300)), now));
        assertFalse(base.matches(mutate(base, Action.RESTORE, OP, 7, 23, 18, SNAPSHOT, S, C, M, 3,
                ISSUED, ISSUED.plusSeconds(300)), now));
        assertFalse(base.matches(mutate(base, Action.RESTORE, UUID.randomUUID(), 7, 23, 17,
                SNAPSHOT, S, C, M, 3, ISSUED, ISSUED.plusSeconds(300)), now));
        assertFalse(base.matches(mutate(base, Action.RESTORE, OP, 7, 23, 17,
                UUID.randomUUID(), S, C, M, 3, ISSUED, ISSUED.plusSeconds(300)), now));
        assertFalse(base.matches(mutate(base, Action.RESTORE, OP, 7, 23, 17,
                SNAPSHOT, "d".repeat(64), C, M, 3, ISSUED, ISSUED.plusSeconds(300)), now));
        assertFalse(base.matches(mutate(base, Action.RESTORE, OP, 7, 23, 17,
                SNAPSHOT, S, "d".repeat(64), M, 3, ISSUED, ISSUED.plusSeconds(300)), now));
        assertFalse(base.matches(mutate(base, Action.RESTORE, OP, 7, 23, 17,
                SNAPSHOT, S, C, "d".repeat(64), 3, ISSUED, ISSUED.plusSeconds(300)), now));
        assertFalse(base.matches(mutate(base, Action.RESTORE, OP, 7, 23, 17,
                SNAPSHOT, S, C, M, 4, ISSUED, ISSUED.plusSeconds(300)), now));
    }

    @Test void malformedIdentityDigestOrLifetimeIsRejectedOnConstruction() {
        assertThrows(IllegalArgumentException.class, () -> new Scope(
                Action.RESET, OP, 0, 23, 17, SNAPSHOT, S, C, null, 3,
                ISSUED, ISSUED.plusSeconds(30)));
        assertThrows(IllegalArgumentException.class, () -> new Scope(
                Action.RESET, OP, 7, 23, 17, SNAPSHOT, "invalid", C, null, 3,
                ISSUED, ISSUED.plusSeconds(30)));
        assertThrows(IllegalArgumentException.class, () -> new Scope(
                Action.RESET, OP, 7, 23, 17, SNAPSHOT, S, C, null, -1,
                ISSUED, ISSUED.plusSeconds(30)));
        assertThrows(IllegalArgumentException.class, () -> new Scope(
                Action.RESET, OP, 7, 23, 17, SNAPSHOT, S, C, null, 3,
                ISSUED, ISSUED.plusSeconds(301)));
        assertThrows(IllegalArgumentException.class, () -> new Scope(
                Action.RESET, OP, 7, 23, 17, SNAPSHOT, S, C, null, 3,
                ISSUED, ISSUED));
    }

    @Test void scopeCannotLeakIdentityOrDigestsThroughDefaultToString() {
        String printable=scope(Action.RESTORE).toString();
        assertTrue(printable.contains("redacted"));
        assertFalse(printable.contains(S));
        assertFalse(printable.contains(C));
        assertFalse(printable.contains(M));
        assertFalse(printable.contains("23"));
        assertFalse(printable.contains("17"));
    }
}
