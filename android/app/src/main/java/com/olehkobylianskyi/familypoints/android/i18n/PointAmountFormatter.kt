package com.olehkobylianskyi.familypoints.android.i18n

data class PointNameForms(val language: String, val one: String, val few: String, val many: String)

/** Optional localized fallback for built-in currencies; user-defined forms always take priority. */
fun builtInCurrencyForms(code: String?, language: String): PointNameForms? {
    val labels = when (code?.uppercase()) {
        "POINTS" -> when (language) {
            "uk" -> listOf("бал", "бали", "балів")
            "ru" -> listOf("балл", "балла", "баллов")
            "en" -> listOf("point", "points", "points")
            "de" -> listOf("Punkt", "Punkte", "Punkte")
            else -> null
        }
        "COPPER" -> when (language) {
            "uk" -> listOf("мідна монета", "мідні монети", "мідних монет")
            "ru" -> listOf("медная монета", "медные монеты", "медных монет")
            "en" -> listOf("copper coin", "copper coins", "copper coins")
            "de" -> listOf("Kupfermünze", "Kupfermünzen", "Kupfermünzen")
            else -> null
        }
        "SILVER" -> when (language) {
            "uk" -> listOf("срібна монета", "срібні монети", "срібних монет")
            "ru" -> listOf("серебряная монета", "серебряные монеты", "серебряных монет")
            "en" -> listOf("silver coin", "silver coins", "silver coins")
            "de" -> listOf("Silbermünze", "Silbermünzen", "Silbermünzen")
            else -> null
        }
        "GOLD" -> when (language) {
            "uk" -> listOf("золота монета", "золоті монети", "золотих монет")
            "ru" -> listOf("золотая монета", "золотые монеты", "золотых монет")
            "en" -> listOf("gold coin", "gold coins", "gold coins")
            "de" -> listOf("Goldmünze", "Goldmünzen", "Goldmünzen")
            else -> null
        }
        else -> null
    } ?: return null
    return PointNameForms(language, labels[0], labels[1], labels[2])
}

/** Quantity label rules shared by built-in and user-defined point types. */
fun formatPointAmount(
    amount: Long,
    language: AppLanguage,
    code: String?,
    fallback: String,
    forms: List<PointNameForms> = emptyList()
): String {
    val lang = when (language) {
        AppLanguage.UK -> "uk"
        AppLanguage.RU -> "ru"
        AppLanguage.EN -> "en"
        AppLanguage.DE -> "de"
    }
    val n = if (amount == Long.MIN_VALUE) Long.MAX_VALUE else kotlin.math.abs(amount)
    val lastTwo = n % 100
    val last = n % 10
    val form = when (lang) {
        "uk", "ru" -> when {
            lastTwo in 11L..14L -> "many"
            last == 1L -> "one"
            last in 2L..4L -> "few"
            else -> "many"
        }
        else -> if (n == 1L) "one" else "many"
    }
    val local = forms.firstOrNull { it.language.equals(lang, ignoreCase = true) }
    val defaults = builtInCurrencyForms(code, lang)
    val unit = when (form) {
        "one" -> local?.one?.takeIf { it.isNotBlank() } ?: defaults?.one
        "few" -> local?.few?.takeIf { it.isNotBlank() } ?: defaults?.few
        else -> local?.many?.takeIf { it.isNotBlank() } ?: defaults?.many
    } ?: fallback
    return "$amount $unit"
}
