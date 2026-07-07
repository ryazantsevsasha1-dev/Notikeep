package com.notikeep.presentation.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/** Thin helpers to jump the user into the relevant system settings screens. */
object SystemSettings {

    fun openNotificationAccess(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    /** Special-access screen where the user grants PACKAGE_USAGE_STATS. */
    fun openUsageAccess(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    fun requestIgnoreBatteryOptimization(context: Context) {
        val intent = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
