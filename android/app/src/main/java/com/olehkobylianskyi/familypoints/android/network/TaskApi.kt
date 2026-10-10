package com.olehkobylianskyi.familypoints.android.network

import com.olehkobylianskyi.familypoints.android.data.TaskDefinitionResponse
import com.olehkobylianskyi.familypoints.android.data.TaskInstanceResponse
import com.olehkobylianskyi.familypoints.android.data.TaskParticipantResponse
import com.olehkobylianskyi.familypoints.android.data.WorkspaceMemberResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface TaskApi {

    @GET("workspaces/{workspaceId}/members")
    suspend fun getWorkspaceMembers(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long
    ): List<WorkspaceMemberResponse>

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

    @GET("workspaces/{workspaceId}/tasks/definitions")
    suspend fun getDefinitions(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long
    ): List<TaskDefinitionResponse>

    @GET("workspaces/{workspaceId}/tasks/definitions/views/{view}")
    suspend fun getDefinitionsByView(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long,
        @Path("view") view: String
    ): List<TaskDefinitionResponse>

    @GET("workspaces/{workspaceId}/tasks/definitions/{definitionId}/participants")
    suspend fun getParticipants(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long,
        @Path("definitionId") definitionId: Long
    ): List<TaskParticipantResponse>

    @POST("workspaces/{workspaceId}/tasks/instances/{instanceId}/{action}")
    suspend fun changeStatus(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long,
        @Path("instanceId") instanceId: Long,
        @Path("action") action: String
    ): TaskInstanceResponse

    @POST("workspaces/{workspaceId}/tasks/definitions/{definitionId}/claim")
    suspend fun claim(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long,
        @Path("definitionId") definitionId: Long,
        @Query("date") date: String,
        @Query("memberId") memberId: Long
    ): TaskInstanceResponse
}
