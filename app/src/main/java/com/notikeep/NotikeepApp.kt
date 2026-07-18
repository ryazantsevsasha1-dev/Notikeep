package com.notikeep

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.notikeep.data.ads.AdsInitializer
import com.notikeep.data.ads.AdsRemoteConfig
import com.notikeep.data.analytics.AppMetricaAnalytics
import com.notikeep.data.icons.AppIconFetcher
import com.notikeep.data.icons.AppIconKeyer
import com.notikeep.data.notification.DailySummaryController
import com.notikeep.data.work.ListenerWatchdogWorker
import com.notikeep.data.work.RetentionCleanupWorker
import com.notikeep.domain.repository.SettingsRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/** Hilt entry point. Also wires WorkManager to Hilt and schedules retention cleanup. */
@HiltAndroidApp
class NotikeepApp : Application(), Configuration.Provider, ImageLoaderFactory {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var appMetrica: AppMetricaAnalytics
    @Inject lateinit var dailySummaryController: DailySummaryController
    @Inject lateinit var adsInitializer: AdsInitializer
    @Inject lateinit var adsRemoteConfig: AdsRemoteConfig
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var appScope: CoroutineScope

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    /** App-wide Coil loader that knows how to fetch launcher icons by package name. */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components {
                add(AppIconKeyer())
                add(AppIconFetcher.Factory(this@NotikeepApp))
            }
            .build()

    override fun onCreate() {
        super.onCreate()
        scheduleRetentionCleanup()
        scheduleListenerWatchdog()
        dailySummaryController.start(appScope)
        initSdksAfterConsent()
    }

    /**
     * Analytics and ads start only once the user has accepted the terms — that
     * acceptance is their legal basis, so nothing may be collected before it.
     * This also keeps both SDKs off the cold-start critical path.
     *
     * Each call is guarded: an SDK failing to initialize on some exotic device
     * must degrade to "no analytics / no ads", never crash the app whose core
     * job is background notification capture.
     */
    private fun initSdksAfterConsent() {
        appScope.launch {
            settingsRepository.observe().map { it.termsAccepted }.first { it }
            withContext(Dispatchers.Main) {
                runCatching { appMetrica.init(this@NotikeepApp) }
                    .onFailure { Log.e(TAG, "AppMetrica init failed", it) }
                // Remote ad config rides on AppMetrica, so it comes right after;
                // failure just means the built-in ad defaults stay in effect.
                runCatching { adsRemoteConfig.init() }
                    .onFailure { Log.e(TAG, "Ads remote config init failed", it) }
                runCatching {
                    adsInitializer.init()
                    // Consent for personalised ads is the same terms acceptance.
                    adsInitializer.setUserConsent(true)
                }.onFailure { Log.e(TAG, "Ads init failed", it) }
            }
        }
    }

    private fun scheduleRetentionCleanup() {
        val request = PeriodicWorkRequestBuilder<RetentionCleanupWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            RetentionCleanupWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    /** Safety net for the "access granted but listener never rebound" limbo. */
    private fun scheduleListenerWatchdog() {
        val request = PeriodicWorkRequestBuilder<ListenerWatchdogWorker>(6, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            ListenerWatchdogWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    private companion object {
        const val TAG = "NotikeepApp"
    }
}
