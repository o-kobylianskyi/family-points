package com.olehkobylianskyi.familypoints.android.data

import com.olehkobylianskyi.familypoints.android.network.ApiClient
import com.olehkobylianskyi.familypoints.android.storage.TokenStore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class TasksRepository(
    private val tokenStore: TokenStore
) {
    private fun authorization(): String {
        val token = tokenStore.getAccessToken()
            ?: throw IllegalStateException("Missing access token")
        return "Bearer $token"
    }

    suspend fun loadMembers(
        workspaceId: Long
    ): List<WorkspaceMemberResponse> =
        ApiClient.taskApi.getWorkspaceMembers(
            authorization(),
            workspaceId
        )

    suspend fun loadMyTab(
        workspaceId: Long,
        memberId: Long,
        date: String
    ): Triple<
        List<TaskInstanceResponse>,
        List<TaskDefinitionResponse>,
        List<TaskDefinitionResponse>
    > = coroutineScope {
        val auth = authorization()

        val tasks = async {
            ApiClient.taskApi.getMemberTasks(
                auth,
                workspaceId,
                memberId,
                date
            )
        }

        val open = async {
            ApiClient.taskApi.getOpenTasks(
                auth,
                workspaceId,
                date,
                memberId
            )
        }

        val definitions = async {
            ApiClient.taskApi.getDefinitions(
                auth,
                workspaceId
            )
        }

        Triple(
            tasks.await(),
            open.await(),
            definitions.await()
        )
    }

    suspend fun performAction(
        workspaceId: Long,
        instanceId: Long,
        action: String
    ): TaskInstanceResponse =
        ApiClient.taskApi.changeStatus(
            authorization(),
            workspaceId,
            instanceId,
            action
        )

    suspend fun claim(
        workspaceId: Long,
        definitionId: Long,
        date: String,
        memberId: Long
    ): TaskInstanceResponse =
        ApiClient.taskApi.claim(
            authorization(),
            workspaceId,
            definitionId,
            date,
            memberId
        )

    suspend fun loadManagement(
        workspaceId: Long,
        view: String
    ): Pair<
        List<TaskDefinitionResponse>,
        Map<Long, List<TaskParticipantResponse>>
    > = coroutineScope {
        val auth = authorization()
        val definitions = ApiClient.taskApi.getDefinitionsByView(
            auth,
            workspaceId,
            view
        )

        val participants = definitions.map { definition ->
            async {
                definition.id to ApiClient.taskApi.getParticipants(
                    auth,
                    workspaceId,
                    definition.id
                )
            }
        }.awaitAll().toMap()

        definitions to participants
    }
}
