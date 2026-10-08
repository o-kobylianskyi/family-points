package com.olehkobylianskyi.familypoints.android.data

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: Long
)

data class CurrentUserResponse(
    val username: String,
    val memberId: Long,
    val memberName: String,
    val memberType: String,
    val workspaceId: Long,
    val workspaceRoleId: Long,
    val workspaceRoleCode: String,
    val workspaceRoleName: String,
    val permissions: List<String>
)
