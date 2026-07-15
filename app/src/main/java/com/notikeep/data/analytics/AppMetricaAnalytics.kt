package com.notikeep.data.analytics

import android.app.Application
import android.util.Log
import com.notikeep.BuildConfig
import com.notikeep.domain.port.Analytics
import com.notikeep.domain.port.AnalyticsEvent
import com.notikeep.domain.repository.SettingsRepository
import io.appmetrica.analytics.AppMetrica
import io.appmetrica.analytics.AppMetricaConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AppMetrica-backed [Analytics]. Chosen for the RU market: free, works without
 * Google Play Services, RuStore-friendly, and pairs with Yandex Mobile Ads later.
 *
 * Privacy rules (RESEARCH.md, anti-pattern #3) still apply: only anonymous UX
 * events go out, never notification content. The user's consent switch maps to
 * AppMetrica's data-sending flag, and with no API key configured the class
 * degrades to local logging only.
 */
@Singleton
class AppMetricaAnalytics @Inject constructor(
    private val settings: SettingsRepository,
    private val scope: CoroutineScope,
) : Analytics {

    private val enabled = BuildConfig.APPMETRICA_API_KEY.isNotBlank()

    /** Call once from Application.onCreate; a no-op without an API key. */
    fun init(app: Application) {
        if (!enabled) return
        val config = AppMetricaConfig.newConfigBuilder(BuildConfig.APPMETRICA_API_KEY)
            .withSessionTimeout(30)
            .build()
        AppMetrica.activate(app, config)
        // Keep the SDK in sync with the consent switch, now and on every change.
        scope.launch {
            settings.observe()
                .map { it.analyticsEnabled }
                .distinctUntilChanged()
                .collect { AppMetrica.setDataSendingEnabled(it) }
        }
    }

    override fun track(event: AnalyticsEvent) {
        scope.launch {
            if (!settings.observe().first().analyticsEnabled) return@launch
            val attrs = attributes(event)
            if (BuildConfig.DEBUG) Log.d(TAG, "${event.name} $attrs")
            if (enabled) AppMetrica.reportEvent(event.name, attrs)
        }
    }

    private fun attributes(event: AnalyticsEvent): Map<String, Any> = when (event) {
        is AnalyticsEvent.OnboardingStepViewed -> mapOf("step" to event.step)
        is AnalyticsEvent.RuleChanged -> mapOf("state" to event.state)
        is AnalyticsEvent.ServiceHealthChanged -> mapOf("healthy" to event.healthy)
        else -> emptyMap()
    }

    private companion object {
        const val TAG = "NotikeepAnalytics"
    }
}
