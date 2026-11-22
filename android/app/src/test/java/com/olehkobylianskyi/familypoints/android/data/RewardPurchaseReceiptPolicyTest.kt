package com.olehkobylianskyi.familypoints.android.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RewardPurchaseReceiptPolicyTest {
    private val uuid = "8c126e6b-7867-49ed-a40f-a20254cd0918"
    private val intent = RewardPurchaseReceiptPolicy.Intent(uuid, 12, 34, 56, 78, 9)
    private val receipt = RewardPurchaseReceiptPolicy.Receipt(
        uuid, 111, 56, 78, 2, "POINTS", 25, "READY"
    )

    private fun check(
        intent: RewardPurchaseReceiptPolicy.Intent = this.intent,
        receipt: RewardPurchaseReceiptPolicy.Receipt = this.receipt,
        workspace: Long = 12,
        actor: Long = 34,
        epoch: Long = 9
    ) = RewardPurchaseReceiptPolicy.evaluate(intent, receipt, workspace, actor, epoch)

    @Test fun exactReceiptAcceptedIncludingZeroPrice() {
        assertEquals(RewardPurchaseReceiptPolicy.Decision.ACCEPT, check())
        assertEquals(RewardPurchaseReceiptPolicy.Decision.ACCEPT, check(receipt = receipt.copy(priceAmount = 0)))
    }

    @Test fun mismatchedUuidRewardOrMemberNeverConfirms() {
        for (candidate in listOf(
            receipt.copy(operationId = "67e34cfa-09c5-4d4d-853f-f7835f063161"),
            receipt.copy(rewardDefinitionId = 57),
            receipt.copy(memberId = 79),
            receipt.copy(purchaseId = 0)
        )) assertEquals(RewardPurchaseReceiptPolicy.Decision.HOLD_FOR_REVIEW, check(receipt = candidate))
    }

    @Test fun accountWorkspaceEpochDriftQuarantines() {
        assertEquals(RewardPurchaseReceiptPolicy.Decision.HOLD_FOR_REVIEW, check(workspace = 13))
        assertEquals(RewardPurchaseReceiptPolicy.Decision.HOLD_FOR_REVIEW, check(actor = 35))
        assertEquals(RewardPurchaseReceiptPolicy.Decision.HOLD_FOR_REVIEW, check(epoch = 10))
    }

    @Test fun invalidOperationAndFinancialFieldsHeld() {
        assertEquals(RewardPurchaseReceiptPolicy.Decision.HOLD_FOR_REVIEW, check(intent = intent.copy(operationId = "not-uuid")))
        assertEquals(RewardPurchaseReceiptPolicy.Decision.HOLD_FOR_REVIEW, check(receipt = receipt.copy(priceAmount = -1)))
        assertEquals(RewardPurchaseReceiptPolicy.Decision.HOLD_FOR_REVIEW, check(receipt = receipt.copy(pointTypeCode = "")))
    }

    @Test fun readyAndRedeemedConfirmOriginalPurchaseIdentity() {
        assertEquals(RewardPurchaseReceiptPolicy.Decision.ACCEPT, check(receipt = receipt.copy(status = "READY")))
        assertEquals(RewardPurchaseReceiptPolicy.Decision.ACCEPT, check(receipt = receipt.copy(status = "REDEEMED")))
    }

    @Test fun cancelledPendingFailedAndUnknownStatusesNeverConfirm() {
        for (status in listOf("FAILED", "CANCELLED", "PENDING_APPROVAL", "REJECTED", "FULFILLED", "UNKNOWN", "", "ready")) {
            assertEquals(RewardPurchaseReceiptPolicy.Decision.HOLD_FOR_REVIEW,
                check(receipt = receipt.copy(status = status)))
        }
    }

    @Test fun operationIdsMustBeCanonical() {
        assertEquals(RewardPurchaseReceiptPolicy.Decision.HOLD_FOR_REVIEW,
            check(intent = intent.copy(operationId = uuid.uppercase()),
                receipt = receipt.copy(operationId = uuid.uppercase())))
    }

    @Test fun uncertainOutcomeIsNeverAutomaticallyReposted() {
        assertFalse(RewardPurchaseReceiptPolicy.mayAutomaticallyPostAfterUncertainOutcome())
    }
}
