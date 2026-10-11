package com.olehkobylianskyi.familypoints.android.storage

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.UUID

/**
 * Durable pending administrative point operations.
 * Never modify confirmed balance here. Only a verified backend response
 * removes the pending operation from the queue.
 */
class OfflinePointsQueueStore(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "fp_pending_points.db", null, 1) {

    data class Pending(
        val operationId: String,
        val workspaceId: Long,
        val viewerId: Long,
        val memberId: Long,
        val type: String,
        val amount: Int,
        val description: String?,
        val createdAt: Long
    )

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE pending_points (
                operation_id TEXT NOT NULL PRIMARY KEY,
                workspace_id INTEGER NOT NULL,
                viewer_id INTEGER NOT NULL,
                member_id INTEGER NOT NULL,
                operation_type TEXT NOT NULL,
                amount INTEGER NOT NULL,
                description TEXT,
                created_at INTEGER NOT NULL
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Migrate in place in future; pending operations must never be dropped.
    }

    @Synchronized
    fun enqueue(
        workspaceId: Long,
        viewerId: Long,
        memberId: Long,
        type: String,
        amount: Int,
        description: String?
    ): Pending {
        require(type == "earn" || type == "penalty") { "Only offline earn/penalty allowed" }
        require(amount in 1..100_000)
        require(description == null || description.length <= 255)
        val item = Pending(
            UUID.randomUUID().toString(), workspaceId, viewerId, memberId,
            type, amount, description, System.currentTimeMillis()
        )
        val values = ContentValues().apply {
            put("operation_id", item.operationId)
            put("workspace_id", item.workspaceId)
            put("viewer_id", item.viewerId)
            put("member_id", item.memberId)
            put("operation_type", item.type)
            put("amount", item.amount)
            put("description", item.description)
            put("created_at", item.createdAt)
        }
        check(writableDatabase.insertOrThrow("pending_points", null, values) >= 0)
        return item
    }

    @Synchronized
    fun pending(workspaceId: Long, viewerId: Long): List<Pending> =
        readableDatabase.query(
            "pending_points", null, "workspace_id=? AND viewer_id=?",
            arrayOf(workspaceId.toString(), viewerId.toString()),
            null, null, "created_at ASC"
        ).use { c ->
            val items = mutableListOf<Pending>()
            while (c.moveToNext()) {
                items.add(Pending(
                    c.getString(c.getColumnIndexOrThrow("operation_id")),
                    c.getLong(c.getColumnIndexOrThrow("workspace_id")),
                    c.getLong(c.getColumnIndexOrThrow("viewer_id")),
                    c.getLong(c.getColumnIndexOrThrow("member_id")),
                    c.getString(c.getColumnIndexOrThrow("operation_type")),
                    c.getInt(c.getColumnIndexOrThrow("amount")),
                    c.getString(c.getColumnIndexOrThrow("description")),
                    c.getLong(c.getColumnIndexOrThrow("created_at"))
                ))
            }
            items
        }

    @Synchronized
    fun confirm(operationId: String, workspaceId: Long, viewerId: Long) {
        writableDatabase.delete(
            "pending_points",
            "operation_id=? AND workspace_id=? AND viewer_id=?",
            arrayOf(operationId, workspaceId.toString(), viewerId.toString())
        )
    }
}
