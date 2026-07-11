package com.notikeep.data.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.notikeep.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Posts (and updates in place) a low-importance notification summarising how many
 * notifications Notikeep captured today, and how many of those were silenced.
 * Uses a fixed id so repeated updates replace the same notification rather than
 * stacking, and [NotificationCompat.Builder.setOnlyAlertOnce] so live updates stay quiet.
 */
@Singleton
class DailySummaryNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val manager = NotificationManagerCompat.from(context)

    @Volatile private var channelReady = false

    /**
     * Create the channel once. IMPORTANCE_MIN keeps the summary silent and out of
     * the status bar's alerting path, so live updates never wake the screen.
     */
    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || channelReady) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.daily_summary_channel_name),
            NotificationManager.IMPORTANCE_MIN,
        ).apply {
            setSound(null, null)
            enableVibration(false)
            enableLights(false)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
        channelReady = true
    }

    private fun canPost(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    // Permission is checked at runtime by canPost() and the notify() call is
    // additionally wrapped in a SecurityException guard below.
    @SuppressLint("MissingPermission")
    fun update(total: Int, silenced: Int) {
        if (!canPost()) return
        ensureChannel()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.daily_summary_title))
            .setContentText(context.getString(R.string.daily_summary_text, total, silenced))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setShowWhen(false)
            .build()
        // canPost() already guards the permission; the try/catch covers the rare
        // race where the user revokes it between the check and this call.
        try {
            manager.notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permission gone; nothing to show.
        }
    }

    fun clear() = manager.cancel(NOTIFICATION_ID)

    private companion object {
        const val CHANNEL_ID = "notikeep_daily_summary"
        const val NOTIFICATION_ID = 4201
    }
}
