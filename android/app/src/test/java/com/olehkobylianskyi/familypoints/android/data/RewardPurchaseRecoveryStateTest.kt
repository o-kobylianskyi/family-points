package com.olehkobylianskyi.familypoints.android.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RewardPurchaseRecoveryStateTest {
    private val operationId = "8c126e6b-7867-49ed-a40f-a20254cd0918"
    private val intent = RewardPurchaseReceiptPolicy.Intent(operationId, 12, 34, 56, 78, 9, 2, "POINTS", 25)
    private val purchase = RewardPurchaseSummary(
        id = 111,
        rewardDefinitionId = 56,
        memberId = 78,
        memberName = "Test member",
        rewardTitle = "Test reward",
        pointTypeId = 2,
        pointTypeCode = "POINTS",
        priceAmount = 25,
        durationMinutes = null,
        status = "READY",
        purchasedAt = null,
        resolvedAt = null
    )

    private fun result(
        uuid: String = operationId,
        purchase: RewardPurchaseSummary = this.purchase,
        actor: Long = 34,
        workspace: Long = 12,
        epoch: Long = 9
    ) = RewardPurchaseRecoveryState.afterReceipt(intent, uuid, purchase, workspace, actor, epoch)

    @Test fun matchingReadOnlyReceiptConfirmsPurchaseNotFulfillment() {
        assertEquals(RewardPurchaseRecoveryState.State.CONFIRMED_PURCHASE, result())
    }

    @Test fun redeemedAndCancelledHaveDifferentHistoricalOutcomes() {
        assertEquals(RewardPurchaseRecoveryState.State.CONFIRMED_REDEEMED,
            result(purchase = purchase.copy(status = "REDEEMED")))
        assertEquals(RewardPurchaseRecoveryState.State.CANCELLED_HISTORY_HOLD,
            result(purchase = purchase.copy(status = "CANCELLED")))
        assertEquals(RewardPurchaseRecoveryState.State.NEEDS_REVIEW,
            result(purchase = purchase.copy(status = "PENDING_APPROVAL")))
        assertEquals(RewardPurchaseRecoveryState.State.NEEDS_REVIEW,
            result(purchase = purchase.copy(status = "FAILED")))
    }

    @Test fun forgedCancellationNeverBecomesVerifiedHistoricalPurchase() {
        assertEquals(RewardPurchaseRecoveryState.State.NEEDS_REVIEW,
            result(purchase = purchase.copy(status = "CANCELLED", rewardDefinitionId = 888)))
        assertEquals(RewardPurchaseRecoveryState.State.NEEDS_REVIEW,
            result(purchase = purchase.copy(status = "CANCELLED"), actor = 999))
        assertEquals(RewardPurchaseRecoveryState.State.NEEDS_REVIEW,
            result(purchase = purchase.copy(status = "CANCELLED"), epoch = 10))
        assertEquals(RewardPurchaseRecoveryState.State.NEEDS_REVIEW,
            result(uuid = "incorrect", purchase = purchase.copy(status = "CANCELLED")))
    }

    @Test fun mismatchedOriginalCurrencyOrPriceIsNeverConfirmed() {
        assertEquals(RewardPurchaseRecoveryState.State.NEEDS_REVIEW,
            result(purchase = purchase.copy(priceAmount = 26)))
        assertEquals(RewardPurchaseRecoveryState.State.NEEDS_REVIEW,
            result(purchase = purchase.copy(pointTypeId = 3)))
        assertEquals(RewardPurchaseRecoveryState.State.NEEDS_REVIEW,
            result(purchase = purchase.copy(pointTypeCode = "OTHER")))
    }

    @Test fun receiptMismatchAndScopeDriftAreHeld() {
        assertEquals(RewardPurchaseRecoveryState.State.NEEDS_REVIEW, result(uuid = "unrelated"))
        assertEquals(RewardPurchaseRecoveryState.State.NEEDS_REVIEW,
            result(purchase = purchase.copy(priceAmount = -1)))
        assertEquals(RewardPurchaseRecoveryState.State.NEEDS_REVIEW, result(actor = 99))
        assertEquals(RewardPurchaseRecoveryState.State.NEEDS_REVIEW, result(workspace = 99))
        assertEquals(RewardPurchaseRecoveryState.State.NEEDS_REVIEW, result(epoch = 10))
    }

    @Test fun httpStatusCannotIndependentlyConfirmFinancialOutcome() {
        assertEquals(RewardPurchaseRecoveryState.State.CHECKING,
            RewardPurchaseRecoveryState.afterHttpStatus(200))
        for (code in listOf(401, 403, 404, 409, 500, 503)) {
            assertEquals(RewardPurchaseRecoveryState.State.NEEDS_REVIEW,
                RewardPurchaseRecoveryState.afterHttpStatus(code))
        }
    }

    @Test fun noStateMayClearOriginalIntentOrAutomaticallyPostAgain() {
        for (state in RewardPurchaseRecoveryState.State.entries) {
            assertFalse(RewardPurchaseRecoveryState.canClearUncertainIntent(state))
            assertFalse(RewardPurchaseRecoveryState.canAutomaticallyRepeatPurchasePost(state))
        }
    }
}
