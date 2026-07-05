package com.notikeep.data.service

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** A launchable app the user can set a rule for. */
data class InstalledApp(val packageName: String, val label: String)

/**
 * Lists user-facing apps (those with a launcher entry), excluding Notikeep itself.
 * Lives in the data layer because it depends on PackageManager.
 */
class InstalledAppsProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun listLaunchable(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .asSequence()
            .filter { it.packageName != context.packageName }
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .map { InstalledApp(it.packageName, it.label(pm)) }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    private fun ApplicationInfo.label(pm: PackageManager): String =
        pm.getApplicationLabel(this).toString()
}
