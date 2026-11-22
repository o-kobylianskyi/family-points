package com.olehkobylianskyi.familypoints.android.data

/**
 * Display-only state machine. A missing/denied receipt is not evidence of no debit.
 * The persisted original UUID must remain intact until authoritative reconciliation.
 */
internal object RewardPurchaseRecoveryState {
    enum class State {
        READY_TO_CHECK,
        CHECKING,
        CONFIRMED_PURCHASE,
        CONFIRMED_REDEEMED,
        CANCELLED_HISTORY_HOLD,
        NEEDS_REVIEW
    }

    fun afterHttpStatus(code: Int): State = when (code) {
        // Only the validated body from HTTP 200 can be treated as confirmed.
        200 -> State.CHECKING
        else -> State.NEEDS_REVIEW
    }

    fun afterReceipt(
        intent: RewardPurchaseReceiptPolicy.Intent,
        responseOperationId: String,
        purchase: RewardPurchaseSummary,
        currentWorkspaceId: Long,
        currentAccountId: Long,
        currentEpoch: Long
    ): State {
        val result = RewardPurchaseReceiptPolicy.evaluate(
            intent,
            RewardPurchaseReceiptPolicy.Receipt(
                responseOperationId,
                purchase.id,
                purchase.rewardDefinitionId,
                purchase.memberId,
                purchase.pointTypeId,
                purchase.pointTypeCode,
                purchase.priceAmount,
                purchase.status
            ),
            currentWorkspaceId,
            currentAccountId,
            currentEpoch
        )
        if (result == RewardPurchaseReceiptPolicy.Decision.ACCEPT) {
            return if (purchase.status == "REDEEMED") State.CONFIRMED_REDEEMED
            else State.CONFIRMED_PURCHASE
        }
        // CANCELLED does not undo the historical purchase or prove a refund.
        // Distinguish it ONLY when the full receipt identity/price/type/scope
        // validates; a forged/foreign cancellation remains NEEDS_REVIEW.
        if (purchase.status == "CANCELLED") {
            val matched = RewardPurchaseReceiptPolicy.evaluate(
                intent,
                RewardPurchaseReceiptPolicy.Receipt(
                    responseOperationId, purchase.id, purchase.rewardDefinitionId,
                    purchase.memberId, purchase.pointTypeId, purchase.pointTypeCode,
                    purchase.priceAmount, "READY"
                ),
                currentWorkspaceId, currentAccountId, currentEpoch
            )
            if (matched == RewardPurchaseReceiptPolicy.Decision.ACCEPT)
                return State.CANCELLED_HISTORY_HOLD
        }
        return State.NEEDS_REVIEW
    }

    fun canClearUncertainIntent(state: State): Boolean = false

    fun canAutomaticallyRepeatPurchasePost(state: State): Boolean = false
}
