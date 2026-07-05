package com.notikeep.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.notikeep.domain.repository.NotificationRepository
import com.notikeep.domain.repository.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Periodically drops notifications older than the user's retention window.
 * Reads the window at run time so changing it in settings takes effect next run.
 */
@HiltWorker
class RetentionCleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val notifications: NotificationRepository,
    private val settings: SettingsRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val retentionDays = settings.observe().first().retentionDays
        val threshold = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(retentionDays.toLong())
        notifications.deleteOlderThan(threshold)
        return Result.success()
    }

    companion object {
        const val UNIQUE_NAME = "notikeep_retention_cleanup"
    }
}
