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
        return if (result == RewardPurchaseReceiptPolicy.Decision.ACCEPT)
            State.CONFIRMED_PURCHASE else State.NEEDS_REVIEW
    }

    fun canClearUncertainIntent(state: State): Boolean = false

    fun canAutomaticallyRepeatPurchasePost(state: State): Boolean = false
}
