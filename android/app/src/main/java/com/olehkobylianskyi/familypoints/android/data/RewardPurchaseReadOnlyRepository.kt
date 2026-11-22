package com.olehkobylianskyi.familypoints.android.data

import com.olehkobylianskyi.familypoints.android.network.ApiClient
import com.olehkobylianskyi.familypoints.android.storage.RewardPurchaseIntentStore
import com.olehkobylianskyi.familypoints.android.storage.TokenStore

/**
 * Inactive read-only PR57 receipt candidate. The shared injectable reconciler
 * covers session rotation, two authenticated dataEpoch fences and loss of response.
 * No receipt response writes to or removes the original SQLite journal row.
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
    ): RewardPurchaseRecoveryState.State =
        RewardReceiptReadOnlyReconciler(
            currentToken = tokenStore::getAccessToken,
            fetchEpoch = { auth, workspace ->
                ApiClient.workspaceEpochApi.getEpoch(auth, workspace)
            },
            fetchReceipt = { auth, workspace, id ->
                ApiClient.rewardPurchaseReceiptApi.getReceipt(auth, workspace, id)
            }
        ).inspect(original, verifiedAccountId, verifiedWorkspaceId)
}
