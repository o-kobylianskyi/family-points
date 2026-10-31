package com.olehkobylianskyi.familypoints.android.storage

import android.content.Context
import com.google.gson.Gson
import com.olehkobylianskyi.familypoints.android.data.DashboardData

/** Read-only dated snapshot for the home screen; never an offline spending permit. */
class OfflineDashboardStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = context.applicationContext.getSharedPreferences(
        "fp_dashboard_snapshots", Context.MODE_PRIVATE
    )
    private val gson = Gson()

    private fun key(workspaceId: Long, memberId: Long, date: String): String =
        "w${workspaceId}_m${memberId}_d${date}"

    fun clearWorkspace(workspaceId: Long) {
        val prefix = "w${workspaceId}_m"
        val editor = prefs.edit()
        prefs.all.keys.filter { it.startsWith(prefix) }.forEach { editor.remove(it) }
        check(editor.commit())
    }

    /**
     * Invoked only after authenticated server GET /data-epoch succeeds.
     * An actual epoch mismatch quarantines pending operations, and removes
     * only workspace-scoped read-only snapshots.
     */
    fun reconcileAuthenticatedEpoch(workspaceId: Long, serverEpoch: Long) {
        OfflinePointsQueueStore(appContext).use { queue ->
            val previous = queue.knownEpoch(workspaceId)
            if (previous != null && previous != serverEpoch) {
                WorkspaceResetCacheCoordinator(appContext).quarantineAndInvalidate(workspaceId)
                clearWorkspace(workspaceId)
            }
            queue.observeEpoch(workspaceId, serverEpoch)
        }
    }

    fun load(workspaceId: Long, memberId: Long, date: String): DashboardData? = try {
        prefs.getString(key(workspaceId, memberId, date), null)
            ?.let { gson.fromJson(it, DashboardData::class.java) }
    } catch (_: Exception) {
        null
    }

    fun save(workspaceId: Long, memberId: Long, date: String, data: DashboardData) {
        prefs.edit().putString(key(workspaceId, memberId, date), gson.toJson(data)).apply()
    }
}
