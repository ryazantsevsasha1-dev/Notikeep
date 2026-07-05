package com.notikeep.data.analytics

import android.util.Log
import com.notikeep.domain.port.Analytics
import com.notikeep.domain.port.AnalyticsEvent
import com.notikeep.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local, anonymous analytics. Honours the user's consent flag and never receives
 * notification content — only UX events. Swappable for a RuStore provider later
 * without touching any caller (see [Analytics]).
 */
@Singleton
class LogAnalytics @Inject constructor(
    private val settings: SettingsRepository,
    private val scope: CoroutineScope,
) : Analytics {

    override fun track(event: AnalyticsEvent) {
        scope.launch {
            if (!settings.observe().first().analyticsEnabled) return@launch
            Log.d(TAG, "${event.name} ${describe(event)}")
        }
    }

    private fun describe(event: AnalyticsEvent): String = when (event) {
        is AnalyticsEvent.OnboardingStepViewed -> "step=${event.step}"
        is AnalyticsEvent.RuleChanged -> "state=${event.state}"
        is AnalyticsEvent.ServiceHealthChanged -> "healthy=${event.healthy}"
        else -> ""
    }

    private companion object {
        const val TAG = "NotikeepAnalytics"
    }
}
