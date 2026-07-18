package com.notikeep.presentation.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * Thin helpers to jump the user into the relevant system settings screens.
 * Every launch is guarded: some OEM firmwares don't resolve these intents
 * (notably the direct battery-optimization dialog), and an unresolved intent
 * is an [android.content.ActivityNotFoundException] — a crash on a button tap.
 */
object SystemSettings {

    fun openNotificationAccess(context: Context) {
        startSafely(
            context,
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
            fallback = Intent(Settings.ACTION_SETTINGS),
        )
    }

    /** Special-access screen where the user grants PACKAGE_USAGE_STATS. */
    fun openUsageAccess(context: Context) {
        startSafely(
            context,
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),
            fallback = Intent(Settings.ACTION_SETTINGS),
        )
    }

    fun requestIgnoreBatteryOptimization(context: Context) {
        startSafely(
            context,
            Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:${context.packageName}"),
            ),
            // The direct dialog is the intent OEMs most often drop; the plain
            // battery-optimization list exists nearly everywhere.
            fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
        )
    }

    private fun startSafely(context: Context, intent: Intent, fallback: Intent) {
        runCatching {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.recoverCatching {
            context.startActivity(fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
