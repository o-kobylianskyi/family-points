package com.olehkobylianskyi.familypoints.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.olehkobylianskyi.familypoints.android.storage.TemplatePendingOperationStore
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** Disposable local DB tests; no backend call, device reset or reward mutation. */
@RunWith(AndroidJUnit4::class)
class TemplatePendingOperationSqliteTest {
    private fun withFreshStore(block: (TemplatePendingOperationStore) -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase("fp_template_receipts.db")
        val store = TemplatePendingOperationStore(context)
        try { block(store) } finally {
            store.close()
            context.deleteDatabase("fp_template_receipts.db")
        }
    }

    private fun draft(account: Long, family: Long, id: UUID = UUID.randomUUID()) =
        TemplatePendingOperationStore.Pending(
            id, account, family, 17L, "TASK", 42L, "gcm1:synthetic", "PREPARED"
        )

    @Test fun sameFamilyCanHaveOnlyOnePendingActivationPerTemplate() = withFreshStore { store ->
        val one = draft(11, 91)
        store.prepare(one)
        assertEquals(listOf(one.operationId), store.pending(11, 91).map { it.operationId })
        assertThrows(IllegalStateException::class.java) {
            store.prepare(draft(12, 91))
        }
    }

    @Test fun familyAndAccountScopeNeverLeakPendingRows() = withFreshStore { store ->
        store.prepare(draft(11, 91))
        store.prepare(draft(12, 92))
        assertEquals(1, store.pending(11, 91).size)
        assertTrue(store.pending(12, 91).isEmpty())
        assertTrue(store.pending(11, 92).isEmpty())
        assertEquals(1, store.pending(12, 92).size)
    }

    @Test fun epochDriftQuarantinesAndPreventsReopeningTerminalState() = withFreshStore { store ->
        val entry = draft(11, 91)
        store.prepare(entry)
        store.transition(entry, "CONFIRMED", 18L)
        val persisted = store.pending(11, 91).single()
        assertEquals("HELD", persisted.status)
        assertEquals(entry.operationId, persisted.operationId)
        assertEquals(entry.payload, persisted.payload)
        assertThrows(IllegalArgumentException::class.java) {
            store.transition(persisted, "CONFIRMED", 18L)
        }
    }

    @Test fun uncertainReceiptSurvivesStoreCloseAndReopen() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase("fp_template_receipts.db")
        val entry = draft(11, 91)
        val first = TemplatePendingOperationStore(context)
        try {
            first.prepare(entry)
            first.transition(entry, "UNCERTAIN", 17L)
        } finally { first.close() }
        val reopened = TemplatePendingOperationStore(context)
        try {
            assertEquals(entry.operationId, reopened.pending(11, 91).single().operationId)
            assertEquals("UNCERTAIN", reopened.pending(11, 91).single().status)
        } finally {
            reopened.close()
            context.deleteDatabase("fp_template_receipts.db")
        }
    }
    @Test fun confirmedReceiptIsNotPendingAndCannotBeReopened() = withFreshStore { store ->
        val entry = draft(11, 91)
        store.prepare(entry)
        store.transition(entry, "CONFIRMED", 17L)
        assertTrue(store.pending(11, 91).isEmpty())
        assertThrows(IllegalStateException::class.java) {
            store.transition(entry, "UNCERTAIN", 17L)
        }
    }

    @Test fun aForeignAccountCannotMutateAnotherAccountsOperation() = withFreshStore { store ->
        val entry = draft(11, 91)
        store.prepare(entry)
        val forged = entry.copy(accountId = 12L)
        assertThrows(IllegalStateException::class.java) {
            store.transition(forged, "CONFIRMED", 17L)
        }
        assertEquals("PREPARED", store.pending(11, 91).single().status)
        assertTrue(store.pending(12, 91).isEmpty())
    }

    @Test fun sameUuidCannotOverwriteAnotherFamilyOrOriginalPayload() = withFreshStore { store ->
        val entry = draft(11, 91)
        store.prepare(entry)
        assertThrows(android.database.SQLException::class.java) {
            store.prepare(draft(12, 92, entry.operationId))
        }
        assertEquals(entry.payload, store.pending(11, 91).single().payload)
        assertTrue(store.pending(12, 92).isEmpty())
        assertThrows(IllegalStateException::class.java) {
            store.transition(entry.copy(payload = "gcm1:modified"), "CONFIRMED", 17L)
        }
    }


}
