package com.notikeep.data.service

import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import com.notikeep.domain.model.ServiceHealth
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Reads live platform state to tell whether capture actually works. Kept in the
 * data layer because it touches Android APIs; exposed to UI via a domain model.
 */
class ServiceHealthProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun current(): ServiceHealth = ServiceHealth(
        notificationAccessGranted = isNotificationAccessGranted(),
        batteryOptimizationIgnored = isBatteryOptimizationIgnored(),
    )

    private fun isNotificationAccessGranted(): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        ).orEmpty()
        return enabled.contains(context.packageName)
    }

    private fun isBatteryOptimizationIgnored(): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }
}
