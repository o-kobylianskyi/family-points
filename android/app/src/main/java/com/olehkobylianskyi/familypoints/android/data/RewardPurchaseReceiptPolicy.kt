package com.olehkobylianskyi.familypoints.android.data

import java.util.UUID

/**
 * Source-only STEP66 guard. Does not perform a purchase, retry POST, or clear a journal.
 * A receipt is authoritative only for the exact persisted intent, authenticated actor
 * and selected workspace. HTTP failures cannot prove that a debit did not occur.
 */
internal object RewardPurchaseReceiptPolicy {
    data class Intent(
        val operationId: String,
        val workspaceId: Long,
        val actorAccountId: Long,
        val rewardId: Long,
        val memberId: Long,
        val dataEpoch: Long
    )

    data class Receipt(
        val operationId: String,
        val purchaseId: Long,
        val rewardDefinitionId: Long?,
        val memberId: Long,
        val pointTypeId: Long,
        val pointTypeCode: String,
        val priceAmount: Int,
        val status: String
    )

    enum class Decision { ACCEPT, HOLD_FOR_REVIEW }

    fun evaluate(
        intent: Intent,
        receipt: Receipt,
        currentWorkspaceId: Long,
        currentActorAccountId: Long,
        currentDataEpoch: Long
    ): Decision {
        if (runCatching { UUID.fromString(intent.operationId) }.getOrNull()?.toString() != intent.operationId ||
            runCatching { UUID.fromString(receipt.operationId) }.getOrNull()?.toString() != receipt.operationId) {
            return Decision.HOLD_FOR_REVIEW
        }
        if (intent.workspaceId <= 0 || intent.actorAccountId <= 0 ||
            intent.rewardId <= 0 || intent.memberId <= 0 || intent.dataEpoch < 0 ||
            currentWorkspaceId != intent.workspaceId ||
            currentActorAccountId != intent.actorAccountId ||
            currentDataEpoch != intent.dataEpoch) return Decision.HOLD_FOR_REVIEW
        if (receipt.operationId != intent.operationId ||
            receipt.purchaseId <= 0 || receipt.rewardDefinitionId != intent.rewardId ||
            receipt.memberId != intent.memberId || receipt.pointTypeId <= 0 ||
            receipt.pointTypeCode.isBlank() || receipt.priceAmount < 0 ||
            receipt.status !in setOf("READY", "REDEEMED")) return Decision.HOLD_FOR_REVIEW
        return Decision.ACCEPT
    }

    /**
     * Never turn 401/403/404/409 or network loss into a new POST.
     * An explicit receipt GET may be retried with the unchanged operation UUID
     * after authenticated scope and workspace epoch have been reverified.
     */
    fun mayAutomaticallyPostAfterUncertainOutcome(): Boolean = false
}
