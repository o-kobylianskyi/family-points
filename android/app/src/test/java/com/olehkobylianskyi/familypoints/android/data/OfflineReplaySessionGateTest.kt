package com.olehkobylianskyi.familypoints.android.data

import org.junit.Assert.*
import org.junit.Test

class OfflineReplaySessionGateTest {
    @Test fun sameOriginalSessionCanAcknowledgePendingUuid() {
        assertTrue(OfflineReplaySessionGate.matches("old-session", "old-session"))
        assertFalse(OfflineReplaySessionGate.matches("old-session", "new-session"))
        assertFalse(OfflineReplaySessionGate.matches("old-session", null))
        assertFalse(OfflineReplaySessionGate.matches(null, "new-session"))
        assertFalse(OfflineReplaySessionGate.matches("", ""))
    }

    @Test fun onlyAuthenticatedSameFamilyEpochChangeCanQuarantine() {
        assertTrue(OfflineReplaySessionGate.canQuarantine("old", "old", 91, 91, 17, 18))
        assertFalse(OfflineReplaySessionGate.canQuarantine("old", "new", 91, 91, 17, 18))
        assertFalse(OfflineReplaySessionGate.canQuarantine("old", "old", 91, 92, 17, 18))
        assertFalse(OfflineReplaySessionGate.canQuarantine("old", "old", 91, 91, 17, 17))
        assertFalse(OfflineReplaySessionGate.canQuarantine("old", "old", 91, 91, 17, -1))
        assertFalse(OfflineReplaySessionGate.canQuarantine("old", "old", -1, -1, 17, 18))
    }

    @Test fun server409WithoutGenerationChangeIsNotResetProof() {
        assertFalse(OfflineReplaySessionGate.canQuarantine("token", "token", 91, 91, 17, 17))
    }
}
