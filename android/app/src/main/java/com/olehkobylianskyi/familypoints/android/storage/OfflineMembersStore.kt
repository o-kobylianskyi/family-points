package com.olehkobylianskyi.familypoints.android.storage

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.olehkobylianskyi.familypoints.android.data.MembersPageData
import com.olehkobylianskyi.familypoints.android.data.WorkspaceMemberResponse

/** Workspace + viewing-member scoped read-only membership snapshot. */
class OfflineMembersStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "fp_members_offline_cache", Context.MODE_PRIVATE
    )
    private val gson = Gson()

    private fun key(workspaceId: Long, viewerMemberId: Long) =
        "w${workspaceId}_viewer${viewerMemberId}"

    fun cachedMembers(workspaceId: Long, viewerMemberId: Long): List<WorkspaceMemberResponse> =
        try {
            val raw = prefs.getString("members_" + key(workspaceId, viewerMemberId), null)
                ?: return emptyList()
            val type = object : TypeToken<List<WorkspaceMemberResponse>>() {}.type
            gson.fromJson<List<WorkspaceMemberResponse>>(raw, type).orEmpty()
        } catch (_: Exception) {
            emptyList()
        }

    fun saveMembers(workspaceId: Long, viewerMemberId: Long, members: List<WorkspaceMemberResponse>) {
        prefs.edit().putString("members_" + key(workspaceId, viewerMemberId),
            gson.toJson(members)).apply()
    }

    fun cachedPage(workspaceId: Long, viewerMemberId: Long): MembersPageData? = try {
        prefs.getString("page_" + key(workspaceId, viewerMemberId), null)
            ?.let { gson.fromJson(it, MembersPageData::class.java) }
    } catch (_: Exception) {
        null
    }

    fun savePage(workspaceId: Long, viewerMemberId: Long, page: MembersPageData) {
        saveMembers(workspaceId, viewerMemberId, page.members)
        prefs.edit().putString("page_" + key(workspaceId, viewerMemberId),
            gson.toJson(page)).apply()
    }
}
