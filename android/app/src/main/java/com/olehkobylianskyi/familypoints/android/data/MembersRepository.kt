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

    suspend fun createGroup(workspaceId: Long, name: String) {
        ApiClient.membersApi.createGroup(
            authorization(), workspaceId, MemberGroupCreateRequest(name)
        )
    }
}
