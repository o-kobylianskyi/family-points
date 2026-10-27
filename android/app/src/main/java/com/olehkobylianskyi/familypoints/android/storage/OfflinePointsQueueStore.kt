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
    SQLiteOpenHelper(context.applicationContext, "fp_pending_points.db", null, 4) {

    data class Pending(
        val operationId: String,
        val accountId: Long?,
        val workspaceId: Long,
        val viewerId: Long,
        val memberId: Long,
        val pointTypeId: Long?,
        val pointTypeCode: String?,
        val pointTypeName: String?,
        val type: String,
        val amount: Int,
        val description: String?,
        val createdAt: Long
    )

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE reset_holds (workspace_id INTEGER NOT NULL PRIMARY KEY, reason TEXT NOT NULL)")
        db.execSQL("""
            CREATE TABLE pending_points (
                operation_id TEXT NOT NULL PRIMARY KEY,
                account_id INTEGER,
                workspace_id INTEGER NOT NULL,
                viewer_id INTEGER NOT NULL,
                member_id INTEGER NOT NULL,
                point_type_id INTEGER,
                point_type_code TEXT,
                point_type_name TEXT,
                operation_type TEXT NOT NULL,
                amount INTEGER NOT NULL,
                description TEXT,
                created_at INTEGER NOT NULL
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            // Legacy entries stay NULL: they keep the original default currency
            // semantics instead of being silently converted to another currency.
            db.execSQL("ALTER TABLE pending_points ADD COLUMN point_type_id INTEGER")
            db.execSQL("ALTER TABLE pending_points ADD COLUMN point_type_code TEXT")
            db.execSQL("ALTER TABLE pending_points ADD COLUMN point_type_name TEXT")
        }
        if (oldVersion < 3) {
            // Existing owner-ambiguous rows remain NULL, quarantined and durable.
            db.execSQL("ALTER TABLE pending_points ADD COLUMN account_id INTEGER")
        }
        if (oldVersion < 4) {
            db.execSQL("CREATE TABLE reset_holds (workspace_id INTEGER NOT NULL PRIMARY KEY, reason TEXT NOT NULL)")
        }
    }

    /** Fail-closed workspace reset quarantine. Pending rows are retained for audit/review. */
    @Synchronized
    fun holdForReset(workspaceId: Long) {
        require(workspaceId > 0)
        val row = ContentValues().apply {
            put("workspace_id", workspaceId)
            put("reason", "RESET_REQUIRES_SERVER_RECONCILIATION")
        }
        writableDatabase.insertWithOnConflict(
            "reset_holds", null, row, SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    @Synchronized
    fun isHeldForReset(workspaceId: Long): Boolean =
        readableDatabase.rawQuery(
            "SELECT 1 FROM reset_holds WHERE workspace_id=? LIMIT 1",
            arrayOf(workspaceId.toString())
        ).use { it.moveToFirst() }

    @Synchronized
    fun legacyUnassignedCount(): Int = readableDatabase.rawQuery(
        "SELECT count(*) FROM pending_points WHERE account_id IS NULL", null
    ).use { cursor -> if (cursor.moveToFirst()) cursor.getInt(0) else 0 }

    @Synchronized
    fun enqueue(
        workspaceId: Long,
        viewerId: Long,
        accountId: Long,
        memberId: Long,
        pointTypeId: Long,
        pointTypeCode: String,
        pointTypeName: String,
        type: String,
        amount: Int,
        description: String?
    ): Pending {
        check(!isHeldForReset(workspaceId)) { "Workspace reset hold blocks new offline point operations" }
        require(accountId > 0) { "Verified accountId is mandatory for new offline operations" }
        require(type == "earn" || type == "penalty") { "Only offline earn/penalty allowed" }
        require(pointTypeId > 0 && pointTypeCode.isNotBlank())
        require(amount in 1..100_000)
        require(description == null || description.length <= 255)
        val item = Pending(
            UUID.randomUUID().toString(), accountId, workspaceId, viewerId, memberId,
            pointTypeId, pointTypeCode, pointTypeName,
            type, amount, description, System.currentTimeMillis()
        )
        val values = ContentValues().apply {
            put("operation_id", item.operationId)
            put("account_id", item.accountId)
            put("workspace_id", item.workspaceId)
            put("viewer_id", item.viewerId)
            put("member_id", item.memberId)
            put("point_type_id", item.pointTypeId)
            put("point_type_code", item.pointTypeCode)
            put("point_type_name", item.pointTypeName)
            put("operation_type", item.type)
            put("amount", item.amount)
            put("description", item.description)
            put("created_at", item.createdAt)
        }
        check(writableDatabase.insertOrThrow("pending_points", null, values) >= 0)
        return item
    }

    @Synchronized
    fun pending(workspaceId: Long, viewerId: Long, accountId: Long): List<Pending> =
        readableDatabase.query(
            "pending_points", null, "workspace_id=? AND viewer_id=? AND account_id=?",
            arrayOf(workspaceId.toString(), viewerId.toString(), accountId.toString()),
            null, null, "created_at ASC"
        ).use { c ->
            val items = mutableListOf<Pending>()
            while (c.moveToNext()) {
                items.add(Pending(
                    c.getString(c.getColumnIndexOrThrow("operation_id")),
                    c.getLong(c.getColumnIndexOrThrow("account_id")),
                    c.getLong(c.getColumnIndexOrThrow("workspace_id")),
                    c.getLong(c.getColumnIndexOrThrow("viewer_id")),
                    c.getLong(c.getColumnIndexOrThrow("member_id")),
                    c.getColumnIndexOrThrow("point_type_id").let { col -> if (c.isNull(col)) null else c.getLong(col) },
                    c.getString(c.getColumnIndexOrThrow("point_type_code")),
                    c.getString(c.getColumnIndexOrThrow("point_type_name")),
                    c.getString(c.getColumnIndexOrThrow("operation_type")),
                    c.getInt(c.getColumnIndexOrThrow("amount")),
                    c.getString(c.getColumnIndexOrThrow("description")),
                    c.getLong(c.getColumnIndexOrThrow("created_at"))
                ))
            }
            items
        }

    @Synchronized
    fun confirm(operationId: String, workspaceId: Long, viewerId: Long, accountId: Long) {
        writableDatabase.delete(
            "pending_points",
            "operation_id=? AND workspace_id=? AND viewer_id=? AND account_id=?",
            arrayOf(operationId, workspaceId.toString(), viewerId.toString(), accountId.toString())
        )
    }
}
