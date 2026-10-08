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

    fun clear() {
        preferences.edit().clear().apply()
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
    }
}
