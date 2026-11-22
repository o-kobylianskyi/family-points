package com.olehkobylianskyi.familypoints.android.data

import com.olehkobylianskyi.familypoints.android.network.ApiClient
import com.olehkobylianskyi.familypoints.android.storage.RewardPurchaseIntentStore
import com.olehkobylianskyi.familypoints.android.storage.TokenStore
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

/**
 * GET-only candidate, never called by live PointsScreen before PR57 integration.
 * Original journal rows remain unchanged for EVERY outcome, including success,
 * 401/403/404/409, cancellation, stale session and network loss.
 */
class RewardPurchaseReadOnlyRepository(private val tokenStore: TokenStore) {
    companion object {
        const val LIVE_RECEIPT_UI_ENABLED = false
        const val PURCHASE_POST_ENABLED = false
    }

    internal suspend fun check(
        original: RewardPurchaseIntentStore.Intent,
        verifiedAccountId: Long,
        verifiedWorkspaceId: Long
    ): RewardPurchaseRecoveryState.State {
        if (original.accountId != verifiedAccountId ||
            original.workspaceId != verifiedWorkspaceId ||
            verifiedAccountId <= 0 || verifiedWorkspaceId <= 0 ||
            original.state !in setOf("PREPARED", "UNCERTAIN", "HELD")) {
            return RewardPurchaseRecoveryState.State.NEEDS_REVIEW
        }
        val token = tokenStore.getAccessToken()?.takeIf { it.isNotBlank() }
            ?: return RewardPurchaseRecoveryState.State.NEEDS_REVIEW
        val auth = "Bearer $token"
        try {
            val before = ApiClient.workspaceEpochApi.getEpoch(auth, original.workspaceId)
            if (tokenStore.getAccessToken() != token ||
                before.workspaceId != original.workspaceId ||
                before.dataEpoch != original.epoch) return RewardPurchaseRecoveryState.State.NEEDS_REVIEW

            val receipt = ApiClient.rewardPurchaseReceiptApi.getReceipt(
                auth, original.workspaceId, original.operationId.toString()
            )
            if (tokenStore.getAccessToken() != token) return RewardPurchaseRecoveryState.State.NEEDS_REVIEW
            val after = ApiClient.workspaceEpochApi.getEpoch(auth, original.workspaceId)
            if (tokenStore.getAccessToken() != token ||
                after.workspaceId != original.workspaceId ||
                after.dataEpoch != original.epoch) return RewardPurchaseRecoveryState.State.NEEDS_REVIEW

            return RewardPurchaseRecoveryState.afterReceipt(
                RewardPurchaseReceiptPolicy.Intent(
                    original.operationId.toString(), original.workspaceId,
                    original.accountId, original.rewardId, original.memberId, original.epoch
                ),
                receipt.operationId, receipt.purchase,
                verifiedWorkspaceId, verifiedAccountId, after.dataEpoch
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: HttpException) {
            // A 404 or 409 cannot prove no debit; the same UUID remains pending.
            return RewardPurchaseRecoveryState.State.NEEDS_REVIEW
        } catch (_: Exception) {
            return RewardPurchaseRecoveryState.State.NEEDS_REVIEW
        }
    }
}
