package com.notikeep

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.notikeep.data.ads.AdsInitializer
import com.notikeep.data.analytics.AppMetricaAnalytics
import com.notikeep.data.icons.AppIconFetcher
import com.notikeep.data.icons.AppIconKeyer
import com.notikeep.data.notification.DailySummaryController
import com.notikeep.data.work.ListenerWatchdogWorker
import com.notikeep.data.work.RetentionCleanupWorker
import com.notikeep.domain.repository.SettingsRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/** Hilt entry point. Also wires WorkManager to Hilt and schedules retention cleanup. */
@HiltAndroidApp
class NotikeepApp : Application(), Configuration.Provider, ImageLoaderFactory {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var appMetrica: AppMetricaAnalytics
    @Inject lateinit var dailySummaryController: DailySummaryController
    @Inject lateinit var adsInitializer: AdsInitializer
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
        appMetrica.init(this)
        adsInitializer.init()
        scheduleRetentionCleanup()
        scheduleListenerWatchdog()
        dailySummaryController.start(appScope)
        syncAdConsent()
    }

    /** Keep the ad SDK's data-collection consent in sync with the privacy toggle. */
    private fun syncAdConsent() {
        appScope.launch {
            settingsRepository.observe()
                .map { it.analyticsEnabled }
                .distinctUntilChanged()
                .collect { adsInitializer.setUserConsent(it) }
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
}
