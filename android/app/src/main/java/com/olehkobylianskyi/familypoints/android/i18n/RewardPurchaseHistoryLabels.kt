package com.olehkobylianskyi.familypoints.android.i18n

/**
 * Labels are intentionally limited to persisted PURCHASE states.
 * READY/REDEEMED are not interpreted as delivery/receipt confirmations.
 */
object RewardPurchaseHistoryLabels {
    fun status(language: AppLanguage, raw: String): String = when (raw) {
        "PENDING_APPROVAL" -> when (language) {
            AppLanguage.UK -> "Очікує погодження"
            AppLanguage.RU -> "Ожидает согласования"
            AppLanguage.DE -> "Wartet auf Freigabe"
            AppLanguage.EN -> "Pending approval"
        }
        "READY" -> when (language) {
            AppLanguage.UK -> "Готова"
            AppLanguage.RU -> "Готова"
            AppLanguage.DE -> "Bereit"
            AppLanguage.EN -> "Ready"
        }
        "REDEEMED" -> when (language) {
            AppLanguage.UK -> "Погашена"
            AppLanguage.RU -> "Погашена"
            AppLanguage.DE -> "Eingelöst"
            AppLanguage.EN -> "Redeemed"
        }
        "CANCELLED" -> when (language) {
            AppLanguage.UK -> "Скасована"
            AppLanguage.RU -> "Отменена"
            AppLanguage.DE -> "Storniert"
            AppLanguage.EN -> "Cancelled"
        }
        else -> raw
    }

    fun duration(language: AppLanguage, minutes: Int): String =
        when (language) {
            AppLanguage.UK -> "$minutes хв"
            AppLanguage.RU -> "$minutes мин"
            AppLanguage.DE -> "$minutes Min."
            AppLanguage.EN -> "$minutes min"
        }
}
