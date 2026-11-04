package com.olehkobylianskyi.familypoints.android.data

/** A history page must be read from a single verified workspace generation. */
object RewardHistoryEpochGate {
    fun matches(
        workspaceId: Long,
        beforeWorkspaceId: Long,
        beforeEpoch: Long,
        afterWorkspaceId: Long,
        afterEpoch: Long
    ): Boolean =
        workspaceId > 0L &&
            beforeWorkspaceId == workspaceId &&
            afterWorkspaceId == workspaceId &&
            beforeEpoch >= 0L &&
            beforeEpoch == afterEpoch
}
