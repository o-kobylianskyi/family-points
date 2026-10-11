package com.olehkobylianskyi.familypoints.android.storage

import android.content.Context
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class OfflineBalanceStore(context: Context) : SQLiteOpenHelper(context.applicationContext, "fp_snapshots.db", null, 1) {
    data class Snapshot(val balance: Long, val savedAt: Long)

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE balances (workspace_id INTEGER NOT NULL, member_id INTEGER NOT NULL, balance INTEGER NOT NULL, saved_at INTEGER NOT NULL, PRIMARY KEY (workspace_id, member_id))")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    @Synchronized
    fun save(workspaceId: Long, memberId: Long, balance: Long) {
        val values = ContentValues().apply {
            put("workspace_id", workspaceId)
            put("member_id", memberId)
            put("balance", balance)
            put("saved_at", System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict("balances", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    @Synchronized
    fun load(workspaceId: Long, memberId: Long): Snapshot? =
        readableDatabase.query("balances", arrayOf("balance", "saved_at"), "workspace_id=? AND member_id=?", arrayOf(workspaceId.toString(), memberId.toString()), null, null, null).use {
            if (it.moveToFirst()) Snapshot(it.getLong(0), it.getLong(1)) else null
        }
}
