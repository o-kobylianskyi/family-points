package com.olehkobylianskyi.familypoints.android.data

import org.junit.Assert.assertFalse
import org.junit.Test

class RewardPurchaseReadOnlyRepositoryPolicyTest {
    @Test fun noLiveReadOrFinancialPurchaseGateIsEnabled() {
        assertFalse(RewardPurchaseReadOnlyRepository.LIVE_RECEIPT_UI_ENABLED)
        assertFalse(RewardPurchaseReadOnlyRepository.PURCHASE_POST_ENABLED)
    }
}
