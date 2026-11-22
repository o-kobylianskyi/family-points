package com.olehkobylianskyi.familypoints.android.data

import com.olehkobylianskyi.familypoints.android.network.RewardPurchaseReceiptResponse
import com.olehkobylianskyi.familypoints.android.network.WorkspaceEpochResponse
import com.olehkobylianskyi.familypoints.android.storage.RewardPurchaseIntentStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.IOException
import java.util.UUID

class RewardReceiptReadOnlyReconcilerTest {
    private val uuid = UUID.fromString("f4271bb3-bf2f-4b6b-b671-1e66b78fa09f")
    private val original = RewardPurchaseIntentStore.Intent(
        uuid, 21, 22, 23, 24, 25, "SELF", "UNCERTAIN", 6, "POINTS", 9
    )
    private val purchase = RewardPurchaseSummary(
        45, 24, 23, "Member", "Reward", 6, "POINTS", 9,
        null, "READY", null, null
    )
    private val envelope = RewardPurchaseReceiptResponse(
        uuid.toString(), purchase, false
    )

    private fun check(
        intent: RewardPurchaseIntentStore.Intent = original,
        currentAccount: Long = 21,
        currentWorkspace: Long = 22,
        tokenSupplier: () -> String? = { "jwt" },
        epoch: suspend (String, Long) -> WorkspaceEpochResponse = { _, _ ->
            WorkspaceEpochResponse(22, 25)
        },
        receipt: suspend (String, Long, String) -> RewardPurchaseReceiptResponse = { _, _, _ ->
            envelope
        }
    ): RewardPurchaseRecoveryState.State = runBlocking {
        RewardReceiptReadOnlyReconciler(tokenSupplier, epoch, receipt)
            .inspect(intent, currentAccount, currentWorkspace)
    }

    @Test fun exactUuidAndTwoEpochReadsGiveReadOnlyConfirmation() {
        var reads = 0
        var receiptReads = 0
        assertEquals(RewardPurchaseRecoveryState.State.CONFIRMED_PURCHASE,
            check(epoch = { _, _ ->
                reads++
                WorkspaceEpochResponse(22, 25)
            }, receipt = { auth, workspace, originalUuid ->
                assertEquals("Bearer jwt", auth)
                assertEquals(22L, workspace)
                assertEquals(uuid.toString(), originalUuid)
                receiptReads++
                envelope
            }))
        assertEquals(2, reads)
        assertEquals(1, receiptReads)
    }

    @Test fun lostResponseOrHttpErrorNeverConfirmsOrRetries() {
        for (code in listOf(401, 403, 404, 409, 500)) {
            var attempts = 0
            assertEquals(RewardPurchaseRecoveryState.State.NEEDS_REVIEW,
                check(receipt = { _, _, _ ->
                    attempts++
                    throw IOException("HTTP $code")
                }))
            assertEquals(1, attempts)
        }
        assertEquals(RewardPurchaseRecoveryState.State.NEEDS_REVIEW,
            check(receipt = { _, _, _ -> throw IOException("socket closed after write") }))
    }

    @Test fun unknownOrChangedSessionNeverFetchesReceipt() {
        var receiptReads = 0
        assertEquals(RewardPurchaseRecoveryState.State.NEEDS_REVIEW,
            check(tokenSupplier = { null }, receipt = { _, _, _ ->
                receiptReads++
                envelope
            }))
        assertEquals(0, receiptReads)
        var tokenReads = 0
        assertEquals(RewardPurchaseRecoveryState.State.NEEDS_REVIEW,
            check(tokenSupplier = {
                tokenReads++
                if (tokenReads < 3) "jwt" else "rotated"
            }, receipt = { _, _, _ ->
                receiptReads++
                envelope
            }))
        assertEquals(0, receiptReads)
    }

    @Test fun accountWorkspaceAndEpochDriftNeverPerformUnsafeRead() {
        var receiptReads = 0
        val fetch: suspend (String, Long, String) -> RewardPurchaseReceiptResponse =
            { _, _, _ -> receiptReads++; envelope }
        assertEquals(RewardPurchaseRecoveryState.State.NEEDS_REVIEW,
            check(currentAccount = 99, receipt = fetch))
        assertEquals(RewardPurchaseRecoveryState.State.NEEDS_REVIEW,
            check(currentWorkspace = 99, receipt = fetch))
        assertEquals(RewardPurchaseRecoveryState.State.NEEDS_REVIEW,
            check(epoch = { _, _ -> WorkspaceEpochResponse(22, 26) }, receipt = fetch))
        assertEquals(0, receiptReads)
        var reads = 0
        assertEquals(RewardPurchaseRecoveryState.State.NEEDS_REVIEW,
            check(epoch = { _, _ ->
                reads++
                WorkspaceEpochResponse(22, if (reads == 1) 25 else 26)
            }))
        assertEquals(2, reads)
    }

    @Test fun statusTransitionsPreserveHistoricalMeaning() {
        assertEquals(RewardPurchaseRecoveryState.State.CONFIRMED_REDEEMED,
            check(receipt = { _, _, _ -> envelope.copy(purchase = purchase.copy(status = "REDEEMED")) }))
        assertEquals(RewardPurchaseRecoveryState.State.CANCELLED_HISTORY_HOLD,
            check(receipt = { _, _, _ -> envelope.copy(purchase = purchase.copy(status = "CANCELLED")) }))
        for (status in listOf("PENDING_APPROVAL", "FAILED", "UNKNOWN", "")) {
            assertEquals(RewardPurchaseRecoveryState.State.NEEDS_REVIEW,
                check(receipt = { _, _, _ -> envelope.copy(purchase = purchase.copy(status = status)) }))
        }
    }

    @Test fun foreignReceiptAndConfirmedIntentFailClosed() {
        assertEquals(RewardPurchaseRecoveryState.State.NEEDS_REVIEW,
            check(receipt = { _, _, _ -> envelope.copy(operationId = UUID.randomUUID().toString()) }))
        assertEquals(RewardPurchaseRecoveryState.State.NEEDS_REVIEW,
            check(receipt = { _, _, _ -> envelope.copy(purchase = purchase.copy(memberId = 999)) }))
        assertEquals(RewardPurchaseRecoveryState.State.NEEDS_REVIEW,
            check(intent = original.copy(state = "CONFIRMED")))
        assertFalse(RewardPurchaseReadOnlyRepository.PURCHASE_POST_ENABLED)
    }
}
