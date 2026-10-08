package com.olehkobylianskyi.familypoints.android.i18n

enum class AppLanguage(
    val code: String,
    val label: String
) {
    UK("uk", "Українська"),
    DE("de", "Deutsch"),
    EN("en", "English"),
    RU("ru", "Русский");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code == code } ?: UK
    }
}
