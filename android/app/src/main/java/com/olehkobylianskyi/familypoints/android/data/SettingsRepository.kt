package com.olehkobylianskyi.familypoints.android.data

import com.olehkobylianskyi.familypoints.android.network.ApiClient
import com.olehkobylianskyi.familypoints.android.storage.TokenStore

class SettingsRepository(private val store: TokenStore) {
    private fun auth(): String = "Bearer " + (store.getAccessToken()
        ?: error("Missing access token"))

    suspend fun pointTypes(workspaceId: Long): List<PointTypeResponse> =
        ApiClient.settingsApi.pointTypes(auth(), workspaceId)

    suspend fun pointNameForms(workspaceId: Long, typeId: Long): List<PointNameFormDto> =
        ApiClient.settingsApi.pointNameForms(auth(), workspaceId, typeId)

    suspend fun savePointNameForms(workspaceId: Long, typeId: Long, language: String, form: PointNameFormSave) {
        ApiClient.settingsApi.savePointNameForms(auth(), workspaceId, typeId, language, form)
    }

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
