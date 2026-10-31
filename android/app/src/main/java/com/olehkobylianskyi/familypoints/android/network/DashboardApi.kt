package com.olehkobylianskyi.familypoints.android.network

import com.olehkobylianskyi.familypoints.android.data.PointBalanceResponse
import com.olehkobylianskyi.familypoints.android.data.TaskDefinitionResponse
import com.olehkobylianskyi.familypoints.android.data.TaskInstanceResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface DashboardApi {

    @GET("workspaces/{workspaceId}/members/{memberId}/points/balance")
    suspend fun getMemberBalance(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long,
        @Path("memberId") memberId: Long
    ): PointBalanceResponse

    @GET("workspaces/{workspaceId}/tasks/members/{memberId}")
    suspend fun getMemberTasks(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long,
        @Path("memberId") memberId: Long,
        @Query("date") date: String
    ): List<TaskInstanceResponse>

    @GET("workspaces/{workspaceId}/tasks/open")
    suspend fun getOpenTasks(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long,
        @Query("date") date: String,
        @Query("memberId") memberId: Long
    ): List<TaskDefinitionResponse>
}
