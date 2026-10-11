package com.olehkobylianskyi.familypoints.android.storage

import android.content.Context
import com.google.gson.Gson
import com.olehkobylianskyi.familypoints.android.data.DashboardData

/** Read-only dated snapshot for the home screen; never an offline spending permit. */
class OfflineDashboardStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "fp_dashboard_snapshots", Context.MODE_PRIVATE
    )
    private val gson = Gson()

    private fun key(workspaceId: Long, memberId: Long, date: String): String =
        "w${workspaceId}_m${memberId}_d${date}"

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
