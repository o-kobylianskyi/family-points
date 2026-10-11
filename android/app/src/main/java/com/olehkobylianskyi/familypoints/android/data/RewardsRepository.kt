package com.olehkobylianskyi.familypoints.android.data

import com.olehkobylianskyi.familypoints.android.network.ApiClient
import com.olehkobylianskyi.familypoints.android.storage.TokenStore
import com.olehkobylianskyi.familypoints.android.storage.OfflineRewardsStore
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import java.io.IOException
import retrofit2.HttpException

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

    fun cached(workspaceId: Long, memberId: Long): RewardsPageData? =
        offlineStore.load(workspaceId, memberId)

    /**
     * Fetch and persist only the essential catalog. Optional endpoints must not
     * delay saving reward names, prices and purchase requirements.
     */
    suspend fun refreshCatalog(workspaceId: Long, memberId: Long): RewardsPageData {
        val rewards = ApiClient.rewardsApi.getRewards(authorization(), workspaceId)
        val previous = cached(workspaceId, memberId)
        val catalog = RewardsPageData(
            rewards = rewards,
            categories = previous?.categories.orEmpty(),
            requests = emptyList(),
            purchases = emptyList(),
            obligations = emptyList(),
            pointTypes = previous?.pointTypes.orEmpty(),
            tasks = emptyList()
        )
        offlineStore.save(workspaceId, memberId, catalog)
        return catalog
    }

    /** Optional data is loaded separately, with an independent timeout per call. */
    suspend fun refreshSupplementary(
        workspaceId: Long,
        memberId: Long,
        catalog: RewardsPageData
    ): RewardsPageData = supervisorScope {
        val auth = authorization()
        suspend fun <T> optional(fallback: T, action: suspend () -> T): T {
            return try {
                withTimeout(8_000L) { action() }
            } catch (error: TimeoutCancellationException) {
                fallback
            } catch (error: HttpException) {
                if (error.code() == 401) throw error
                fallback
            } catch (_: IOException) {
                fallback
            }
        }

        val categories = async {
            optional(catalog.categories) { ApiClient.rewardsApi.getCategories(auth, workspaceId) }
        }
        val pointTypes = async {
            optional(catalog.pointTypes) { ApiClient.createTaskApi.getPointTypes(auth, workspaceId) }
        }
        val requests = async {
            optional(emptyList<RewardRequestSummary>()) {
                ApiClient.rewardsApi.getRequests(auth, workspaceId)
            }
        }
        val purchases = async {
            optional(emptyList<RewardPurchaseSummary>()) {
                ApiClient.rewardsApi.getPurchases(auth, workspaceId)
            }
        }
        val obligations = async {
            optional(emptyList<RewardObligationSummary>()) {
                ApiClient.rewardsApi.getOpenObligations(auth, workspaceId)
            }
        }
        val tasks = async {
            optional(emptyList<TaskDefinitionResponse>()) {
                ApiClient.taskApi.getDefinitions(auth, workspaceId)
            }
        }

        val result = catalog.copy(
            categories = categories.await(),
            pointTypes = pointTypes.await(),
            requests = requests.await(),
            purchases = purchases.await(),
            obligations = obligations.await(),
            tasks = tasks.await().filter { it.active }
        )
        offlineStore.save(workspaceId, memberId, result)
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
