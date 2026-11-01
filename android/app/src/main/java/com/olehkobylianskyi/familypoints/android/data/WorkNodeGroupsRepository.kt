package com.olehkobylianskyi.familypoints.android.data

import com.olehkobylianskyi.familypoints.android.network.ApiClient
import com.olehkobylianskyi.familypoints.android.network.WorkNodeGroupDto
import com.olehkobylianskyi.familypoints.android.network.WorkNodeGroupMutation
import com.olehkobylianskyi.familypoints.android.storage.TokenStore
import retrofit2.HttpException

class WorkNodeGroupsRepository(private val tokens: TokenStore) {
    private fun auth() = "Bearer " + (tokens.getAccessToken() ?: error("Missing access token"))

    suspend fun list(workspaceId: Long): List<WorkNodeGroupDto> {
        require(workspaceId > 0)
        return ApiClient.workNodeGroupsApi.list(auth(), workspaceId)
            .also { groups -> check(groups.all { it.id > 0 && it.active }) }
    }

    suspend fun create(workspaceId: Long, request: WorkNodeGroupMutation): WorkNodeGroupDto {
        validate(request)
        return ApiClient.workNodeGroupsApi.create(auth(), workspaceId, request)
            .also { check(it.id > 0 && it.title == request.title) }
    }

    suspend fun edit(workspaceId: Long, groupId: Long, request: WorkNodeGroupMutation): WorkNodeGroupDto {
        require(groupId > 0)
        validate(request)
        return ApiClient.workNodeGroupsApi.edit(auth(), workspaceId, groupId, request)
            .also { check(it.id == groupId) }
    }

    suspend fun deactivate(workspaceId: Long, groupId: Long) {
        require(workspaceId > 0 && groupId > 0)
        val result = ApiClient.workNodeGroupsApi.deactivate(auth(), workspaceId, groupId)
        if (!result.isSuccessful) throw HttpException(result)
    }

    companion object {
        fun validate(request: WorkNodeGroupMutation) {
            require(request.title == request.title.trim() && request.title.isNotBlank())
            require(request.title.length <= 150)
            require(request.description == null || request.description.length <= 2000)
            require(request.parentGroupId == null || request.parentGroupId > 0)
            require(request.sortOrder >= 0)
        }
    }
}
