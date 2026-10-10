package com.olehkobylianskyi.familypoints.android.network

import com.olehkobylianskyi.familypoints.android.data.MemberGroupCreateRequest
import com.olehkobylianskyi.familypoints.android.data.MemberSaveRequest
import com.olehkobylianskyi.familypoints.android.data.MembersGroupResponse
import com.olehkobylianskyi.familypoints.android.data.WorkspaceMemberResponse
import com.olehkobylianskyi.familypoints.android.data.WorkspaceRoleResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface MembersApi {
    @GET("workspaces/{workspaceId}/members")
    suspend fun getMembers(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long
    ): List<WorkspaceMemberResponse>

    @GET("workspaces/{workspaceId}/roles")
    suspend fun getRoles(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long
    ): List<WorkspaceRoleResponse>

    @GET("workspaces/{workspaceId}/member-groups")
    suspend fun getGroups(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long
    ): List<MembersGroupResponse>

    @POST("workspaces/{workspaceId}/members")
    suspend fun createMember(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long,
        @Body request: MemberSaveRequest
    ): WorkspaceMemberResponse

    @PUT("workspaces/{workspaceId}/members/{memberId}")
    suspend fun updateMember(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long,
        @Path("memberId") memberId: Long,
        @Body request: MemberSaveRequest
    ): WorkspaceMemberResponse

    @DELETE("workspaces/{workspaceId}/members/{memberId}")
    suspend fun deleteMember(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long,
        @Path("memberId") memberId: Long
    ): retrofit2.Response<Unit>

    @POST("workspaces/{workspaceId}/member-groups")
    suspend fun createGroup(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long,
        @Body request: MemberGroupCreateRequest
    ): MembersGroupResponse
}
