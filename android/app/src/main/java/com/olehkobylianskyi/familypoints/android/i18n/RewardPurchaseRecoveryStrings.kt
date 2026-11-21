package com.olehkobylianskyi.familypoints.android.i18n

/** Read-only receipt recovery copy; no purchase execution is enabled by these labels. */
data class RewardPurchaseRecoveryStrings(
    val checkReceipt: String,
    val checking: String,
    val confirmed: String,
    val needsReview: String,
    val pendingExplanation: String,
    val noAutomaticRetry: String
)

fun rewardPurchaseRecoveryStrings(language: AppLanguage): RewardPurchaseRecoveryStrings =
    when (language) {
        AppLanguage.UK -> RewardPurchaseRecoveryStrings(
            "Перевірити квитанцію", "Перевіряємо квитанцію…",
            "Покупку підтверджено", "Потрібна перевірка",
            "Результат покупки невідомий. Можливе списання балів.",
            "Не повторюйте покупку автоматично."
        )
        AppLanguage.RU -> RewardPurchaseRecoveryStrings(
            "Проверить квитанцию", "Проверяем квитанцию…",
            "Покупка подтверждена", "Нужна проверка",
            "Результат покупки неизвестен. Баллы могли быть списаны.",
            "Не повторяйте покупку автоматически."
        )
        AppLanguage.DE -> RewardPurchaseRecoveryStrings(
            "Beleg prüfen", "Beleg wird geprüft…",
            "Kauf bestätigt", "Prüfung erforderlich",
            "Das Kaufergebnis ist unklar. Punkte könnten abgebucht worden sein.",
            "Den Kauf nicht automatisch wiederholen."
        )
        AppLanguage.EN -> RewardPurchaseRecoveryStrings(
            "Check receipt", "Checking receipt…",
            "Purchase confirmed", "Review required",
            "Purchase outcome is unknown. Points may have been deducted.",
            "Do not automatically repeat the purchase."
        )
    }
