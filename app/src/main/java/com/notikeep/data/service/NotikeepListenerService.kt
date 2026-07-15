package com.notikeep.data.service

import android.app.Notification
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.notikeep.data.notification.DailySummaryForegroundHost
import com.notikeep.data.notification.DailySummaryNotifier
import com.notikeep.domain.model.NotificationRecord
import com.notikeep.domain.repository.SettingsRepository
import com.notikeep.domain.usecase.ApplyRuleUseCase
import com.notikeep.domain.usecase.BackfillActiveNotificationsUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The core of the product. The system binds this by name and delivers every
 * notification here. This class stays a thin adapter: it maps the platform type
 * into a domain model and delegates the "keep or silence" decision to
 * [ApplyRuleUseCase], which is unit-testable without Android.
 */
@AndroidEntryPoint
class NotikeepListenerService : NotificationListenerService() {

    @Inject lateinit var applyRule: ApplyRuleUseCase
    @Inject lateinit var backfill: BackfillActiveNotificationsUseCase
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var connectionState: ListenerConnectionState
    @Inject lateinit var scope: CoroutineScope
    @Inject lateinit var foregroundHost: DailySummaryForegroundHost

    /**
     * Adopts the daily-summary notification as this service's foreground notification.
     * A foreground service — and its notification — outlives the app being swiped away,
     * which is exactly what keeps the summary on screen after the app is closed.
     */
    private val foregroundDelegate = object : DailySummaryForegroundHost.Delegate {
        override fun showForeground(notification: Notification) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        DailySummaryNotifier.NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                    )
                } else {
                    startForeground(DailySummaryNotifier.NOTIFICATION_ID, notification)
                }
            }
        }

        override fun clearForeground() {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
            }
        }
    }

    override fun onListenerConnected() {
        connectionState.onConnected()
        foregroundHost.register(foregroundDelegate)
        // The user just granted access; remember when so the archive can honestly
        // explain that history starts now (RESEARCH.md, anti-pattern #1).
        scope.launch { settings.markFirstAccessGranted(System.currentTimeMillis()) }
        backfillActiveNotifications()
    }

    override fun onListenerDisconnected() {
        connectionState.onDisconnected()
        foregroundHost.unregister(foregroundDelegate)
    }

    override fun onDestroy() {
        foregroundHost.unregister(foregroundDelegate)
        super.onDestroy()
    }

    /**
     * Pick up everything already sitting in the shade so a fresh install (or a
     * reconnect) starts with the notifications the user can currently see.
     * Storage-level dedup keeps this idempotent; the shade is never touched.
     */
    private fun backfillActiveNotifications() {
        val records = runCatching { activeNotifications.orEmpty().toList() }
            .getOrDefault(emptyList())
            .mapNotNull { it.toRecord() }
        if (records.isEmpty()) return
        scope.launch { backfill(records) }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val record = sbn.toRecord() ?: return
        scope.launch {
            if (applyRule(record) == ApplyRuleUseCase.ShadeAction.CANCEL) {
                cancelNotification(sbn.key)
            }
        }
    }

    /** Ignore our own posts and content-less system noise; extract title+text. */
    private fun StatusBarNotification.toRecord(): NotificationRecord? {
        if (packageName == applicationContext.packageName) return null
        val extras = notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return null

        return NotificationRecord(
            id = 0,
            packageName = packageName,
            appLabel = resolveAppLabel(packageName),
            title = title,
            text = text,
            postedAt = postTime,
            wasSilenced = false,
            sbnKey = key,
        )
    }

    private fun resolveAppLabel(pkg: String): String = runCatching {
        val pm = applicationContext.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault(pkg)
}
