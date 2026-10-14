package com.olehkobylianskyi.familypoints.android.auth

import android.util.Base64
import com.olehkobylianskyi.familypoints.android.data.CurrentUserResponse
import com.olehkobylianskyi.familypoints.android.data.LoginRequest
import com.olehkobylianskyi.familypoints.android.network.ApiClient
import com.olehkobylianskyi.familypoints.android.storage.TokenStore
import com.olehkobylianskyi.familypoints.android.storage.OfflineAccountStore
import com.olehkobylianskyi.familypoints.android.network.ServerReachability
import com.olehkobylianskyi.familypoints.android.network.ServerStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import retrofit2.HttpException
import org.json.JSONObject

class WorkspaceSelectionRequiredException : Exception("Choose a family to continue")

class AuthSessionManager(
    private val tokenStore: TokenStore,
    private val offlineAccountStore: OfflineAccountStore
) {
    companion object {
        private const val IDLE_TIMEOUT_MS = 60 * 60 * 1000L
        private const val REFRESH_THRESHOLD_MS = 15 * 60 * 1000L
        private const val ACTIVITY_WRITE_THROTTLE_MS = 30 * 1000L
    }

    private var lastActivityWrite = 0L
    private var refreshInFlight = false
    private var offlineAuthenticated = false
    private var activeUsername: String? = null

    fun isOfflineAuthenticated(): Boolean = offlineAuthenticated

    /**
     * Use the same username/password online and offline. Offline authentication
     * succeeds ONLY for accounts enrolled by a prior successful online login.
     */
    suspend fun login(username: String, password: String): CurrentUserResponse {
        if (ServerReachability.status.value == ServerStatus.OFFLINE) {
            return loginOffline(username, password)
        }
        return try {
            val loginResponse = ApiClient.authApi.login(LoginRequest(username, password))
            tokenStore.saveAccessToken(loginResponse.accessToken)
            // Global JWT authenticates an account, not a member profile.
            // Do not call /api/me until a workspace-scoped JWT is selected.
            val choices = try {
                ApiClient.authApi.listWorkspaces(bearer(loginResponse.accessToken))
            } catch (error: Exception) {
                tokenStore.clear()
                throw error
            }
            offlineAuthenticated = false
            activeUsername = username
            tokenStore.saveLastActivityAt(System.currentTimeMillis())
            if (choices.isEmpty() || choices.size > 1) throw WorkspaceSelectionRequiredException()
            val user = try {
                switchWorkspace(choices.single().workspaceId)
            } catch (error: Exception) {
                tokenStore.clear()
                throw error
            }
            // A verified online password enrolls/refreshes the local verifier.
            withContext(Dispatchers.Default) {
                offlineAccountStore.enroll(user, password)
            }
            offlineAuthenticated = false
            activeUsername = user.username
            tokenStore.saveLastActivityAt(System.currentTimeMillis())
            markActivity(force = true)
            user
        } catch (error: WorkspaceSelectionRequiredException) {
            throw error
        } catch (error: IOException) {
            // Never substitute an offline login after a successful online
            // authentication and a subsequent workspace-selection network error.
            if (tokenStore.getAccessToken() != null) throw error
            loginOffline(username, password)
        } catch (error: HttpException) {
            if (error.code() in listOf(502, 503, 504) && tokenStore.getAccessToken() == null) {
                loginOffline(username, password)
            } else {
                throw error
            }
        }
    }

    suspend fun registerAccount(username: String, password: String) {
        check(ServerReachability.status.value == ServerStatus.ONLINE) { "Registration requires network" }
        ApiClient.authApi.registerAccount(
            com.olehkobylianskyi.familypoints.android.network.AccountRegistrationRequest(username, password)
        )
    }

    suspend fun createFirstFamily(familyName: String, ownerName: String): CurrentUserResponse {
        check(!offlineAuthenticated && ServerReachability.status.value == ServerStatus.ONLINE)
        val token = tokenStore.getAccessToken() ?: error("Not signed in")
        val created = ApiClient.authApi.createFirstFamily(
            bearer(token),
            com.olehkobylianskyi.familypoints.android.network.FirstFamilyRequest(familyName, ownerName)
        )
        return switchWorkspace(created.workspaceId)
    }

    fun hasUnselectedWorkspace(): Boolean = !offlineAuthenticated &&
            tokenStore.getAccessToken()?.let { getSelectedWorkspaceId(it) == null } == true

    private fun getSelectedWorkspaceId(token: String): Long? {
        return try {
        val segment = token.split(".").getOrNull(1) ?: return null
        val payload = JSONObject(String(Base64.decode(segment,
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)))
        if (payload.has("workspaceId")) payload.getLong("workspaceId") else null
        } catch (_: Exception) { null }
    }

    /** Switch only while online; never use a cached JWT or an offline profile to switch families. */
    suspend fun listWorkspaces(): List<com.olehkobylianskyi.familypoints.android.network.AccountWorkspaceChoice> {
        check(!offlineAuthenticated && ServerReachability.status.value == ServerStatus.ONLINE) {
            "Workspace switching requires an online session"
        }
        val token = tokenStore.getAccessToken() ?: error("Not signed in")
        return ApiClient.authApi.listWorkspaces(bearer(token))
    }

    suspend fun switchWorkspace(workspaceId: Long): CurrentUserResponse {
        check(!offlineAuthenticated && ServerReachability.status.value == ServerStatus.ONLINE) {
            "Workspace switching requires an online session"
        }
        val token = tokenStore.getAccessToken() ?: error("Not signed in")
        val result = ApiClient.authApi.activateWorkspace(bearer(token), workspaceId)
        // Verify server-side workspace identity BEFORE replacing the active JWT.
        val next = ApiClient.authApi.getCurrentUser(bearer(result.accessToken))
        check(next.workspaceId == workspaceId) { "Unexpected workspace identity" }
        tokenStore.saveAccessToken(result.accessToken)
        tokenStore.saveLastActivityAt(System.currentTimeMillis())
        // Keep the password verifier but replace the cached offline identity with the
        // most recently verified workspace. Never grant offline switching by JWT alone.
        withContext(Dispatchers.Default) { offlineAccountStore.refreshProfile(next) }
        activeUsername = next.username
        markActivity(force = true)
        return next
    }

    private suspend fun loginOffline(username: String, password: String): CurrentUserResponse {
        val user = withContext(Dispatchers.Default) {
            offlineAccountStore.authenticate(username, password)
        }
        // Never use a previously cached JWT for writes under an offline identity.
        tokenStore.clear()
        tokenStore.saveLastActivityAt(System.currentTimeMillis())
        lastActivityWrite = 0L
        offlineAuthenticated = true
        activeUsername = user.username
        markActivity(force = true)
        return user
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
            if (getSelectedWorkspaceId(token) == null) {
                ApiClient.authApi.listWorkspaces(bearer(token))
                return null
            }
            val user = ApiClient.authApi.getCurrentUser(
                bearer(token)
            )
            withContext(Dispatchers.Default) { offlineAccountStore.refreshProfile(user) }
            offlineAuthenticated = false
            activeUsername = user.username
            markActivity(force = true)
            user
        } catch (error: java.io.IOException) {
            // Transport failure is not an explicit logout or server revocation.
            // Offline profile access still requires a future local PIN challenge.
            null
        } catch (error: retrofit2.HttpException) {
            if (error.code() !in listOf(502, 503, 504)) clearSession()
            null
        } catch (_: Exception) {
            clearSession()
            null
        }
    }

    suspend fun refreshIfNeeded(): Boolean {
        val now = System.currentTimeMillis()
        if (offlineAuthenticated) {
            val last = tokenStore.getLastActivityAt()
            if (last > 0L && now >= last && now - last < IDLE_TIMEOUT_MS) return true
            clearSession()
            return false
        }
        val token = tokenStore.getAccessToken() ?: return false
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
            } catch (_: java.io.IOException) {
                // A temporarily unreachable server must not log out an active user.
                // The token remains subject to idle timeout and online validation.
                return true
            } catch (error: retrofit2.HttpException) {
                if (error.code() in listOf(502, 503, 504)) return true
                clearSession()
                return false
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
        if (tokenStore.getAccessToken() == null && !offlineAuthenticated) return

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

    /**
     * Logging out ends the session but keeps encrypted offline enrollment.
     * A server-side unauthorized response can invalidate the current enrollment.
     */
    fun clearSession(invalidateOfflineAccount: Boolean = false) {
        tokenStore.clear()
        if (invalidateOfflineAccount) {
            activeUsername?.let { offlineAccountStore.remove(it) }
        }
        offlineAuthenticated = false
        activeUsername = null
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
