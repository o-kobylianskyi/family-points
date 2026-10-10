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
    val memberName: String,
    val roleIds: List<Long>? = null
)

data class MemberGroupBalanceResponse(
    val pointTypeId: Long,
    val code: String?,
    val name: String?,
    val amount: Long
)

data class GroupRoleResponse(
    val id: Long,
    val name: String,
    val description: String? = null,
    val roleSetId: Long? = null,
    val systemCode: String?,
    val visibility: String?,
    val systemDefault: Boolean?
)

data class GroupPermissionGrantResponse(
    val id: Long,
    val roleId: Long,
    val permission: String,
    val scope: String
)

data class GroupPermissionGrantRequest(
    val permission: String,
    val scope: String
)

data class GroupRoleSaveRequest(
    val name: String,
    val description: String?,
    val roleSetId: Long?
)

data class GroupUpdateRequest(
    val name: String,
    val description: String?,
    val showInNavigation: Boolean
)

data class AddGroupMemberRequest(
    val memberId: Long,
    val roleIds: List<Long> = emptyList()
)

data class ChildGroupRequest(val childGroupId: Long)

data class MembersGroupResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val showInNavigation: Boolean? = null,
    val roles: List<GroupRoleResponse>? = null,
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
