package com.olehkobylianskyi.familypoints.android.storage

import android.content.Context

class TokenStore(context: Context) {

    private val preferences = context.getSharedPreferences(
        "family_points_auth",
        Context.MODE_PRIVATE
    )

    fun saveAccessToken(token: String) {
        preferences.edit()
            .putString(KEY_ACCESS_TOKEN, token)
            .apply()
    }

    fun getAccessToken(): String? =
        preferences.getString(KEY_ACCESS_TOKEN, null)

    fun saveLastActivityAt(timestamp: Long) {
        preferences.edit()
            .putLong(KEY_LAST_ACTIVITY_AT, timestamp)
            .apply()
    }

    fun getLastActivityAt(): Long =
        preferences.getLong(KEY_LAST_ACTIVITY_AT, 0L)

    fun clear() {
        preferences.edit().clear().apply()
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_LAST_ACTIVITY_AT = "last_activity_at"
    }
}
