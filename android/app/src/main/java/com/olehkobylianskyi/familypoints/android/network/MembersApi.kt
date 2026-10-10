package com.olehkobylianskyi.familypoints.android.network

import com.olehkobylianskyi.familypoints.android.data.MemberGroupCreateRequest
import com.olehkobylianskyi.familypoints.android.data.MemberSaveRequest
import com.olehkobylianskyi.familypoints.android.data.GroupUpdateRequest
import com.olehkobylianskyi.familypoints.android.data.AddGroupMemberRequest
import com.olehkobylianskyi.familypoints.android.data.ChildGroupRequest
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

    @PUT("workspaces/{workspaceId}/member-groups/{groupId}")
    suspend fun updateGroup(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long,
        @Path("groupId") groupId: Long,
        @Body request: GroupUpdateRequest
    ): MembersGroupResponse

    @PUT("workspaces/{workspaceId}/member-groups/{groupId}/members")
    suspend fun addGroupMember(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long,
        @Path("groupId") groupId: Long,
        @Body request: AddGroupMemberRequest
    ): MembersGroupResponse

    @DELETE("workspaces/{workspaceId}/member-groups/{groupId}/members/{memberId}")
    suspend fun removeGroupMember(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long,
        @Path("groupId") groupId: Long,
        @Path("memberId") memberId: Long
    ): retrofit2.Response<Unit>

    @PUT("workspaces/{workspaceId}/member-groups/{groupId}/children")
    suspend fun addChildGroup(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long,
        @Path("groupId") groupId: Long,
        @Body request: ChildGroupRequest
    ): MembersGroupResponse

    @DELETE("workspaces/{workspaceId}/member-groups/{groupId}/children/{childId}")
    suspend fun removeChildGroup(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long,
        @Path("groupId") groupId: Long,
        @Path("childId") childId: Long
    ): retrofit2.Response<Unit>

    @PUT("workspaces/{workspaceId}/member-groups/{groupId}/members/{memberId}/roles")
    suspend fun setGroupMemberRoles(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long,
        @Path("groupId") groupId: Long,
        @Path("memberId") memberId: Long,
        @Body roleIds: List<Long>
    ): MembersGroupResponse

    @POST("workspaces/{workspaceId}/member-groups")
    suspend fun createGroup(
        @Header("Authorization") authorization: String,
        @Path("workspaceId") workspaceId: Long,
        @Body request: MemberGroupCreateRequest
    ): MembersGroupResponse
}
