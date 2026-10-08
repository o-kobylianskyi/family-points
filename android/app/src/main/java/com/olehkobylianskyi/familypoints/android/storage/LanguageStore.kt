package com.olehkobylianskyi.familypoints.android.storage

import android.content.Context
import com.olehkobylianskyi.familypoints.android.i18n.AppLanguage

class LanguageStore(context: Context) {

    private val preferences = context.getSharedPreferences(
        "family_points_settings",
        Context.MODE_PRIVATE
    )

    fun getLanguage(): AppLanguage =
        AppLanguage.fromCode(
            preferences.getString(KEY_LANGUAGE, null)
        )

    fun saveLanguage(language: AppLanguage) {
        preferences.edit()
            .putString(KEY_LANGUAGE, language.code)
            .apply()
    }

    companion object {
        private const val KEY_LANGUAGE = "language"
    }
}
