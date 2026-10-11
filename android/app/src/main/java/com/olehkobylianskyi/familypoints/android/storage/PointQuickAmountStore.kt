package com.olehkobylianskyi.familypoints.android.storage

import android.content.Context

/**
 * Remembers only the last successful manual amount for each workspace, account,
 * operation and currency. This is a UI convenience, not a point transaction.
 */
class PointQuickAmountStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "fp_point_quick_amounts", Context.MODE_PRIVATE
    )

    private fun key(workspaceId: Long, viewerId: Long, operation: String, pointTypeId: Long) =
        "w${workspaceId}_v${viewerId}_${operation}_p${pointTypeId}"

    fun suggested(
        workspaceId: Long,
        viewerId: Long,
        operation: String,
        pointTypeId: Long
    ): Int = prefs.getInt(key(workspaceId, viewerId, operation, pointTypeId), 20)
        .takeIf { it in 1..100_000 } ?: 20

    fun rememberSuccessful(
        workspaceId: Long,
        viewerId: Long,
        operation: String,
        pointTypeId: Long,
        amount: Int
    ) {
        require(amount in 1..100_000)
        prefs.edit().putInt(
            key(workspaceId, viewerId, operation, pointTypeId),
            amount
        ).apply()
    }
}
