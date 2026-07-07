package com.notikeep.data.service

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Foreground-usage ranking for installed apps via [UsageStatsManager].
 * Needs the special PACKAGE_USAGE_STATS grant; callers must handle the
 * no-permission case (empty map) and fall back to alphabetical order.
 */
@Singleton
class UsageStatsProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun hasPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Total foreground time per package over the last 30 days. Empty when the
     * permission is missing. Runs off the main thread: the underlying call is IPC.
     */
    suspend fun foregroundTimeByPackage(): Map<String, Long> = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext emptyMap()
        val end = System.currentTimeMillis()
        val start = end - TimeUnit.DAYS.toMillis(30)
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        manager.queryUsageStats(UsageStatsManager.INTERVAL_MONTHLY, start, end)
            .orEmpty()
            .groupBy { it.packageName }
            .mapValues { (_, stats) -> stats.sumOf { it.totalTimeInForeground } }
            .filterValues { it > 0 }
    }
}
