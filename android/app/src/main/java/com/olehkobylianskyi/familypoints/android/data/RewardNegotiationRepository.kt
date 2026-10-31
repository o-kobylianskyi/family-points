package com.olehkobylianskyi.familypoints.android.data

import com.olehkobylianskyi.familypoints.android.network.ApiClient
import com.olehkobylianskyi.familypoints.android.storage.TokenStore
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class RewardNegotiationRepository(
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
        date: String,
        instanceId: Long
    ): RewardNegotiationData = coroutineScope {
        val auth = authorization()

        val instance = async {
            val response = ApiClient.taskApi.getDefinitionInstance(
                auth,
                workspaceId,
                definitionId,
                date
            )

            if (!response.isSuccessful || response.body() == null) {
                throw IllegalStateException(
                    "Task instance is not available"
                )
            }

            response.body()!!
        }

        val pointTypes = async {
            ApiClient.createTaskApi.getPointTypes(
                auth,
                workspaceId
            )
        }

        val rewards = async {
            ApiClient.rewardNegotiationApi.getRewards(
                auth,
                workspaceId
            )
        }

        val requests = async {
            ApiClient.rewardNegotiationApi.getTaskRewardRequests(
                auth,
                workspaceId,
                instanceId
            )
        }

        RewardNegotiationData(
            taskInstance = instance.await(),
            pointTypes = pointTypes.await(),
            rewards = rewards.await(),
            requests = requests.await()
        )
    }

    suspend fun create(
        workspaceId: Long,
        instanceId: Long,
        request: TaskRewardRequestCreateRequest
    ): TaskRewardRequestResponse =
        ApiClient.rewardNegotiationApi.createTaskRewardRequest(
            authorization(),
            workspaceId,
            instanceId,
            request
        )

    suspend fun cancel(
        workspaceId: Long,
        requestId: Long
    ): TaskRewardRequestResponse =
        ApiClient.rewardNegotiationApi.cancelTaskRewardRequest(
            authorization(),
            workspaceId,
            requestId
        )
}
