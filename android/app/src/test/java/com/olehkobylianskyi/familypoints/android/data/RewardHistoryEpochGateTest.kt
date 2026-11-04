package com.olehkobylianskyi.familypoints.android.data

import org.junit.Assert.*
import org.junit.Test

class RewardHistoryEpochGateTest {
    @Test fun stableVerifiedWorkspaceEpochPermitsReadOnlyHistory() {
        assertTrue(RewardHistoryEpochGate.matches(91, 91, 0, 91, 0))
        assertTrue(RewardHistoryEpochGate.matches(91, 91, 177, 91, 177))
    }

    @Test fun driftOrDifferentFamilyNeverExposesStaleHistory() {
        assertFalse(RewardHistoryEpochGate.matches(91, 91, 177, 91, 178))
        assertFalse(RewardHistoryEpochGate.matches(91, 92, 177, 91, 177))
        assertFalse(RewardHistoryEpochGate.matches(91, 91, 177, 92, 177))
        assertFalse(RewardHistoryEpochGate.matches(0, 0, 177, 0, 177))
        assertFalse(RewardHistoryEpochGate.matches(91, 91, -1, 91, -1))
        assertFalse(RewardHistoryEpochGate.matches(91, 91, -1, 91, 0))
    }

    @Test fun resetToSameNumbersWithoutGenerationChangeIsNotAssumed() {
        assertTrue(RewardHistoryEpochGate.matches(91, 91, 5, 91, 5))
    }
}
