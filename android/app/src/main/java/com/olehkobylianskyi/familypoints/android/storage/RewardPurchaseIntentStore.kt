package com.olehkobylianskyi.familypoints.android.storage

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.UUID
import com.olehkobylianskyi.familypoints.android.data.RewardPurchaseReceiptPolicy

/**
 * Durable identity-only journal for an eventual online reward purchase.
 * It stores no credentials, session token or financial authorization payload.
 * It is NOT a work queue: nothing in this store sends POST or deletes a pending intent.
 * The caller must verify the current authenticated account/workspace/epoch online.
 */
class RewardPurchaseIntentStore(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "fp_reward_purchase_intents.db", null, 1) {

    data class Intent(
        val operationId: UUID,
        val accountId: Long,
        val workspaceId: Long,
        val memberId: Long,
        val rewardId: Long,
        val epoch: Long,
        val route: String,
        val state: String
    )

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE reward_purchase_intents (
            operation_id TEXT PRIMARY KEY NOT NULL,
            account_id INTEGER NOT NULL,
            workspace_id INTEGER NOT NULL,
            member_id INTEGER NOT NULL,
            reward_id INTEGER NOT NULL,
            data_epoch INTEGER NOT NULL,
            route_kind TEXT NOT NULL CHECK(route_kind IN ('SELF','MANAGER')),
            state TEXT NOT NULL CHECK(state IN ('PREPARED','UNCERTAIN','HELD','CONFIRMED'))
        )""")
        db.execSQL("""CREATE INDEX reward_purchase_intent_scope_idx
            ON reward_purchase_intents(account_id,workspace_id,state)""")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) =
        error("Unexpected reward intent schema upgrade: $oldVersion -> $newVersion")

    /** Fails rather than replace the immutable original UUID or intent. */
    @Synchronized
    fun prepare(intent: Intent) {
        require(intent.accountId > 0 && intent.workspaceId > 0 &&
            intent.memberId > 0 && intent.rewardId > 0 && intent.epoch >= 0)
        require(intent.route == "SELF" || intent.route == "MANAGER")
        require(intent.state == "PREPARED")
        require(intent.operationId.toString() == intent.operationId.toString().lowercase())
        val db = writableDatabase
        db.beginTransaction()
        try {
            // Avoid an accidental second charge request while an earlier attempt
            // for the same actor/member/reward is still unresolved.
            val exists = db.rawQuery(
                """SELECT 1 FROM reward_purchase_intents
                   WHERE account_id=? AND workspace_id=? AND member_id=? AND reward_id=?
                     AND state IN ('PREPARED','UNCERTAIN','HELD') LIMIT 1""",
                arrayOf(intent.accountId.toString(), intent.workspaceId.toString(),
                    intent.memberId.toString(), intent.rewardId.toString())
            ).use { it.moveToFirst() }
            check(!exists) { "Previous reward purchase must be reviewed first" }
            db.insertOrThrow("reward_purchase_intents", null, ContentValues().apply {
                put("operation_id", intent.operationId.toString())
                put("account_id", intent.accountId)
                put("workspace_id", intent.workspaceId)
                put("member_id", intent.memberId)
                put("reward_id", intent.rewardId)
                put("data_epoch", intent.epoch)
                put("route_kind", intent.route)
                put("state", intent.state)
            })
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    @Synchronized
    fun scoped(accountId: Long, workspaceId: Long): List<Intent> {
        require(accountId > 0 && workspaceId > 0)
        return readableDatabase.rawQuery(
            """SELECT operation_id,account_id,workspace_id,member_id,reward_id,
                      data_epoch,route_kind,state FROM reward_purchase_intents
               WHERE account_id=? AND workspace_id=? ORDER BY rowid ASC""",
            arrayOf(accountId.toString(), workspaceId.toString())
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(Intent(
                    UUID.fromString(cursor.getString(0)), cursor.getLong(1),
                    cursor.getLong(2), cursor.getLong(3), cursor.getLong(4),
                    cursor.getLong(5), cursor.getString(6), cursor.getString(7)
                ))
            }
        }
    }

    /**
     * Re-check scope/epoch on every local transition. Drift always leads to HELD.
     * CONFIRMED is accepted only when the caller has already checked the exact
     * authenticated read-only server receipt; this store never interprets HTTP.
     * Neither HELD nor CONFIRMED can be reopened automatically.
     */
    @Synchronized
    fun transition(
        original: Intent,
        requestedState: String,
        verifiedAccountId: Long,
        verifiedWorkspaceId: Long,
        verifiedEpoch: Long
    ) {
        require(requestedState in setOf("UNCERTAIN", "HELD"))
        require(verifiedAccountId > 0 && verifiedWorkspaceId > 0 && verifiedEpoch >= 0)
        require(original.state in setOf("PREPARED", "UNCERTAIN"))
        val target = if (original.accountId != verifiedAccountId ||
            original.workspaceId != verifiedWorkspaceId ||
            original.epoch != verifiedEpoch) "HELD" else requestedState
        // A foreign account must never update another account's intent.
        check(original.accountId == verifiedAccountId &&
            original.workspaceId == verifiedWorkspaceId) { "Intent owner mismatch" }
        val changed = writableDatabase.update(
            "reward_purchase_intents",
            ContentValues().apply { put("state", target) },
            """operation_id=? AND account_id=? AND workspace_id=? AND member_id=?
               AND reward_id=? AND data_epoch=? AND route_kind=? AND state=?""",
            arrayOf(original.operationId.toString(), original.accountId.toString(),
                original.workspaceId.toString(), original.memberId.toString(),
                original.rewardId.toString(), original.epoch.toString(),
                original.route, original.state)
        )
        check(changed == 1) { "Intent was changed or already resolved" }
    }

    /**
     * Only a validated read-only server receipt may confirm an intent.
     * This is a local journal acknowledgement, NEVER a purchase/fulfillment action.
     */
    @Synchronized
    internal fun confirmReceipt(
        original: Intent,
        receipt: RewardPurchaseReceiptPolicy.Receipt,
        verifiedAccountId: Long,
        verifiedWorkspaceId: Long,
        verifiedEpoch: Long
    ) {
        require(original.state in setOf("PREPARED", "UNCERTAIN"))
        check(original.accountId == verifiedAccountId &&
            original.workspaceId == verifiedWorkspaceId &&
            original.epoch == verifiedEpoch) { "Receipt scope drift requires review" }
        val decision = RewardPurchaseReceiptPolicy.evaluate(
            RewardPurchaseReceiptPolicy.Intent(
                original.operationId.toString(), original.workspaceId,
                original.accountId, original.rewardId, original.memberId, original.epoch
            ), receipt, verifiedWorkspaceId, verifiedAccountId, verifiedEpoch
        )
        check(decision == RewardPurchaseReceiptPolicy.Decision.ACCEPT) {
            "Unverified purchase receipt cannot confirm local intent"
        }
        val changed = writableDatabase.update(
            "reward_purchase_intents",
            ContentValues().apply { put("state", "CONFIRMED") },
            """operation_id=? AND account_id=? AND workspace_id=? AND member_id=?
               AND reward_id=? AND data_epoch=? AND route_kind=? AND state=?""",
            arrayOf(original.operationId.toString(), original.accountId.toString(),
                original.workspaceId.toString(), original.memberId.toString(),
                original.rewardId.toString(), original.epoch.toString(),
                original.route, original.state)
        )
        check(changed == 1) { "Original intent changed before receipt reconciliation" }
    }

    /** Quarantine all unresolved intents on generation change; never delete rows. */
    @Synchronized
    fun quarantineEpoch(workspaceId: Long, verifiedEpoch: Long): Int {
        require(workspaceId > 0 && verifiedEpoch >= 0)
        return writableDatabase.update(
            "reward_purchase_intents",
            ContentValues().apply { put("state", "HELD") },
            """workspace_id=? AND data_epoch!=? AND state IN ('PREPARED','UNCERTAIN')""",
            arrayOf(workspaceId.toString(), verifiedEpoch.toString())
        )
    }
}
