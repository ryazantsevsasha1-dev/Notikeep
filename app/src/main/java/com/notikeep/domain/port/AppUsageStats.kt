package com.notikeep.domain.port

/**
 * Foreground-usage ranking of apps. Backed by UsageStatsManager in the data
 * layer; callers must handle the no-permission case (empty map).
 */
interface AppUsageStats {
    fun hasPermission(): Boolean

    /** Total foreground time per package; empty when the permission is missing. */
    suspend fun foregroundTimeByPackage(): Map<String, Long>
}
