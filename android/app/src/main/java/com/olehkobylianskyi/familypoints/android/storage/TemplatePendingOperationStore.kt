package com.olehkobylianskyi.familypoints.android.storage

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.UUID

/**
 * Durable receipt-recovery journal, NOT an offline execution queue.
 * Rows remain after timeout until the exact server receipt is confirmed.
 * Callers MUST verify authenticated account, family and current server epoch
 * before displaying or sending a reviewed operation.
 */
class TemplatePendingOperationStore(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "fp_template_receipts.db", null, 1) {

    data class Pending(
        val operationId: UUID,
        val accountId: Long,
        val workspaceId: Long,
        val epoch: Long,
        val kind: String,
        val templateId: Long?,
        val payload: String,
        val status: String
    )

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE pending_template_receipts (
            operation_id TEXT PRIMARY KEY NOT NULL,
            account_id INTEGER NOT NULL,
            workspace_id INTEGER NOT NULL,
            data_epoch INTEGER NOT NULL,
            operation_kind TEXT NOT NULL,
            template_id INTEGER,
            payload TEXT NOT NULL,
            status TEXT NOT NULL CHECK(status IN ('PREPARED','UNCERTAIN','CONFIRMED','HELD'))
        )""")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) =
        error("Unexpected template receipt schema upgrade: $oldVersion -> $newVersion")

    @Synchronized
    fun prepare(value: Pending) {
        require(value.accountId > 0 && value.workspaceId > 0 && value.epoch >= 0)
        require(value.kind in setOf("IMPORT", "TASK", "REWARD"))
        require((value.kind == "IMPORT") == (value.templateId == null))
        require(value.templateId == null || value.templateId > 0)
        require(value.payload.isNotBlank() && value.status == "PREPARED")
        val db = writableDatabase
        val row = ContentValues().apply {
            put("operation_id", value.operationId.toString())
            put("account_id", value.accountId)
            put("workspace_id", value.workspaceId)
            put("data_epoch", value.epoch)
            put("operation_kind", value.kind)
            put("template_id", value.templateId)
            put("payload", value.payload)
            put("status", "PREPARED")
        }
        // Refuse concurrent/uncertain imports from any locally known account in
        // the same family. This check and insertion must be one DB transaction.
        db.beginTransaction()
        try {
            if (value.kind == "IMPORT") {
                val exists = db.rawQuery(
                    """SELECT 1 FROM pending_template_receipts
                       WHERE workspace_id=? AND operation_kind='IMPORT'
                         AND status!='CONFIRMED' LIMIT 1""",
                    arrayOf(value.workspaceId.toString())
                ).use { it.moveToFirst() }
                check(!exists) { "Existing family import requires receipt reconciliation" }
            }
            if (value.kind == "TASK" || value.kind == "REWARD") {
                require(value.payload.startsWith("gcm1:")) { "Activation payload must be encrypted" }
                val exists = db.rawQuery(
                    """SELECT 1 FROM pending_template_receipts
                       WHERE workspace_id=? AND operation_kind=? AND template_id=?
                       AND status!='CONFIRMED' LIMIT 1""",
                    arrayOf(value.workspaceId.toString(), value.kind, value.templateId.toString())
                ).use { it.moveToFirst() }
                check(!exists) { "Existing activation requires receipt reconciliation" }
            }
            // Never replace an operation UUID or immutable payload on retries.
            db.insertOrThrow("pending_template_receipts", null, row)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    @Synchronized
    fun pending(accountId: Long, workspaceId: Long): List<Pending> =
        readableDatabase.rawQuery(
            """SELECT operation_id,account_id,workspace_id,data_epoch,operation_kind,
               template_id,payload,status FROM pending_template_receipts
               WHERE account_id=? AND workspace_id=? AND status!='CONFIRMED'
               ORDER BY rowid""",
            arrayOf(accountId.toString(), workspaceId.toString())
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(Pending(
                        UUID.fromString(cursor.getString(0)), cursor.getLong(1),
                        cursor.getLong(2), cursor.getLong(3), cursor.getString(4),
                        if (cursor.isNull(5)) null else cursor.getLong(5),
                        cursor.getString(6), cursor.getString(7)
                    ))
                }
            }
        }

    @Synchronized
    fun transition(operation: Pending, status: String, verifiedEpoch: Long) {
        require(status in setOf("UNCERTAIN", "CONFIRMED", "HELD"))
        require(verifiedEpoch >= 0)
        // Epoch drift never confirms or deletes previously prepared operations.
        val target = if (verifiedEpoch == operation.epoch) status else "HELD"
        val values = ContentValues().apply { put("status", target) }
        val updated = writableDatabase.update(
            "pending_template_receipts", values,
            """operation_id=? AND account_id=? AND workspace_id=? AND data_epoch=?
               AND operation_kind=? AND payload=? AND status!='CONFIRMED'""",
            arrayOf(operation.operationId.toString(), operation.accountId.toString(),
                operation.workspaceId.toString(), operation.epoch.toString(),
                operation.kind, operation.payload)
        )
        check(updated == 1) { "Operation identity or state changed" }
    }
}
