package com.olehkobylianskyi.familypoints.android.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ActivationCompletionGateTest {
    private fun outcome(
        originalAccount: Long = 11, activeAccount: Long = 11,
        originalFamily: Long = 21, verifiedFamily: Long = 21,
        originalEpoch: Long = 17, verifiedEpoch: Long = 17,
        originalToken: String? = "session-a", activeToken: String? = "session-a"
    ) = ActivationCompletionGate.decide(originalAccount, activeAccount,
        originalFamily, verifiedFamily, originalEpoch, verifiedEpoch,
        originalToken, activeToken)

    @Test fun validSameGenerationCanConfirm() {
        assertEquals(ActivationCompletionGate.Decision.CONFIRM, outcome())
    }

    @Test fun successfulPostFollowedByFamilyGenerationChangeMustHold() {
        assertEquals(ActivationCompletionGate.Decision.HOLD, outcome(verifiedEpoch = 18))
    }

    @Test fun changedUserOrFamilyCannotConfirmOldReceipt() {
        assertEquals(ActivationCompletionGate.Decision.UNCERTAIN, outcome(activeAccount = 12))
        assertEquals(ActivationCompletionGate.Decision.UNCERTAIN, outcome(verifiedFamily = 22))
        assertEquals(ActivationCompletionGate.Decision.UNCERTAIN, outcome(activeToken = "session-b"))
        assertEquals(ActivationCompletionGate.Decision.UNCERTAIN, outcome(activeToken = null))
    }

    @Test fun missingServerGenerationOrMissingAuthenticatedTokenCannotConfirm() {
        assertEquals(ActivationCompletionGate.Decision.UNCERTAIN, outcome(verifiedEpoch = -1))
        assertEquals(ActivationCompletionGate.Decision.UNCERTAIN, outcome(originalEpoch = -1))
        assertEquals(ActivationCompletionGate.Decision.UNCERTAIN, outcome(originalToken = null))
        assertEquals(ActivationCompletionGate.Decision.UNCERTAIN, outcome(originalToken = ""))
    }
}
