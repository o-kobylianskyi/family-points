package com.olehkobylianskyi.familypoints.android.data

import com.olehkobylianskyi.familypoints.android.network.ApiClient
import com.olehkobylianskyi.familypoints.android.storage.TokenStore
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class MembersRepository(private val tokenStore: TokenStore) {
    private fun authorization(): String =
        "Bearer " + (tokenStore.getAccessToken()
            ?: throw IllegalStateException("Missing access token"))

    suspend fun load(workspaceId: Long): MembersPageData = coroutineScope {
        val auth = authorization()
        val members = async { ApiClient.membersApi.getMembers(auth, workspaceId) }
        val groups = async { ApiClient.membersApi.getGroups(auth, workspaceId) }
        val roles = async { ApiClient.membersApi.getRoles(auth, workspaceId) }
        MembersPageData(members.await(), groups.await(), roles.await())
    }

    suspend fun save(workspaceId: Long, memberId: Long?, request: MemberSaveRequest) {
        val auth = authorization()
        if (memberId == null) {
            ApiClient.membersApi.createMember(auth, workspaceId, request)
        } else {
            ApiClient.membersApi.updateMember(auth, workspaceId, memberId, request)
        }
    }

    suspend fun delete(workspaceId: Long, memberId: Long) {
        ApiClient.membersApi.deleteMember(authorization(), workspaceId, memberId)
    }

    suspend fun updateGroup(workspaceId: Long, groupId: Long, request: GroupUpdateRequest) {
        ApiClient.membersApi.updateGroup(authorization(), workspaceId, groupId, request)
    }

    suspend fun addGroupMember(workspaceId: Long, groupId: Long, memberId: Long) {
        ApiClient.membersApi.addGroupMember(
            authorization(), workspaceId, groupId, AddGroupMemberRequest(memberId)
        )
    }

    suspend fun removeGroupMember(workspaceId: Long, groupId: Long, memberId: Long) {
        ApiClient.membersApi.removeGroupMember(authorization(), workspaceId, groupId, memberId)
    }

    suspend fun addChildGroup(workspaceId: Long, groupId: Long, childId: Long) {
        ApiClient.membersApi.addChildGroup(
            authorization(), workspaceId, groupId, ChildGroupRequest(childId)
        )
    }

    suspend fun removeChildGroup(workspaceId: Long, groupId: Long, childId: Long) {
        ApiClient.membersApi.removeChildGroup(authorization(), workspaceId, groupId, childId)
    }

    suspend fun setGroupMemberRoles(workspaceId: Long, groupId: Long, memberId: Long, roleIds: List<Long>) {
        ApiClient.membersApi.setGroupMemberRoles(authorization(), workspaceId, groupId, memberId, roleIds)
    }

    suspend fun saveGroupRole(
        workspaceId: Long,
        groupId: Long,
        roleId: Long?,
        request: GroupRoleSaveRequest,
        permissions: Map<String, String>?
    ) {
        val auth = authorization()
        val saved = if (roleId == null) {
            ApiClient.membersApi.createGroupRole(auth, workspaceId, groupId, request)
        } else {
            ApiClient.membersApi.updateGroupRole(auth, workspaceId, groupId, roleId, request)
        }
        if (permissions != null) {
            val resolvedId = roleId ?: saved.roles.orEmpty()
                .firstOrNull { it.name == request.name && it.visibility == "PRIVATE" }?.id
                ?: error("Created role ID could not be resolved; refresh before retrying")
            updateRolePermissions(workspaceId, groupId, resolvedId, permissions)
        }
    }

    suspend fun deleteGroupRole(workspaceId: Long, groupId: Long, roleId: Long) {
        ApiClient.membersApi.deleteGroupRole(authorization(), workspaceId, groupId, roleId)
    }

    suspend fun getGroupPermissions(workspaceId: Long, groupId: Long): List<GroupPermissionGrantResponse> =
        ApiClient.membersApi.getGroupPermissions(authorization(), workspaceId, groupId)

    suspend fun updateRolePermissions(
        workspaceId: Long,
        groupId: Long,
        roleId: Long,
        requested: Map<String, String>
    ) {
        val auth = authorization()
        val existing = ApiClient.membersApi.getGroupPermissions(auth, workspaceId, groupId)
            .filter { it.roleId == roleId }
        existing.filter { requested[it.permission] != it.scope }.forEach {
            ApiClient.membersApi.removeGroupPermission(auth, workspaceId, groupId, it.id)
        }
        requested.forEach { (permission, scope) ->
            if (existing.none { it.permission == permission && it.scope == scope }) {
                ApiClient.membersApi.addGroupPermission(
                    auth, workspaceId, groupId, roleId,
                    GroupPermissionGrantRequest(permission, scope)
                )
            }
        }
    }

    suspend fun createGroup(workspaceId: Long, name: String) {
        ApiClient.membersApi.createGroup(
            authorization(), workspaceId, MemberGroupCreateRequest(name)
        )
    }
}
