package com.notikeep.data.notification

import com.notikeep.domain.repository.NotificationRepository
import com.notikeep.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
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
 * capture. A [dayStartFlow] re-emits the "start of today" boundary exactly at
 * midnight, so the counter resets to zero at the day change without needing the
 * app to be reopened.
 */
@Singleton
class DailySummaryController @Inject constructor(
    private val settings: SettingsRepository,
    private val notifications: NotificationRepository,
    private val notifier: DailySummaryNotifier,
    private val foregroundHost: DailySummaryForegroundHost,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun start(scope: CoroutineScope) {
        val enabledFlow = settings.observe()
            .map { it.dailySummaryEnabled }
            .distinctUntilChanged()

        combine(enabledFlow, dayStartFlow()) { enabled, dayStart -> enabled to dayStart }
            .flatMapLatest { (enabled, dayStart) ->
                if (!enabled) {
                    if (!foregroundHost.clear()) notifier.clear()
                    flowOf(null)
                } else {
                    notifications.observeDailyCounts(dayStart)
                }
            }
            // Room re-emits on every insert (even from other apps); only re-post the
            // notification when the shown numbers actually change, so we don't wake
            // the device on every capture.
            .distinctUntilChanged()
            .onEach { counts ->
                if (counts != null) post(counts.total, counts.silenced)
            }
            .launchIn(scope)
    }

    /**
     * Prefer the always-alive service's foreground notification so the summary survives
     * the app being swiped away. Only when no service is bound do we fall back to a plain
     * notification (which lives just as long as the app process).
     */
    private fun post(total: Int, silenced: Int) {
        val shown = foregroundHost.show(notifier.build(total, silenced))
        if (!shown) notifier.update(total, silenced)
    }

    /**
     * Emits the current start-of-day epoch millis, then sleeps until the next
     * midnight and emits again — indefinitely. Downstream [flatMapLatest] rebuilds
     * the counts query with the new boundary, resetting "today" at the day change.
     */
    private fun dayStartFlow(): Flow<Long> = flow {
        while (true) {
            val start = startOfToday()
            emit(start)
            val nextMidnight = start + DAY_MILLIS
            delay((nextMidnight - System.currentTimeMillis()).coerceAtLeast(1_000L))
        }
    }

    private fun startOfToday(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private companion object {
        const val DAY_MILLIS = 24 * 60 * 60 * 1000L
    }
}
