package com.notikeep.data.service

import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.notikeep.domain.model.NotificationRecord
import com.notikeep.domain.repository.SettingsRepository
import com.notikeep.domain.usecase.ApplyRuleUseCase
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
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var scope: CoroutineScope

    override fun onListenerConnected() {
        // The user just granted access; remember when so the archive can honestly
        // explain that history starts now (RESEARCH.md, anti-pattern #1).
        scope.launch { settings.markFirstAccessGranted(System.currentTimeMillis()) }
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
        )
    }

    private fun resolveAppLabel(pkg: String): String = runCatching {
        val pm = applicationContext.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault(pkg)
}
