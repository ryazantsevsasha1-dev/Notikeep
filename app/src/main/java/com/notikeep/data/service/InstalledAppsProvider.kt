package com.notikeep.data.service

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** A launchable app the user can set a rule for. */
data class InstalledApp(val packageName: String, val label: String)

/**
 * Lists user-facing apps (those with a launcher entry), excluding Notikeep itself.
 * Lives in the data layer because it depends on PackageManager.
 *
 * Performance notes:
 *  - a single `queryIntentActivities(MAIN/LAUNCHER)` replaces the previous
 *    per-package `getLaunchIntentForPackage` (one IPC instead of hundreds);
 *  - the result is cached in memory: the installed-app set changes rarely, and
 *    re-resolving 100+ labels on every screen open is what caused the jank.
 */
@Singleton
class InstalledAppsProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val mutex = Mutex()
    private var cache: List<InstalledApp>? = null

    suspend fun listLaunchable(forceRefresh: Boolean = false): List<InstalledApp> =
        mutex.withLock {
            cache?.takeIf { !forceRefresh } ?: load().also { cache = it }
        }

    private suspend fun load(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
            .asSequence()
            .map { it.activityInfo.applicationInfo }
            .distinctBy { it.packageName }
            .filter { it.packageName != context.packageName }
            .map { InstalledApp(it.packageName, pm.getApplicationLabel(it).toString()) }
            .sortedBy { it.label.lowercase() }
            .toList()
    }
}
