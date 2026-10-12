package com.olehkobylianskyi.familypoints.android.storage

import android.content.Context

/**
 * Non-sensitive sign-in convenience value kept independently of auth tokens.
 * Session logout must not erase the last successfully used username.
 */
class LastUsernameStore(context: Context) {
    private val preferences = context.getSharedPreferences(
        "family_points_login_preferences", Context.MODE_PRIVATE
    )

    fun get(): String = preferences.getString("last_username", "").orEmpty()

    fun save(username: String) {
        val normalized = username.trim()
        if (normalized.isNotEmpty()) {
            preferences.edit().putString("last_username", normalized).apply()
        }
    }
}
