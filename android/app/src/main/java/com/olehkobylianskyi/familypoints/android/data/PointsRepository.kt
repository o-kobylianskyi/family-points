package com.olehkobylianskyi.familypoints.android.data

import com.olehkobylianskyi.familypoints.android.network.ApiClient
import com.olehkobylianskyi.familypoints.android.storage.TokenStore

class PointsRepository(
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
