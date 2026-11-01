package com.olehkobylianskyi.familypoints.android.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/** Synthetic V48 post-response generation gate, independent of device networking. */
class ActivationPostEpochGateTest {
    @Test fun sameFamilyGenerationMayConfirm() {
        assertEquals("CONFIRMED",
            TemplatePendingOperationStore.targetStatus("PREPARED", "CONFIRMED", 14, 14))
    }

    @Test fun postSucceededButFamilyResetHappenedMustQuarantine() {
        assertEquals("HELD",
            TemplatePendingOperationStore.targetStatus("PREPARED", "CONFIRMED", 14, 15))
    }

    @Test fun priorTerminalReceiptCannotBeConvertedToConfirmed() {
        assertThrows(IllegalArgumentException::class.java) {
            TemplatePendingOperationStore.targetStatus("HELD", "CONFIRMED", 14, 14)
        }
    }
}
