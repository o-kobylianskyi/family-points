package com.olehkobylianskyi.familypoints.android.storage

import android.content.Context
import com.google.gson.Gson
import com.olehkobylianskyi.familypoints.android.data.TaskDefinitionResponse
import com.olehkobylianskyi.familypoints.android.data.TaskInstanceResponse

/** A dated read-only snapshot of the current member's visible tasks. */
class OfflineTasksStore(context: Context) {
    data class Snapshot(
        val tasks: List<TaskInstanceResponse>,
        val openTasks: List<TaskDefinitionResponse>,
        val definitions: List<TaskDefinitionResponse>
    )

    private val prefs = context.applicationContext.getSharedPreferences(
        "fp_task_snapshots", Context.MODE_PRIVATE
    )
    private val gson = Gson()

    private fun key(workspaceId: Long, viewerMemberId: Long, memberId: Long, date: String): String =
        "w${workspaceId}_viewer${viewerMemberId}_target${memberId}_d${date}"

    fun load(workspaceId: Long, viewerMemberId: Long, memberId: Long, date: String): Snapshot? = try {
        prefs.getString(key(workspaceId, viewerMemberId, memberId, date), null)
            ?.let { gson.fromJson(it, Snapshot::class.java) }
    } catch (_: Exception) {
        null
    }

    fun save(workspaceId: Long, viewerMemberId: Long, memberId: Long, date: String, snapshot: Snapshot) {
        prefs.edit().putString(key(workspaceId, viewerMemberId, memberId, date), gson.toJson(snapshot)).apply()
    }
}
