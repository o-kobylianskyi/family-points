package com.olehkobylianskyi.familypoints.android.data

data class OwnPasswordChangeRequest(
    val currentPassword: String,
    val newPassword: String
)

data class SettingsAccountResponse(
    val id: Long?,
    val workspaceMemberId: Long,
    val username: String,
    val enabled: Boolean
)

data class SettingsAccountCreateRequest(val username: String, val password: String)
data class SettingsAccountUpdateRequest(val username: String, val enabled: Boolean)
data class SettingsPasswordResetRequest(val newPassword: String)
