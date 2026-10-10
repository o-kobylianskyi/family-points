package com.olehkobylianskyi.familypoints.android.network

import com.olehkobylianskyi.familypoints.android.data.MemberGroupSummary
import com.olehkobylianskyi.familypoints.android.data.PointTypeResponse
import com.olehkobylianskyi.familypoints.android.data.TaskDefinitionCreateRequest
import com.olehkobylianskyi.familypoints.android.data.TaskDefinitionResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface CreateTaskApi {

    @GET("workspaces/{workspaceId}/economy/point-types")
    suspend fun getPointTypes(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long
    ): List<PointTypeResponse>

    @GET("workspaces/{workspaceId}/member-groups")
    suspend fun getMemberGroups(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long
    ): List<MemberGroupSummary>

    @POST("workspaces/{workspaceId}/tasks/definitions")
    suspend fun createTask(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long,
        @Body request: TaskDefinitionCreateRequest
    ): TaskDefinitionResponse
}
