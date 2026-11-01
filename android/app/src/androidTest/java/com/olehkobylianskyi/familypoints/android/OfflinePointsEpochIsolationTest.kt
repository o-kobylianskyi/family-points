package com.olehkobylianskyi.familypoints.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.olehkobylianskyi.familypoints.android.storage.OfflinePointsQueueStore
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Disposable SQLite regression. Does not call the API or award real points. */
@RunWith(AndroidJUnit4::class)
class OfflinePointsEpochIsolationTest {
    private fun withQueue(block: (OfflinePointsQueueStore) -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase("fp_pending_points.db")
        val store = OfflinePointsQueueStore(context)
        try { block(store) } finally {
            store.close()
            context.deleteDatabase("fp_pending_points.db")
        }
    }

    @Test fun accountAndFamilyQueuesAreIndependent() = withQueue { q ->
        assertTrue(q.observeEpoch(91, 17))
        assertTrue(q.observeEpoch(92, 17))
        q.enqueue(91, 11, 1001, 21, 1, "POINT", "Points", "earn", 3, null)
        q.enqueue(92, 12, 1002, 22, 1, "POINT", "Points", "penalty", 2, null)
        assertEquals(1, q.pending(91, 11, 1001).size)
        assertTrue(q.pending(91, 11, 1002).isEmpty())
        assertTrue(q.pending(92, 11, 1001).isEmpty())
        assertEquals(1, q.pending(92, 12, 1002).size)
    }

    @Test fun changedEpochHoldsExistingQueueAndRetainsUuid() = withQueue { q ->
        assertTrue(q.observeEpoch(91, 17))
        val entry = q.enqueue(91, 11, 1001, 21, 1, "POINT", "Points", "earn", 3, null)
        assertFalse(q.observeEpoch(91, 18))
        assertTrue(q.isHeldForReset(91))
        assertEquals(entry.operationId, q.pending(91, 11, 1001).single().operationId)
        assertThrows(IllegalStateException::class.java) {
            q.enqueue(91, 11, 1001, 21, 1, "POINT", "Points", "earn", 3, null)
        }
    }

    @Test fun foreignAccountCannotConfirmAnotherAccountsUuid() = withQueue { q ->
        assertTrue(q.observeEpoch(91, 17))
        val entry = q.enqueue(91, 11, 1001, 21, 1, "POINT", "Points", "earn", 3, null)
        q.confirm(entry.operationId, 91, 11, 1002)
        assertEquals(entry.operationId, q.pending(91, 11, 1001).single().operationId)
    }

    @Test fun firstEpochObservationWithExistingQueueForcesReview() = withQueue { q ->
        val entry = q.enqueue(91, 11, 1001, 21, 1, "POINT", "Points", "earn", 3, null)
        assertFalse(q.observeEpoch(91, 17))
        assertEquals(entry.operationId, q.pending(91, 11, 1001).single().operationId)
        assertTrue(q.isHeldForReset(91))
    }
    @Test fun resetHoldRemainsDurableAndBlocksNewEnqueueAfterReopen() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase("fp_pending_points.db")
        val first = OfflinePointsQueueStore(context)
        try {
            assertTrue(first.observeEpoch(91, 17))
            first.holdForReset(91)
        } finally { first.close() }
        val reopened = OfflinePointsQueueStore(context)
        try {
            assertTrue(reopened.isHeldForReset(91))
            assertThrows(IllegalStateException::class.java) {
                reopened.enqueue(91, 11, 1001, 21, 1, "POINT", "Points", "earn", 3, null)
            }
            assertTrue(reopened.pending(91, 11, 1001).isEmpty())
            // A hold in family A must not block safe operations in family B.
            assertTrue(reopened.observeEpoch(92, 17))
            reopened.enqueue(92, 12, 1002, 22, 1, "POINT", "Points", "earn", 3, null)
            assertEquals(1, reopened.pending(92, 12, 1002).size)
        } finally {
            reopened.close()
            context.deleteDatabase("fp_pending_points.db")
        }
    }

    @Test fun epochQuarantineDuringHttpReplayPreventsAcknowledgement() = withQueue { q ->
        assertTrue(q.observeEpoch(91, 17))
        val pending = q.enqueue(91, 11, 1001, 21, 1, "POINT", "Points", "earn", 3, null)
        // Simulates verified server epoch change after POST but before commit.
        assertFalse(q.observeEpoch(91, 18))
        assertFalse(q.confirm(pending.operationId, 91, 11, 1001))
        assertEquals(pending.operationId, q.pending(91, 11, 1001).single().operationId)
        assertTrue(q.isHeldForReset(91))
    }

    @Test fun acknowledgementIsScopedToOriginalOwnerAndUnrelatedFamilyIsUnaffected() = withQueue { q ->
        assertTrue(q.observeEpoch(91, 17))
        assertTrue(q.observeEpoch(92, 17))
        val first = q.enqueue(91, 11, 1001, 21, 1, "POINT", "Points", "earn", 3, null)
        val second = q.enqueue(92, 12, 1002, 22, 1, "POINT", "Points", "earn", 4, null)
        assertFalse(q.confirm(first.operationId, 91, 11, 1002))
        assertFalse(q.confirm(first.operationId, 92, 12, 1002))
        q.holdForReset(91)
        assertFalse(q.confirm(first.operationId, 91, 11, 1001))
        assertTrue(q.confirm(second.operationId, 92, 12, 1002))
        assertEquals(first.operationId, q.pending(91, 11, 1001).single().operationId)
        assertTrue(q.pending(92, 12, 1002).isEmpty())
    }

}
