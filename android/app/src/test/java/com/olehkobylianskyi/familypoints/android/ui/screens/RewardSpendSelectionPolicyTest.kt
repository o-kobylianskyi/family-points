package com.olehkobylianskyi.familypoints.android.ui.screens

import org.junit.Assert.*
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

    @Test fun inaccessibleOrOfflineCatalogCannotOfferRewardPurchase() {
        assertTrue(RewardSpendSelectionPolicy.catalog(emptyList(), emptyList(), true, true).isEmpty())
        assertTrue(RewardSpendSelectionPolicy.catalog(emptyList(), emptyList(), false, true).isEmpty())
        assertTrue(RewardSpendSelectionPolicy.catalog(emptyList(), emptyList(), true, false).isEmpty())
    }
}
