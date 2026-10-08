package com.olehkobylianskyi.familypoints.android.auth

import android.util.Base64
import com.olehkobylianskyi.familypoints.android.data.CurrentUserResponse
import com.olehkobylianskyi.familypoints.android.data.LoginRequest
import com.olehkobylianskyi.familypoints.android.network.ApiClient
import com.olehkobylianskyi.familypoints.android.storage.TokenStore
import org.json.JSONObject

class AuthSessionManager(
    private val tokenStore: TokenStore
) {
    companion object {
        private const val IDLE_TIMEOUT_MS = 60 * 60 * 1000L
        private const val REFRESH_THRESHOLD_MS = 15 * 60 * 1000L
        private const val ACTIVITY_WRITE_THROTTLE_MS = 30 * 1000L
    }

    private var lastActivityWrite = 0L
    private var refreshInFlight = false

    suspend fun login(
        username: String,
        password: String
    ): CurrentUserResponse {
        val loginResponse = ApiClient.authApi.login(
            LoginRequest(username, password)
        )

        tokenStore.saveAccessToken(loginResponse.accessToken)
        tokenStore.saveLastActivityAt(System.currentTimeMillis())

        return try {
            val user = ApiClient.authApi.getCurrentUser(
                bearer(loginResponse.accessToken)
            )
            markActivity(force = true)
            user
        } catch (error: Exception) {
            clearSession()
            throw error
        }
    }

    suspend fun restoreSession(): CurrentUserResponse? {
        val token = tokenStore.getAccessToken() ?: return null
        val lastActivity = tokenStore.getLastActivityAt()
        val now = System.currentTimeMillis()

        if (
            lastActivity > 0L &&
            now - lastActivity >= IDLE_TIMEOUT_MS
        ) {
            clearSession()
            return null
        }

        return try {
            val user = ApiClient.authApi.getCurrentUser(
                bearer(token)
            )
            markActivity(force = true)
            user
        } catch (_: Exception) {
            clearSession()
            null
        }
    }

    suspend fun refreshIfNeeded(): Boolean {
        val token = tokenStore.getAccessToken() ?: return false
        val now = System.currentTimeMillis()
        val lastActivity = tokenStore.getLastActivityAt()

        if (
            lastActivity <= 0L ||
            now - lastActivity >= IDLE_TIMEOUT_MS
        ) {
            clearSession()
            return false
        }

        val expiresAt = getTokenExpirationMs(token)
            ?: run {
                clearSession()
                return false
            }

        val recentlyActive =
            now - lastActivity < REFRESH_THRESHOLD_MS

        if (
            expiresAt - now <= REFRESH_THRESHOLD_MS &&
            recentlyActive &&
            !refreshInFlight
        ) {
            refreshInFlight = true
            try {
                val refreshed = ApiClient.authApi.refresh(
                    bearer(token)
                )
                tokenStore.saveAccessToken(refreshed.accessToken)
            } catch (_: Exception) {
                clearSession()
                return false
            } finally {
                refreshInFlight = false
            }
        }

        return true
    }

    fun markActivity(force: Boolean = false) {
        if (tokenStore.getAccessToken() == null) return

        val now = System.currentTimeMillis()
        if (
            !force &&
            now - lastActivityWrite < ACTIVITY_WRITE_THROTTLE_MS
        ) {
            return
        }

        lastActivityWrite = now
        tokenStore.saveLastActivityAt(now)
    }

    fun clearSession() {
        tokenStore.clear()
    }

    fun hasToken(): Boolean =
        !tokenStore.getAccessToken().isNullOrBlank()

    private fun bearer(token: String): String =
        "Bearer $token"

    private fun getTokenExpirationMs(token: String): Long? {
        return try {
            val payloadPart = token.split(".").getOrNull(1)
                ?: return null

            val decoded = Base64.decode(
                payloadPart,
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
            )

            val payload = JSONObject(String(decoded))
            if (!payload.has("exp")) {
                null
            } else {
                payload.getLong("exp") * 1000L
            }
        } catch (_: Exception) {
            null
        }
    }
}
