package com.olehkobylianskyi.familypoints.android

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.olehkobylianskyi.familypoints.android.storage.OfflinePointsQueueStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfflinePointsQueueIsolationTest {
    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @After fun clean() { context.deleteDatabase("fp_pending_points.db") }

    @Test fun sameWorkspaceAndViewerDistinctAccountsCannotReadOrConfirmEachOthersRows() {
        clean()
        val queue = OfflinePointsQueueStore(context)
        try {
            val a = queue.enqueue(100, 200, 301, 400, 1, "POINTS", "Points", "earn", 10, null)
            val b = queue.enqueue(100, 200, 302, 400, 1, "POINTS", "Points", "penalty", 5, null)
            assertNotEquals(a.operationId, b.operationId)
            assertEquals(listOf(a.operationId), queue.pending(100, 200, 301).map { it.operationId })
            assertEquals(listOf(b.operationId), queue.pending(100, 200, 302).map { it.operationId })
            queue.confirm(a.operationId, 100, 200, 302) // wrong account must not delete
            assertEquals(1, queue.pending(100, 200, 301).size)
            queue.confirm(a.operationId, 100, 200, 301)
            assertEquals(0, queue.pending(100, 200, 301).size)
            assertEquals(1, queue.pending(100, 200, 302).size)
        } finally { queue.close() }
    }

    @Test fun legacyV2RowsRemainDurableQuarantinedAfterV3Upgrade() {
        clean()
        context.openOrCreateDatabase("fp_pending_points.db", Context.MODE_PRIVATE, null).use { db ->
            db.execSQL("""CREATE TABLE pending_points (
                operation_id TEXT NOT NULL PRIMARY KEY,
                workspace_id INTEGER NOT NULL, viewer_id INTEGER NOT NULL,
                member_id INTEGER NOT NULL, point_type_id INTEGER,
                point_type_code TEXT, point_type_name TEXT,
                operation_type TEXT NOT NULL, amount INTEGER NOT NULL,
                description TEXT, created_at INTEGER NOT NULL)""")
            db.execSQL("INSERT INTO pending_points VALUES ('legacy-1', 100, 200, 400, 1, 'POINTS', 'Points', 'earn', 10, NULL, 1)")
            db.version = 2
        }
        val queue = OfflinePointsQueueStore(context)
        try {
            assertEquals(1, queue.legacyUnassignedCount())
            assertEquals(0, queue.pending(100, 200, 301).size)
            assertEquals(0, queue.pending(100, 200, 302).size)
            assertEquals(1, queue.legacyUnassignedCount())
        } finally { queue.close() }
    }
}
