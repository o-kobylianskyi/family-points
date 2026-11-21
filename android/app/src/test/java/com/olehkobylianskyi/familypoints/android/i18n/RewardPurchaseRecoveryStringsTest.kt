package com.olehkobylianskyi.familypoints.android.i18n

import org.junit.Assert.assertTrue
import org.junit.Test

class RewardPurchaseRecoveryStringsTest {
    @Test fun allLocalesHaveNonEmptyRecoveryLabels() {
        for (language in AppLanguage.entries) {
            val labels = rewardPurchaseRecoveryStrings(language)
            assertTrue(labels.checkReceipt.isNotBlank())
            assertTrue(labels.checking.isNotBlank())
            assertTrue(labels.confirmed.isNotBlank())
            assertTrue(labels.needsReview.isNotBlank())
            assertTrue(labels.pendingExplanation.isNotBlank())
            assertTrue(labels.noAutomaticRetry.isNotBlank())
        }
    }
}
