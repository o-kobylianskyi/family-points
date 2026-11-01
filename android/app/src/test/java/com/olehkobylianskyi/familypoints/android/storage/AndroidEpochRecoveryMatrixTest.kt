package com.olehkobylianskyi.familypoints.android.storage

import org.junit.Assert.*
import org.junit.Test

/** Pure synthetic policy tests. Live HTTP, SQLite and offline outbox require separate QA. */
class AndroidEpochRecoveryMatrixTest {
    @Test fun unchangedEpochCanMovePreparedToUncertainThenConfirmed() {
        assertEquals("UNCERTAIN", TemplatePendingOperationStore.targetStatus(
            "PREPARED", "UNCERTAIN", 4, 4))
        assertEquals("CONFIRMED", TemplatePendingOperationStore.targetStatus(
            "UNCERTAIN", "CONFIRMED", 4, 4))
    }

    @Test fun changedEpochQuarantinesEveryProposedOutcome() {
        listOf("UNCERTAIN", "CONFIRMED", "HELD").forEach { requested ->
            assertEquals("HELD", TemplatePendingOperationStore.targetStatus(
                "PREPARED", requested, 4, 5))
        }
    }

    @Test fun alreadyHeldOrConfirmedCannotBeReopenedByCatchBlock() {
        listOf("HELD", "CONFIRMED").forEach { current ->
            listOf("UNCERTAIN", "CONFIRMED", "HELD").forEach { requested ->
                assertThrows(IllegalArgumentException::class.java) {
                    TemplatePendingOperationStore.targetStatus(current, requested, 4, 4)
                }
            }
        }
    }

    @Test fun invalidEpochAndUnknownStatusAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            TemplatePendingOperationStore.targetStatus("PREPARED", "CONFIRMED", -1, 1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            TemplatePendingOperationStore.targetStatus("PREPARED", "RETRY", 1, 1)
        }
    }
}
