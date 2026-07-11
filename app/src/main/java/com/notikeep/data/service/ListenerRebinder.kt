package com.notikeep.data.service

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Recovers a dead notification listener. After an APK update (or sometimes after
 * a crash) Android keeps the access grant but silently never rebinds the service
 * until reboot — the app looks fine, permissions are granted, yet nothing is
 * captured. The known fix is to toggle the service component (which forces the
 * system to re-evaluate enabled listeners) and to ask for an explicit rebind.
 *
 * Safe to call often: it is a no-op when access is missing or the listener is
 * already connected.
 */
@Singleton
class ListenerRebinder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val health: ServiceHealthProvider,
    private val connectionState: ListenerConnectionState,
) {
    fun ensureBound() {
        if (!health.current().notificationAccessGranted) return
        if (connectionState.isConnected) return

        val component = ComponentName(context, NotikeepListenerService::class.java)
        runCatching {
            val pm = context.packageManager
            pm.setComponentEnabledSetting(
                component,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
            pm.setComponentEnabledSetting(
                component,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP,
            )
        }.onFailure { Log.w(TAG, "component toggle failed", it) }

        runCatching {
            NotificationListenerService.requestRebind(component)
        }.onFailure { Log.w(TAG, "requestRebind failed", it) }
    }

    private companion object {
        const val TAG = "ListenerRebinder"
    }
}
