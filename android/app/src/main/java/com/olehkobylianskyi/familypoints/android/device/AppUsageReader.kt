package com.olehkobylianskyi.familypoints.android.device

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process

data class AppUsageSummary(
    val packageName: String,
    val displayName: String,
    val foregroundMillis: Long,
    val lastTimeUsedMillis: Long
)

/**
 * Read-only app usage discovery. Does not grant permissions, block applications,
 * launch other apps, or assume Android allows termination of another process.
 */
class AppUsageReader(private val context: Context) {
    fun hasUsageAccess(): Boolean {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = ops.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun recentUsage(fromMillis: Long, toMillis: Long): List<AppUsageSummary> {
        require(fromMillis >= 0 && toMillis > fromMillis) { "Invalid usage interval" }
        if (!hasUsageAccess()) return emptyList()
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val stats = manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, fromMillis, toMillis)
            .orEmpty()
        return stats.groupBy { it.packageName }.map { (packageName, entries) ->
            val name = try {
                val info = context.packageManager.getApplicationInfo(packageName, 0)
                context.packageManager.getApplicationLabel(info).toString()
            } catch (_: Exception) {
                packageName
            }
            AppUsageSummary(
                packageName = packageName,
                displayName = name,
                foregroundMillis = entries.sumOf { it.totalTimeInForeground },
                lastTimeUsedMillis = entries.maxOfOrNull { it.lastTimeUsed } ?: 0
            )
        }.filter { it.foregroundMillis > 0L }
            .sortedByDescending { it.foregroundMillis }
    }
}
