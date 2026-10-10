package com.olehkobylianskyi.familypoints.android.data

import com.olehkobylianskyi.familypoints.android.network.ApiClient
import com.olehkobylianskyi.familypoints.android.storage.TokenStore
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class TaskDetailsRepository(
    private val tokenStore: TokenStore
) {
    private fun authorization(): String {
        val token = tokenStore.getAccessToken()
            ?: throw IllegalStateException("Missing access token")
        return "Bearer $token"
    }

    suspend fun load(
        workspaceId: Long,
        definitionId: Long,
        date: String
    ): TaskDetailsData = coroutineScope {
        val auth = authorization()

        val definition = async {
            ApiClient.taskApi.getDefinition(
                auth,
                workspaceId,
                definitionId
            )
        }

        val definitions = async {
            ApiClient.taskApi.getDefinitions(
                auth,
                workspaceId
            )
        }

        val members = async {
            ApiClient.taskApi.getWorkspaceMembers(
                auth,
                workspaceId
            )
        }

        val participants = async {
            ApiClient.taskApi.getParticipants(
                auth,
                workspaceId,
                definitionId
            )
        }

        val history = async {
            ApiClient.taskApi.getDefinitionHistory(
                auth,
                workspaceId,
                definitionId
            )
        }

        val subtasks = async {
            ApiClient.taskApi.getSubtasks(
                auth,
                workspaceId,
                definitionId
            )
        }

        val instance = async {
            val response = ApiClient.taskApi.getDefinitionInstance(
                auth,
                workspaceId,
                definitionId,
                date
            )

            if (response.code() == 204) {
                null
            } else if (response.isSuccessful) {
                response.body()
            } else {
                throw retrofit2.HttpException(response)
            }
        }

        TaskDetailsData(
            task = definition.await(),
            visibleDefinitions = definitions.await(),
            instance = instance.await(),
            members = members.await(),
            participants = participants.await(),
            history = history.await(),
            subtasks = subtasks.await()
        )
    }

    suspend fun runInstanceAction(
        workspaceId: Long,
        instanceId: Long,
        action: String
    ) {
        ApiClient.taskApi.changeStatus(
            authorization(),
            workspaceId,
            instanceId,
            action
        )
    }

    suspend fun delegate(
        workspaceId: Long,
        instanceId: Long,
        toMemberId: Long
    ) {
        ApiClient.taskApi.delegate(
            authorization(),
            workspaceId,
            instanceId,
            TaskDelegationRequest(toMemberId)
        )
    }

    suspend fun setDefinitionActive(
        workspaceId: Long,
        definitionId: Long,
        active: Boolean
    ) {
        ApiClient.taskApi.setDefinitionActive(
            authorization(),
            workspaceId,
            definitionId,
            active
        )
    }
}
