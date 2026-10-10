package com.olehkobylianskyi.familypoints.android.network

import com.olehkobylianskyi.familypoints.android.data.*
import retrofit2.http.*

interface SettingsApi {
    @PUT("api/me/password")
    suspend fun changePassword(
        @Header("Authorization") authorization: String,
        @Body request: OwnPasswordChangeRequest
    ): retrofit2.Response<Unit>

    @GET("workspaces/{workspaceId}/accounts")
    suspend fun accounts(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long
    ): List<SettingsAccountResponse>

    @POST("workspaces/{workspaceId}/members/{memberId}/account")
    suspend fun createAccount(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long,
        @Path("memberId") memberId: Long,
        @Body request: SettingsAccountCreateRequest
    ): SettingsAccountResponse

    @PUT("workspaces/{workspaceId}/members/{memberId}/account")
    suspend fun updateAccount(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long,
        @Path("memberId") memberId: Long,
        @Body request: SettingsAccountUpdateRequest
    ): SettingsAccountResponse

    @PUT("workspaces/{workspaceId}/members/{memberId}/account/password")
    suspend fun resetAccountPassword(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long,
        @Path("memberId") memberId: Long,
        @Body request: SettingsPasswordResetRequest
    ): retrofit2.Response<Unit>
}
