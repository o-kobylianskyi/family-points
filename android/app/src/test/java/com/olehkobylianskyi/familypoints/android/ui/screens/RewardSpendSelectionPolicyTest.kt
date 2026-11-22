package com.olehkobylianskyi.familypoints.android.ui.screens

import org.junit.Assert.*
import com.olehkobylianskyi.familypoints.android.data.RewardDefinitionSummary
import com.olehkobylianskyi.familypoints.android.data.PointTypeResponse
import org.junit.Test

class RewardSpendSelectionPolicyTest {
    @Test fun selectedRewardCanNeverBeManualSpendOrAutomaticPurchase() {
        assertFalse(RewardSpendSelectionPolicy.PURCHASE_WRITES_ENABLED)
        assertEquals(RewardSpendSelectionPolicy.Action.REWARD_PURCHASE_DISABLED,
            RewardSpendSelectionPolicy.action(true, 25, 4))
        assertEquals(RewardSpendSelectionPolicy.Action.REWARD_PURCHASE_DISABLED,
            RewardSpendSelectionPolicy.action(true, null, null))
    }

    @Test fun manualSpendRequiresPositiveAmountAndValidCurrency() {
        assertEquals(RewardSpendSelectionPolicy.Action.MANUAL_SPEND,
            RewardSpendSelectionPolicy.action(false, 12, 2))
        for (amount in listOf(null, -1, 0, 100_001)) {
            assertEquals(RewardSpendSelectionPolicy.Action.INVALID,
                RewardSpendSelectionPolicy.action(false, amount, 2))
        }
        assertEquals(RewardSpendSelectionPolicy.Action.INVALID,
            RewardSpendSelectionPolicy.action(false, 12, null))
    }

    @Test fun catalogRequiresDirectModeActiveCurrencyAndManageRewards() {
        val pointType = PointTypeResponse(7, "POINTS", "Points", true, 0)
        val reward = RewardDefinitionSummary(
            id = 2, title = "One hour", description = null,
            pointTypeId = 7, pointTypeCode = "POINTS", pointTypeName = "Points",
            priceAmount = 25, minimumReputation = null, requiresApproval = false,
            rewardKind = "TIME", defaultDurationMinutes = 60, acquisitionMode = "DIRECT",
            categories = emptyList(), requirements = emptyList(), active = true
        )
        val list = listOf(reward)
        assertEquals(list, RewardSpendSelectionPolicy.catalog(list, listOf(pointType), true, true))
        assertTrue(RewardSpendSelectionPolicy.catalog(list, listOf(pointType), false, true).isEmpty())
        assertTrue(RewardSpendSelectionPolicy.catalog(list, listOf(pointType), true, false).isEmpty())
        assertTrue(RewardSpendSelectionPolicy.catalog(list, listOf(pointType.copy(active = false)), true, true).isEmpty())
        assertTrue(RewardSpendSelectionPolicy.catalog(list, listOf(pointType.copy(code = "OTHER")), true, true).isEmpty())
        assertTrue(RewardSpendSelectionPolicy.catalog(listOf(reward.copy(requiresApproval = true)), listOf(pointType), true, true).isEmpty())
        assertTrue(RewardSpendSelectionPolicy.catalog(listOf(reward.copy(acquisitionMode = "REQUEST")), listOf(pointType), true, true).isEmpty())
        assertTrue(RewardSpendSelectionPolicy.catalog(listOf(reward.copy(priceAmount = -1)), listOf(pointType), true, true).isEmpty())
    }

    @Test fun inaccessibleOrOfflineCatalogCannotOfferRewardPurchase() {
        assertTrue(RewardSpendSelectionPolicy.catalog(emptyList(), emptyList(), true, true).isEmpty())
        assertTrue(RewardSpendSelectionPolicy.catalog(emptyList(), emptyList(), false, true).isEmpty())
        assertTrue(RewardSpendSelectionPolicy.catalog(emptyList(), emptyList(), true, false).isEmpty())
    }
}
