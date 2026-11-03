package com.olehkobylianskyi.familypoints.android.i18n

import org.junit.Assert.*
import org.junit.Test

class RewardPurchaseHistoryLabelsTest {
    @Test fun fourLocalesRepresentPersistedPurchaseStatusesOnly() {
        AppLanguage.entries.forEach { locale ->
            listOf("PENDING_APPROVAL", "READY", "REDEEMED", "CANCELLED").forEach { source ->
                assertTrue(RewardPurchaseHistoryLabels.status(locale, source).isNotBlank())
            }
            assertEquals("RECEIVED", RewardPurchaseHistoryLabels.status(locale, "RECEIVED"))
            assertEquals("DELIVERED", RewardPurchaseHistoryLabels.status(locale, "DELIVERED"))
            assertEquals("EXPIRED", RewardPurchaseHistoryLabels.status(locale, "EXPIRED"))
        }
    }

    @Test fun durationHasLanguageSpecificUnit() {
        assertEquals("30 хв", RewardPurchaseHistoryLabels.duration(AppLanguage.UK, 30))
        assertEquals("30 мин", RewardPurchaseHistoryLabels.duration(AppLanguage.RU, 30))
        assertEquals("30 Min.", RewardPurchaseHistoryLabels.duration(AppLanguage.DE, 30))
        assertEquals("30 min", RewardPurchaseHistoryLabels.duration(AppLanguage.EN, 30))
    }
}
