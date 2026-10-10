package com.olehkobylianskyi.familypoints.android.data

data class WorkspaceRoleResponse(
    val id: Long,
    val code: String?,
    val name: String
)

data class MemberSaveRequest(
    val name: String,
    val memberType: String,
    val workspaceRoleId: Long
)

data class MemberGroupChildResponse(
    val groupId: Long,
    val groupName: String
)

data class MemberGroupMemberResponse(
    val memberId: Long,
    val memberName: String
)

data class MemberGroupBalanceResponse(
    val pointTypeId: Long,
    val code: String?,
    val name: String?,
    val amount: Long
)

data class MembersGroupResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val members: List<MemberGroupMemberResponse>?,
    val childGroups: List<MemberGroupChildResponse>?,
    val balances: List<MemberGroupBalanceResponse>?
)

data class MemberGroupCreateRequest(
    val name: String,
    val description: String? = null
)

data class MembersPageData(
    val members: List<WorkspaceMemberResponse>,
    val groups: List<MembersGroupResponse>,
    val roles: List<WorkspaceRoleResponse>
)
