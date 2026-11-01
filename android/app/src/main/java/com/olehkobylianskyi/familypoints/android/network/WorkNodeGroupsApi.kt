package com.olehkobylianskyi.familypoints.android.network

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/** Task hierarchy folders. Never confuse with member groups or catalog categories. */
data class WorkNodeGroupDto(
    val id: Long,
    val parentGroupId: Long?,
    val title: String,
    val description: String?,
    val sortOrder: Int,
    val active: Boolean
)

data class WorkNodeGroupMutation(
    val title: String,
    val description: String?,
    val parentGroupId: Long?,
    val sortOrder: Int
)

interface WorkNodeGroupsApi {
    @GET("workspaces/{workspaceId}/work-node-groups")
    suspend fun list(
        @Header("Authorization") auth: String,
        @Path("workspaceId") workspaceId: Long
    ): List<WorkNodeGroupDto>

    @POST("workspaces/{workspaceId}/work-node-groups")
    suspend fun create(
        @Header("Authorization") auth: String,
        @Path("workspaceId") workspaceId: Long,
        @Body request: WorkNodeGroupMutation
    ): WorkNodeGroupDto

    @PUT("workspaces/{workspaceId}/work-node-groups/{groupId}")
    suspend fun edit(
        @Header("Authorization") auth: String,
        @Path("workspaceId") workspaceId: Long,
        @Path("groupId") groupId: Long,
        @Body request: WorkNodeGroupMutation
    ): WorkNodeGroupDto

    @DELETE("workspaces/{workspaceId}/work-node-groups/{groupId}")
    suspend fun deactivate(
        @Header("Authorization") auth: String,
        @Path("workspaceId") workspaceId: Long,
        @Path("groupId") groupId: Long
    ): retrofit2.Response<Void>
}
