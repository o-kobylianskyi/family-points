package com.olehkobylianskyi.familypoints.android.i18n

data class PointNameForms(val language: String, val one: String, val few: String, val many: String)

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
    val n = kotlin.math.abs(amount)
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
    val local = forms.firstOrNull { it.language == lang }
    val unit = if (local != null) {
        when (form) {
            "one" -> local.one
            "few" -> local.few
            else -> local.many
        }
    } else if (code?.uppercase() == "POINTS") {
        when (language) {
            AppLanguage.UK -> when (form) {
                "one" -> "бал"
                "few" -> "бали"
                else -> "балів"
            }
            AppLanguage.RU -> when (form) {
                "one" -> "балл"
                "few" -> "балла"
                else -> "баллов"
            }
            AppLanguage.EN -> if (form == "one") "point" else "points"
            AppLanguage.DE -> if (form == "one") "Punkt" else "Punkte"
        }
    } else fallback
    return "$amount $unit"
}
