package com.notikeep.data.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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

    /**
     * Tapping the ongoing summary opens Notikeep. Launches the app's main activity via the
     * package's launch intent (resolved by package name so we don't hard-depend on the class),
     * reusing the existing task if the app is already open. FLAG_IMMUTABLE is required on S+.
     */
    private fun contentIntent(): PendingIntent? {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }
            ?: return null
        return PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun canPost(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    /**
     * Builds the summary notification for the given counts. Exposed so the always-alive
     * listener service can adopt it as its foreground notification, which is what keeps
     * the summary on screen after the app is swiped away (the process — and the app scope —
     * would otherwise be killed, taking a plain notification with it).
     */
    fun build(total: Int, silenced: Int): android.app.Notification {
        ensureChannel()
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setColor(BRAND_BLUE)
            .setContentTitle(context.getString(R.string.daily_summary_title))
            .setContentText(context.getString(R.string.daily_summary_text, total, silenced))
            .setContentIntent(contentIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setShowWhen(false)
            .build()
    }

    // Permission is checked at runtime by canPost() and the notify() call is
    // additionally wrapped in a SecurityException guard below.
    @SuppressLint("MissingPermission")
    fun update(total: Int, silenced: Int) {
        if (!canPost()) return
        // canPost() already guards the permission; the try/catch covers the rare
        // race where the user revokes it between the check and this call.
        try {
            manager.notify(NOTIFICATION_ID, build(total, silenced))
        } catch (_: SecurityException) {
            // Permission gone; nothing to show.
        }
    }

    fun clear() = manager.cancel(NOTIFICATION_ID)

    companion object {
        const val CHANNEL_ID = "notikeep_daily_summary"
        const val NOTIFICATION_ID = 4201
        /** Brand blue (BrandBlue in the theme) used to tint the small icon. */
        const val BRAND_BLUE = 0xFF2E44BE.toInt()
    }
}
