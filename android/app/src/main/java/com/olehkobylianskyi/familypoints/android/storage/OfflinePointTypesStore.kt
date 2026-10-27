package com.olehkobylianskyi.familypoints.android.storage

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.olehkobylianskyi.familypoints.android.data.PointTypeResponse

/** Offline view of the server-authoritative currency catalog. */
class OfflinePointTypesStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "fp_point_types_cache", Context.MODE_PRIVATE
    )
    private val gson = Gson()

    private fun key(workspaceId: Long, viewerId: Long) =
        "w${workspaceId}_viewer${viewerId}"

    fun clearWorkspace(workspaceId: Long) {
        val prefix = "w${workspaceId}_viewer"
        val editor = prefs.edit()
        prefs.all.keys.filter { it.startsWith(prefix) }.forEach { editor.remove(it) }
        check(editor.commit())
    }

    fun load(workspaceId: Long, viewerId: Long): List<PointTypeResponse> {
        return try {
            val json = prefs.getString(key(workspaceId, viewerId), null)
                ?: return emptyList()
            val type = object : TypeToken<List<PointTypeResponse>>() {}.type
            gson.fromJson<List<PointTypeResponse>>(json, type).orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(workspaceId: Long, viewerId: Long, currencies: List<PointTypeResponse>) {
        check(prefs.edit().putString(key(workspaceId, viewerId),
            gson.toJson(currencies)).commit())
    }
}
