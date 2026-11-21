package com.olehkobylianskyi.familypoints.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.olehkobylianskyi.familypoints.android.storage.RewardPurchaseIntentStore
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** Disposable on-device SQLite only. No network, monetary writes or reset. */
@RunWith(AndroidJUnit4::class)
class RewardPurchaseIntentSqliteTest {
    private val database = "fp_reward_purchase_intents.db"

    private fun fresh(block: (RewardPurchaseIntentStore) -> Unit) {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        ctx.deleteDatabase(database)
        try {
            RewardPurchaseIntentStore(ctx).use(block)
        } finally {
            ctx.deleteDatabase(database)
        }
    }

    private fun intent(
        account: Long = 11, workspace: Long = 91,
        id: UUID = UUID.randomUUID(), epoch: Long = 7
    ) = RewardPurchaseIntentStore.Intent(
        id, account, workspace, 24, 42, epoch, "MANAGER", "PREPARED"
    )

    @Test fun identitySurvivesSqliteReopen() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        ctx.deleteDatabase(database)
        val first = intent()
        try {
            RewardPurchaseIntentStore(ctx).use { it.prepare(first) }
            RewardPurchaseIntentStore(ctx).use { reopened ->
                assertEquals(first, reopened.scoped(11, 91).single())
            }
        } finally { ctx.deleteDatabase(database) }
    }

    @Test fun accountAndWorkspaceAreStrictlyIsolated() = fresh { store ->
        val one = intent()
        val two = intent(account = 12, workspace = 92)
        store.prepare(one)
        store.prepare(two)
        assertEquals(listOf(one), store.scoped(11, 91))
        assertEquals(listOf(two), store.scoped(12, 92))
        assertTrue(store.scoped(12, 91).isEmpty())
        assertTrue(store.scoped(11, 92).isEmpty())
    }

    @Test fun duplicateIntentIsRefusedWithoutReplacingOriginalUuid() = fresh { store ->
        val first = intent()
        store.prepare(first)
        assertThrows(IllegalStateException::class.java) { store.prepare(intent()) }
        assertEquals(first.operationId, store.scoped(11, 91).single().operationId)
        assertThrows(android.database.SQLException::class.java) {
            store.prepare(intent(account = 12, workspace = 92, id = first.operationId))
        }
    }

    @Test fun epochDriftQuarantinesAndRetainsOriginal() = fresh { store ->
        val entry = intent()
        store.prepare(entry)
        assertEquals(1, store.quarantineEpoch(91, 8))
        assertEquals("HELD", store.scoped(11, 91).single().state)
        assertEquals(entry.operationId, store.scoped(11, 91).single().operationId)
        assertThrows(IllegalArgumentException::class.java) {
            store.transition(store.scoped(11, 91).single(), "UNCERTAIN", 11, 91, 8)
        }
    }

    @Test fun unknownOutcomeSurvivesAndCannotBecomeConfirmedWithoutReceipt() = fresh { store ->
        val entry = intent()
        store.prepare(entry)
        store.transition(entry, "UNCERTAIN", 11, 91, 7)
        val unknown = store.scoped(11, 91).single()
        assertEquals("UNCERTAIN", unknown.state)
        assertThrows(IllegalStateException::class.java) {
            store.transition(unknown, "CONFIRMED", 11, 91, 7)
        }
        assertEquals("UNCERTAIN", store.scoped(11, 91).single().state)
    }

    @Test fun ownerSwitchCannotMutateForeignIntent() = fresh { store ->
        val entry = intent()
        store.prepare(entry)
        assertThrows(IllegalStateException::class.java) {
            store.transition(entry, "HELD", 12, 91, 7)
        }
        assertEquals("PREPARED", store.scoped(11, 91).single().state)
    }
}
