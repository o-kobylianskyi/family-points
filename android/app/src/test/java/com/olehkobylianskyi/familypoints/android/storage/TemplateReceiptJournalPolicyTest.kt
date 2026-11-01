package com.olehkobylianskyi.familypoints.android.storage

import org.junit.Assert.*
import org.junit.Test

class TemplateReceiptJournalPolicyTest {
    @Test fun sameEpochCanConfirmAndUnknownRemainsUncertain() {
        assertEquals("CONFIRMED", TemplatePendingOperationStore.targetStatus(
            "UNCERTAIN", "CONFIRMED", 5L, 5L))
        assertEquals("UNCERTAIN", TemplatePendingOperationStore.targetStatus(
            "PREPARED", "UNCERTAIN", 5L, 5L))
    }

    @Test fun staleEpochIsAlwaysHeldEvenAfterServerReceipt() {
        assertEquals("HELD", TemplatePendingOperationStore.targetStatus(
            "UNCERTAIN", "CONFIRMED", 5L, 6L))
        assertEquals("HELD", TemplatePendingOperationStore.targetStatus(
            "PREPARED", "UNCERTAIN", 5L, 6L))
    }

    @Test fun terminalStatesCannotBeReopened() {
        for (state in listOf("HELD", "CONFIRMED")) {
            assertThrows(IllegalArgumentException::class.java) {
                TemplatePendingOperationStore.targetStatus(state, "CONFIRMED", 1L, 1L)
            }
        }
    }
}
