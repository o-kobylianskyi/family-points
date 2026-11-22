package com.olehkobylianskyi.familypoints.android.data

import com.olehkobylianskyi.familypoints.android.network.RewardPurchaseReceiptResponse
import com.olehkobylianskyi.familypoints.android.network.WorkspaceEpochResponse
import com.olehkobylianskyi.familypoints.android.storage.RewardPurchaseIntentStore
import kotlinx.coroutines.CancellationException

/**
 * Read-only, injectable lost-response reconciliation. All operations are GET.
 * Does not mutate, acknowledge, clear, replay, or retry the original intent.
 * Supplied endpoints must be authenticated and scoped to current session.
 */
internal class RewardReceiptReadOnlyReconciler(
    private val currentToken: () -> String?,
    private val fetchEpoch: suspend (auth: String, workspaceId: Long) -> WorkspaceEpochResponse,
    private val fetchReceipt: suspend (
        auth: String, workspaceId: Long, operationId: String
    ) -> RewardPurchaseReceiptResponse
) {
    suspend fun inspect(
        original: RewardPurchaseIntentStore.Intent,
        actorAccountId: Long,
        workspaceId: Long
    ): RewardPurchaseRecoveryState.State {
        val unresolved = original.state in setOf("PREPARED", "UNCERTAIN", "HELD")
        if (!unresolved || actorAccountId <= 0 || workspaceId <= 0 ||
            original.accountId != actorAccountId || original.workspaceId != workspaceId ||
            original.memberId <= 0 || original.rewardId <= 0 || original.epoch < 0
        ) return RewardPurchaseRecoveryState.State.NEEDS_REVIEW

        val token = currentToken()?.takeIf { it.isNotBlank() }
            ?: return RewardPurchaseRecoveryState.State.NEEDS_REVIEW
        val auth = "Bearer $token"

        try {
            fun sameSession(): Boolean = currentToken() == token

            if (!sameSession()) return RewardPurchaseRecoveryState.State.NEEDS_REVIEW
            val before = fetchEpoch(auth, workspaceId)
            if (!sameSession() || before.workspaceId != workspaceId ||
                before.dataEpoch != original.epoch) return RewardPurchaseRecoveryState.State.NEEDS_REVIEW

            val receipt = fetchReceipt(auth, workspaceId, original.operationId.toString())
            if (!sameSession()) return RewardPurchaseRecoveryState.State.NEEDS_REVIEW

            val after = fetchEpoch(auth, workspaceId)
            if (!sameSession() || after.workspaceId != workspaceId ||
                after.dataEpoch != before.dataEpoch) return RewardPurchaseRecoveryState.State.NEEDS_REVIEW

            return RewardPurchaseRecoveryState.afterReceipt(
                RewardPurchaseReceiptPolicy.Intent(
                    original.operationId.toString(), original.workspaceId, original.accountId,
                    original.rewardId, original.memberId, original.epoch
                ),
                receipt.operationId, receipt.purchase, workspaceId, actorAccountId,
                after.dataEpoch
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // 401/403/404/409, timeout, malformed transport, and network failure
            // never indicate that no debit happened.
            return RewardPurchaseRecoveryState.State.NEEDS_REVIEW
        }
    }
}
