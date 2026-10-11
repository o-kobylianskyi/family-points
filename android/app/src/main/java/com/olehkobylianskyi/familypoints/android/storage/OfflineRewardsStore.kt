package com.olehkobylianskyi.familypoints.android.storage

import android.content.Context
import com.google.gson.Gson
import com.olehkobylianskyi.familypoints.android.data.RewardsPageData

/**
 * Read-only catalog snapshot. Scope by both workspace and logged-in member:
 * different accounts may see different rewards and requirements.
 *
 * A cached reward is NEVER an offline purchase entitlement.
 */
class OfflineRewardsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "fp_reward_catalog_cache", Context.MODE_PRIVATE
    )
    private val gson = Gson()

    private fun key(workspaceId: Long, memberId: Long): String =
        "catalog_w${workspaceId}_m${memberId}"

    fun load(workspaceId: Long, memberId: Long): RewardsPageData? = try {
        prefs.getString(key(workspaceId, memberId), null)
            ?.let { gson.fromJson(it, RewardsPageData::class.java) }
    } catch (_: Exception) {
        null
    }

    fun save(workspaceId: Long, memberId: Long, data: RewardsPageData) {
        // Requests, balances, purchase history and tasks are not authoritative offline.
        val catalog = data.copy(
            requests = emptyList(),
            purchases = emptyList(),
            obligations = emptyList(),
            tasks = emptyList()
        )
        prefs.edit().putString(key(workspaceId, memberId), gson.toJson(catalog)).apply()
    }
}
