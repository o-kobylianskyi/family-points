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
    @Test fun comprehensiveReplayResponseAccountEpochMatrix() {
        data class Case(
            val original: String?, val active: String?,
            val expectedWorkspace: Long, val returnedWorkspace: Long,
            val expectedEpoch: Long, val returnedEpoch: Long,
            val allowQuarantine: Boolean
        )
        val cases = listOf(
            Case("a", "a", 100, 100, 1, 2, true),
            Case("a", "a", 100, 100, 2, 1, true),
            Case("a", "a", 100, 100, 0, 1, true),
            Case("a", "a", 100, 100, 2, 2, false),
            Case("a", "b", 100, 100, 1, 2, false),
            Case("a", null, 100, 100, 1, 2, false),
            Case(null, "a", 100, 100, 1, 2, false),
            Case("", "", 100, 100, 1, 2, false),
            Case("a", "a", 100, 101, 1, 2, false),
            Case("a", "a", 0, 0, 1, 2, false),
            Case("a", "a", 100, 100, -1, 2, false),
            Case("a", "a", 100, 100, 1, -1, false)
        )
        cases.forEachIndexed { index, case ->
            assertEquals(
                "Replay session/epoch gate scenario #$index", case.allowQuarantine,
                OfflineReplaySessionGate.canQuarantine(
                    case.original, case.active, case.expectedWorkspace,
                    case.returnedWorkspace, case.expectedEpoch, case.returnedEpoch
                )
            )
        }
    }

}
