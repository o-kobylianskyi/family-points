package com.olehkobylianskyi.familypoints.android.storage

import android.content.Context
import com.google.gson.Gson
import com.olehkobylianskyi.familypoints.android.data.RewardsPageData

/**
 * Workspace-scoped cached reward catalog for display.
 * Server approval is still mandatory for purchasing a reward.
 */
class OfflineRewardsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("fp_reward_catalog_cache", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun load(workspaceId: Long): RewardsPageData? = try {
        prefs.getString("workspace_$workspaceId", null)
            ?.let { gson.fromJson(it, RewardsPageData::class.java) }
    } catch (_: Exception) {
        null
    }

    fun save(workspaceId: Long, data: RewardsPageData) {
        // Do not persist transient requests/purchases as if they were up-to-date.
        val safe = data.copy(
            requests = emptyList(),
            purchases = emptyList(),
            obligations = emptyList()
        )
        prefs.edit().putString("workspace_$workspaceId", gson.toJson(safe)).apply()
    }
}
