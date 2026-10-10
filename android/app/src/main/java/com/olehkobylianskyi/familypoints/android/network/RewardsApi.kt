package com.olehkobylianskyi.familypoints.android.network

import com.olehkobylianskyi.familypoints.android.data.RewardCategoryCreateRequest
import com.olehkobylianskyi.familypoints.android.data.RewardCategorySummary
import com.olehkobylianskyi.familypoints.android.data.RewardDefinitionCreateRequest
import com.olehkobylianskyi.familypoints.android.data.RewardDefinitionSummary
import com.olehkobylianskyi.familypoints.android.data.RewardObligationSummary
import com.olehkobylianskyi.familypoints.android.data.RewardPurchaseSummary
import com.olehkobylianskyi.familypoints.android.data.RewardRequestCreateRequest
import com.olehkobylianskyi.familypoints.android.data.RewardRequestReviewRequest
import com.olehkobylianskyi.familypoints.android.data.RewardRequestSummary
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface RewardsApi {

    @GET("workspaces/{workspaceId}/rewards")
    suspend fun getRewards(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long
    ): List<RewardDefinitionSummary>

    @POST("workspaces/{workspaceId}/rewards")
    suspend fun createReward(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long,
        @Body request: RewardDefinitionCreateRequest
    ): RewardDefinitionSummary

    @GET("workspaces/{workspaceId}/rewards/categories")
    suspend fun getCategories(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long
    ): List<RewardCategorySummary>

    @POST("workspaces/{workspaceId}/rewards/categories")
    suspend fun createCategory(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long,
        @Body request: RewardCategoryCreateRequest
    ): RewardCategorySummary

    @GET("workspaces/{workspaceId}/rewards/requests")
    suspend fun getRequests(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long
    ): List<RewardRequestSummary>

    @POST("workspaces/{workspaceId}/rewards/requests")
    suspend fun createRequest(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long,
        @Body request: RewardRequestCreateRequest
    ): RewardRequestSummary

    @POST("workspaces/{workspaceId}/rewards/requests/{requestId}/approve")
    suspend fun approveRequest(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long,
        @Path("requestId") requestId: Long,
        @Body request: RewardRequestReviewRequest
    ): RewardRequestSummary

    @POST("workspaces/{workspaceId}/rewards/requests/{requestId}/reject")
    suspend fun rejectRequest(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long,
        @Path("requestId") requestId: Long
    ): RewardRequestSummary

    @POST("workspaces/{workspaceId}/rewards/requests/{requestId}/refresh")
    suspend fun refreshRequest(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long,
        @Path("requestId") requestId: Long
    ): RewardRequestSummary

    @POST("workspaces/{workspaceId}/rewards/{rewardId}/purchase")
    suspend fun purchaseReward(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long,
        @Path("rewardId") rewardId: Long
    ): RewardPurchaseSummary

    @POST("workspaces/{workspaceId}/rewards/requests/{requestId}/purchase")
    suspend fun purchaseRequest(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long,
        @Path("requestId") requestId: Long
    ): RewardPurchaseSummary

    @GET("workspaces/{workspaceId}/rewards/purchases")
    suspend fun getPurchases(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long
    ): List<RewardPurchaseSummary>

    @GET("workspaces/{workspaceId}/rewards/obligations/open")
    suspend fun getOpenObligations(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long
    ): List<RewardObligationSummary>
}
