package com.olehkobylianskyi.familypoints.android.ui.screens

import com.olehkobylianskyi.familypoints.android.storage.RewardPurchaseIntentStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class RewardPurchaseIntentReviewModelTest {
    private fun intent(account: Long, workspace: Long, state: String) =
        RewardPurchaseIntentStore.Intent(
            UUID.randomUUID(), account, workspace, 17, 99, 3, "SELF", state
        )

    @Test fun exactAccountAndWorkspaceOnly() {
        val source = listOf(
            intent(11, 50, "UNCERTAIN"),
            intent(12, 50, "HELD"),
            intent(11, 51, "PREPARED"),
            intent(11, 50, "CONFIRMED")
        )
        val result = RewardPurchaseIntentReviewModel.scoped(source, 11, 50)
        assertEquals(2, result.size)
        assertEquals(listOf("UNCERTAIN", "CONFIRMED"), result.map { it.state })
    }

    @Test fun unknownAndInvalidScopeAreFailClosed() {
        assertTrue(RewardPurchaseIntentReviewModel.scoped(listOf(intent(1, 2, "UNKNOWN")), 1, 2).isEmpty())
        assertTrue(RewardPurchaseIntentReviewModel.scoped(listOf(intent(1, 2, "HELD")), 0, 2).isEmpty())
        assertTrue(RewardPurchaseIntentReviewModel.scoped(listOf(intent(1, 2, "HELD")), 1, 0).isEmpty())
    }

    @Test fun noStateEnablesAutomaticPurchase() {
        val items = listOf("PREPARED", "UNCERTAIN", "HELD", "CONFIRMED")
            .map { intent(1, 2, it) }
        val projected = RewardPurchaseIntentReviewModel.scoped(items, 1, 2)
        assertEquals(4, projected.size)
        projected.forEach { assertFalse(RewardPurchaseIntentReviewModel.actionableForAutomaticPurchase(it)) }
    }
}
