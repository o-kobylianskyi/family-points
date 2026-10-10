package com.olehkobylianskyi.familypoints.android.data

import com.olehkobylianskyi.familypoints.android.network.ApiClient
import com.olehkobylianskyi.familypoints.android.storage.TokenStore
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class RewardsRepository(
    private val tokenStore: TokenStore
) {
    private fun authorization(): String {
        val token = tokenStore.getAccessToken()
            ?: throw IllegalStateException("Missing access token")
        return "Bearer $token"
    }

    suspend fun loadPointNameForms(workspaceId: Long, pointTypeId: Long): List<PointNameFormDto> =
        ApiClient.settingsApi.pointNameForms(authorization(), workspaceId, pointTypeId)

    suspend fun load(workspaceId: Long): RewardsPageData =
        coroutineScope {
            val auth = authorization()

            val rewards = async {
                ApiClient.rewardsApi.getRewards(auth, workspaceId)
            }
            val categories = async {
                ApiClient.rewardsApi.getCategories(auth, workspaceId)
            }
            val requests = async {
                ApiClient.rewardsApi.getRequests(auth, workspaceId)
            }
            val purchases = async {
                ApiClient.rewardsApi.getPurchases(auth, workspaceId)
            }
            val obligations = async {
                ApiClient.rewardsApi.getOpenObligations(auth, workspaceId)
            }
            val pointTypes = async {
                ApiClient.createTaskApi.getPointTypes(auth, workspaceId)
            }
            val tasks = async {
                ApiClient.taskApi.getDefinitions(auth, workspaceId)
            }

            RewardsPageData(
                rewards = rewards.await(),
                categories = categories.await(),
                requests = requests.await(),
                purchases = purchases.await(),
                obligations = obligations.await(),
                pointTypes = pointTypes.await(),
                tasks = tasks.await().filter { it.active }
            )
        }

    suspend fun createCategory(
        workspaceId: Long,
        name: String,
        sortOrder: Int
    ) = ApiClient.rewardsApi.createCategory(
        authorization(),
        workspaceId,
        RewardCategoryCreateRequest(name, sortOrder)
    )

    suspend fun createReward(
        workspaceId: Long,
        request: RewardDefinitionCreateRequest
    ) = ApiClient.rewardsApi.createReward(
        authorization(),
        workspaceId,
        request
    )

    suspend fun requestCatalogReward(
        workspaceId: Long,
        rewardId: Long
    ) = ApiClient.rewardsApi.createRequest(
        authorization(),
        workspaceId,
        RewardRequestCreateRequest(
            rewardDefinitionId = rewardId,
            title = null,
            description = null
        )
    )

    suspend fun createCustomRequest(
        workspaceId: Long,
        title: String,
        description: String?
    ) = ApiClient.rewardsApi.createRequest(
        authorization(),
        workspaceId,
        RewardRequestCreateRequest(
            rewardDefinitionId = null,
            title = title,
            description = description
        )
    )

    suspend fun purchaseReward(
        workspaceId: Long,
        rewardId: Long
    ) = ApiClient.rewardsApi.purchaseReward(
        authorization(),
        workspaceId,
        rewardId
    )

    suspend fun approveRequest(
        workspaceId: Long,
        requestId: Long,
        request: RewardRequestReviewRequest
    ) = ApiClient.rewardsApi.approveRequest(
        authorization(),
        workspaceId,
        requestId,
        request
    )

    suspend fun rejectRequest(
        workspaceId: Long,
        requestId: Long
    ) = ApiClient.rewardsApi.rejectRequest(
        authorization(),
        workspaceId,
        requestId
    )

    suspend fun refreshRequest(
        workspaceId: Long,
        requestId: Long
    ) = ApiClient.rewardsApi.refreshRequest(
        authorization(),
        workspaceId,
        requestId
    )

    suspend fun purchaseRequest(
        workspaceId: Long,
        requestId: Long
    ) = ApiClient.rewardsApi.purchaseRequest(
        authorization(),
        workspaceId,
        requestId
    )
}
