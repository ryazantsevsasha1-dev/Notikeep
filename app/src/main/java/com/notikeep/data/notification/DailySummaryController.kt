package com.notikeep.data.notification

import com.notikeep.domain.repository.NotificationRepository
import com.notikeep.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the daily-summary notification in sync with today's capture counts, in
 * real time. Subscribes once (from [com.notikeep.NotikeepApp]) in the app scope so
 * it survives independently of any screen.
 *
 * When the toggle is off it clears the notification; when on it updates on every
 * capture. The "start of today" boundary is recomputed on each emission, so the
 * counter naturally resets after midnight.
 */
@Singleton
class DailySummaryController @Inject constructor(
    private val settings: SettingsRepository,
    private val notifications: NotificationRepository,
    private val notifier: DailySummaryNotifier,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun start(scope: CoroutineScope) {
        settings.observe()
            .map { it.dailySummaryEnabled }
            .distinctUntilChanged()
            .flatMapLatest { enabled ->
                if (!enabled) {
                    notifier.clear()
                    flowOf(null)
                } else {
                    notifications.observeDailyCounts(startOfToday())
                }
            }
            // Room re-emits on every insert (even from other apps); only re-post the
            // notification when the shown numbers actually change, so we don't wake
            // the device on every capture.
            .distinctUntilChanged()
            .onEach { counts ->
                if (counts != null) notifier.update(counts.total, counts.silenced)
            }
            .launchIn(scope)
    }

    private fun startOfToday(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
