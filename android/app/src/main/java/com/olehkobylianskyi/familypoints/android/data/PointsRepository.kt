package com.olehkobylianskyi.familypoints.android.data

import com.olehkobylianskyi.familypoints.android.network.ApiClient
import com.olehkobylianskyi.familypoints.android.storage.TokenStore
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

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

    suspend fun loadMemberPoints(
        workspaceId: Long,
        memberId: Long
    ): Pair<Long, List<PointTransactionResponse>> =
        coroutineScope {
            val auth = authorization()

            val balance = async {
                ApiClient.pointsApi.getBalance(
                    auth,
                    workspaceId,
                    memberId
                )
            }

            val history = async {
                ApiClient.pointsApi.getHistory(
                    auth,
                    workspaceId,
                    memberId
                )
            }

            balance.await().balance to history.await()
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
