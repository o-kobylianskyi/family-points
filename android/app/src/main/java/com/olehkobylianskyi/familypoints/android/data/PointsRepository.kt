package com.olehkobylianskyi.familypoints.android.data

import com.olehkobylianskyi.familypoints.android.network.ApiClient
import com.olehkobylianskyi.familypoints.android.storage.TokenStore
import com.olehkobylianskyi.familypoints.android.storage.OfflineBalanceStore
import java.io.IOException

class PointsRepository(
    private val tokenStore: TokenStore,
    private val offlineStore: OfflineBalanceStore
) {
    private fun authorization(): String {
        val token = tokenStore.getAccessToken()
            ?: throw IllegalStateException("Missing access token")
        return "Bearer $token"
    }

    data class CachedPoints(val balance: Long, val history: List<PointTransactionResponse>, val offline: Boolean)

    suspend fun cachedBalance(workspaceId: Long, memberId: Long): Long? =
        offlineStore.load(workspaceId, memberId)?.balance

    suspend fun loadPointsWithFallback(workspaceId: Long, memberId: Long): CachedPoints {
        val auth = authorization()
        val balance = try {
            ApiClient.pointsApi.getBalance(auth, workspaceId, memberId).balance.also { value ->
                offlineStore.save(workspaceId, memberId, value)
            }
        } catch (error: IOException) {
            val cached = offlineStore.load(workspaceId, memberId) ?: throw error
            return CachedPoints(cached.balance, emptyList(), true)
        }
        val history = try {
            ApiClient.pointsApi.getHistory(auth, workspaceId, memberId)
        } catch (error: Exception) {
            if (error is retrofit2.HttpException && error.code() == 401) throw error
            emptyList()
        }
        return CachedPoints(balance, history, false)
    }

    suspend fun loadHistory(workspaceId: Long, memberId: Long): List<PointTransactionResponse> =
        ApiClient.pointsApi.getHistory(authorization(), workspaceId, memberId)

    suspend fun loadMembers(
        workspaceId: Long
    ): List<WorkspaceMemberResponse> =
        ApiClient.taskApi.getWorkspaceMembers(
            authorization(),
            workspaceId
        )

    // A failed history request must not make the current balance disappear.
    suspend fun loadMemberPoints(
        workspaceId: Long,
        memberId: Long
    ): Pair<Long, List<PointTransactionResponse>> {
        val auth = authorization()
        val balance = ApiClient.pointsApi.getBalance(auth, workspaceId, memberId).balance
        val history = try {
            ApiClient.pointsApi.getHistory(auth, workspaceId, memberId)
        } catch (error: Exception) {
            // A bad authorization must never be hidden as an empty history.
            if (error is retrofit2.HttpException && error.code() == 401) throw error
            emptyList()
        }
        return balance to history
    }

    suspend fun operate(
        workspaceId: Long,
        memberId: Long,
        operation: String,
        amount: Int,
        description: String?
    ) {
        ApiClient.pointsApi.operate(
            authorization(),
            workspaceId,
            memberId,
            operation,
            PointOperationRequest(
                amount = amount,
                description = description
            )
        )
    }
}
