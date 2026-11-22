package com.olehkobylianskyi.familypoints.android.ui.screens

import com.olehkobylianskyi.familypoints.android.storage.RewardPurchaseIntentStore

/**
 * Presentation-only projection for an already authenticated selected account/family.
 * Never scans other users' intents. Does not invoke a receipt GET or purchase POST.
 */
internal object RewardPurchaseIntentReviewModel {
    data class Item(
        val operationId: String,
        val memberId: Long,
        val rewardId: Long,
        val state: String
    )

    fun scoped(
        records: List<RewardPurchaseIntentStore.Intent>,
        accountId: Long,
        workspaceId: Long
    ): List<Item> {
        if (accountId <= 0 || workspaceId <= 0) return emptyList()
        return records.asSequence()
            .filter { it.accountId == accountId && it.workspaceId == workspaceId }
            .filter { it.state in setOf("PREPARED", "UNCERTAIN", "HELD", "CONFIRMED") }
            .map { Item(it.operationId.toString(), it.memberId, it.rewardId, it.state) }
            .toList()
    }

    fun actionableForAutomaticPurchase(item: Item): Boolean = false
}
