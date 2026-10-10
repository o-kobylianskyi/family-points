package com.olehkobylianskyi.familypoints.android.network

import com.olehkobylianskyi.familypoints.android.data.PointBalanceResponse
import com.olehkobylianskyi.familypoints.android.data.PointOperationRequest
import com.olehkobylianskyi.familypoints.android.data.PointTransactionResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface PointsApi {

    @GET("workspaces/{workspaceId}/members/{memberId}/points/balance")
    suspend fun getBalance(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long,
        @Path("memberId") memberId: Long
    ): PointBalanceResponse

    @GET("workspaces/{workspaceId}/members/{memberId}/points/history")
    suspend fun getHistory(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long,
        @Path("memberId") memberId: Long
    ): List<PointTransactionResponse>

    @POST("workspaces/{workspaceId}/members/{memberId}/points/{operation}")
    suspend fun operate(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long,
        @Path("memberId") memberId: Long,
        @Path("operation") operation: String,
        @Body request: PointOperationRequest
    ): PointTransactionResponse
}
