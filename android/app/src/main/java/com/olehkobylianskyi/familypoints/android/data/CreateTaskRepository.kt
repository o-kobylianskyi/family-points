package com.olehkobylianskyi.familypoints.android.data

import com.olehkobylianskyi.familypoints.android.network.ApiClient
import com.olehkobylianskyi.familypoints.android.storage.TokenStore
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class CreateTaskRepository(
    private val tokenStore: TokenStore
) {
    private fun authorization(): String {
        val token = tokenStore.getAccessToken()
            ?: throw IllegalStateException("Missing access token")
        return "Bearer $token"
    }

    suspend fun loadReferenceData(
        workspaceId: Long
    ): CreateTaskReferenceData = coroutineScope {
        val auth = authorization()

        val members = async {
            ApiClient.taskApi.getWorkspaceMembers(auth, workspaceId)
        }

        val definitions = async {
            ApiClient.taskApi.getDefinitions(auth, workspaceId)
        }

        val pointTypes = async {
            ApiClient.createTaskApi.getPointTypes(auth, workspaceId)
        }

        val groups = async {
            ApiClient.createTaskApi.getMemberGroups(auth, workspaceId)
        }

        CreateTaskReferenceData(
            members = members.await(),
            definitions = definitions.await(),
            pointTypes = pointTypes.await(),
            groups = groups.await()
        )
    }

    suspend fun create(
        workspaceId: Long,
        request: TaskDefinitionCreateRequest
    ): TaskDefinitionResponse =
        ApiClient.createTaskApi.createTask(
            authorization(),
            workspaceId,
            request
        )
}
