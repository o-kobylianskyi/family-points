package com.olehkobylianskyi.familypoints.android.network

import com.olehkobylianskyi.familypoints.android.data.RewardDefinitionSummary
import com.olehkobylianskyi.familypoints.android.data.TaskRewardRequestCreateRequest
import com.olehkobylianskyi.familypoints.android.data.TaskRewardRequestResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface RewardNegotiationApi {

    @GET("workspaces/{workspaceId}/rewards")
    suspend fun getRewards(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long
    ): List<RewardDefinitionSummary>

    @GET("workspaces/{workspaceId}/tasks/instances/{instanceId}/reward-requests")
    suspend fun getTaskRewardRequests(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long,
        @Path("instanceId") instanceId: Long
    ): List<TaskRewardRequestResponse>

    @POST("workspaces/{workspaceId}/tasks/instances/{instanceId}/reward-requests")
    suspend fun createTaskRewardRequest(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long,
        @Path("instanceId") instanceId: Long,
        @Body request: TaskRewardRequestCreateRequest
    ): TaskRewardRequestResponse

    @POST("workspaces/{workspaceId}/tasks/reward-requests/{requestId}/cancel")
    suspend fun cancelTaskRewardRequest(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long,
        @Path("requestId") requestId: Long
    ): TaskRewardRequestResponse
}
