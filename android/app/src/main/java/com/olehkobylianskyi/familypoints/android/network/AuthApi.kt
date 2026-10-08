package com.olehkobylianskyi.familypoints.android.network

import com.olehkobylianskyi.familypoints.android.data.CurrentUserResponse
import com.olehkobylianskyi.familypoints.android.data.LoginRequest
import com.olehkobylianskyi.familypoints.android.data.LoginResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApi {

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): LoginResponse

    @GET("api/me")
    suspend fun getCurrentUser(
        @Header("Authorization") authorization: String
    ): CurrentUserResponse

    @POST("auth/refresh")
    suspend fun refresh(
        @Header("Authorization") authorization: String
    ): LoginResponse
}
