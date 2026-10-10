package com.olehkobylianskyi.familypoints.android.data

import com.olehkobylianskyi.familypoints.android.network.ApiClient
import com.olehkobylianskyi.familypoints.android.storage.TokenStore

class SettingsRepository(private val store: TokenStore) {
    private fun auth(): String = "Bearer " + (store.getAccessToken()
        ?: error("Missing access token"))

    suspend fun changePassword(current: String, next: String) {
        ApiClient.settingsApi.changePassword(auth(), OwnPasswordChangeRequest(current, next))
    }

    suspend fun loadAccounts(workspaceId: Long): List<SettingsAccountResponse> =
        ApiClient.settingsApi.accounts(auth(), workspaceId)

    suspend fun createAccount(workspaceId: Long, memberId: Long, username: String, password: String) {
        ApiClient.settingsApi.createAccount(auth(), workspaceId, memberId,
            SettingsAccountCreateRequest(username, password))
    }

    suspend fun updateAccount(workspaceId: Long, memberId: Long, username: String, enabled: Boolean) {
        ApiClient.settingsApi.updateAccount(auth(), workspaceId, memberId,
            SettingsAccountUpdateRequest(username, enabled))
    }

    suspend fun resetAccountPassword(workspaceId: Long, memberId: Long, password: String) {
        ApiClient.settingsApi.resetAccountPassword(auth(), workspaceId, memberId,
            SettingsPasswordResetRequest(password))
    }
}
