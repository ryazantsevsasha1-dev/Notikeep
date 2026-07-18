package com.notikeep.data.analytics

import android.app.Application
import android.util.Log
import com.notikeep.BuildConfig
import com.notikeep.domain.port.Analytics
import com.notikeep.domain.port.AnalyticsEvent
import io.appmetrica.analytics.AppMetrica
import io.appmetrica.analytics.AppMetricaConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AppMetrica-backed [Analytics]. Chosen for the RU market: free, works without
 * Google Play Services, RuStore-friendly, and pairs with Yandex Mobile Ads later.
 *
 * Privacy rules (RESEARCH.md, anti-pattern #3) still apply: only anonymous UX
 * events go out, never notification content. Activation happens only after the
 * user accepts the terms (see NotikeepApp), which is the legal basis for
 * collection; with no API key configured the class degrades to local logging only.
 */
@Singleton
class AppMetricaAnalytics @Inject constructor() : Analytics {

    private val enabled = BuildConfig.APPMETRICA_API_KEY.isNotBlank()

    /** Events must not reach the SDK before [init] has activated it. */
    @Volatile private var activated = false

    /** Called once from the app, only after terms acceptance; a no-op without an API key. */
    fun init(app: Application) {
        if (!enabled || activated) return
        val config = AppMetricaConfig.newConfigBuilder(BuildConfig.APPMETRICA_API_KEY)
            .withSessionTimeout(30)
            .build()
        AppMetrica.activate(app, config)
        AppMetrica.setDataSendingEnabled(true)
        activated = true
    }

    override fun track(event: AnalyticsEvent) {
        val attrs = attributes(event)
        if (BuildConfig.DEBUG) Log.d(TAG, "${event.name} $attrs")
        if (activated) AppMetrica.reportEvent(event.name, attrs)
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
