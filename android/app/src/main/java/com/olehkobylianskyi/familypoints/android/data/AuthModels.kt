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
