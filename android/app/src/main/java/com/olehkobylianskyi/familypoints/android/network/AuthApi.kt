package com.olehkobylianskyi.familypoints.android.network

import com.olehkobylianskyi.familypoints.android.data.CurrentUserResponse
import com.olehkobylianskyi.familypoints.android.data.LoginRequest
import com.olehkobylianskyi.familypoints.android.data.LoginResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface AuthApi {

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): LoginResponse

    @GET("api/me")
    suspend fun getCurrentUser(
        @Header("Authorization") authorization: String
    ): CurrentUserResponse

    @GET("api/me/reputation")
    suspend fun getMyReputation(
        @Header("Authorization") authorization: String
    ): ReputationBalance

    @GET("api/me/workspaces")
    suspend fun listWorkspaces(
        @Header("Authorization") authorization: String
    ): List<AccountWorkspaceChoice>

    @POST("api/me/workspaces/{workspaceId}/activate")
    suspend fun activateWorkspace(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long
    ): LoginResponse

    @POST("auth/refresh")
    suspend fun refresh(
        @Header("Authorization") authorization: String
    ): LoginResponse
}

data class AccountWorkspaceChoice(
    val workspaceId: Long,
    val workspaceName: String,
    val memberId: Long,
    val memberName: String,
    val active: Boolean
)

data class ReputationBalance(
    val workspaceId: Long,
    val memberId: Long,
    val balance: Long
)
