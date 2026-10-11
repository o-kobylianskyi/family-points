package com.olehkobylianskyi.familypoints.android.data

import com.olehkobylianskyi.familypoints.android.network.ApiClient
import com.olehkobylianskyi.familypoints.android.storage.TokenStore
import com.olehkobylianskyi.familypoints.android.storage.OfflineRewardsStore
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope

class RewardsRepository(
    private val tokenStore: TokenStore,
    private val offlineStore: OfflineRewardsStore
) {
    private fun authorization(): String {
        val token = tokenStore.getAccessToken()
            ?: throw IllegalStateException("Missing access token")
        return "Bearer $token"
    }

    suspend fun loadPointNameForms(workspaceId: Long, pointTypeId: Long): List<PointNameFormDto> =
        ApiClient.settingsApi.pointNameForms(authorization(), workspaceId, pointTypeId)

    fun cached(workspaceId: Long): RewardsPageData? = offlineStore.load(workspaceId)

    suspend fun load(workspaceId: Long): RewardsPageData = supervisorScope {
        val auth = authorization()
        // The catalog is essential. A failure in requests, purchases or tasks
        // must not stop the price list from loading.
        suspend fun <T> optional(block: suspend () -> T, fallback: T): T =
            try { block() } catch (e: retrofit2.HttpException) {
                if (e.code() == 401) throw e
                fallback
            } catch (_: java.io.IOException) { fallback }

        val rewards = async { ApiClient.rewardsApi.getRewards(auth, workspaceId) }
        val categories = async { optional({ ApiClient.rewardsApi.getCategories(auth, workspaceId) }, emptyList()) }
        val requests = async { optional({ ApiClient.rewardsApi.getRequests(auth, workspaceId) }, emptyList()) }
        val purchases = async { optional({ ApiClient.rewardsApi.getPurchases(auth, workspaceId) }, emptyList()) }
        val obligations = async { optional({ ApiClient.rewardsApi.getOpenObligations(auth, workspaceId) }, emptyList()) }
        val pointTypes = async { ApiClient.createTaskApi.getPointTypes(auth, workspaceId) }
        val tasks = async { optional({ ApiClient.taskApi.getDefinitions(auth, workspaceId) }, emptyList()) }

        val result = RewardsPageData(
            rewards = rewards.await(),
            categories = categories.await(),
            requests = requests.await(),
            purchases = purchases.await(),
            obligations = obligations.await(),
            pointTypes = pointTypes.await(),
            tasks = tasks.await().filter { it.active }
        )
        offlineStore.save(workspaceId, result)
        result
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
