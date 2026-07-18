package com.notikeep.data.ads

import android.content.Context
import android.util.Log
import com.notikeep.BuildConfig
import com.yandex.varioqub.appmetricaadapter.AppMetricaAdapter
import com.yandex.varioqub.config.FetchError
import com.yandex.varioqub.config.OnFetchCompleteListener
import com.yandex.varioqub.config.Varioqub
import com.yandex.varioqub.config.VarioqubSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remote "knobs" for ads, backed by Varioqub (Yandex remote config on top of
 * AppMetrica). Lets us turn placements on/off and tune interstitial frequency
 * from the Varioqub web console without shipping an app update.
 *
 * Contract: every getter must be safe to call at any time. Before [init] (or
 * when no client id is configured, or the fetch never lands — first offline
 * launch) the hardcoded defaults below apply. Conservative defaults: banner on,
 * interstitial OFF — fullscreen ads only ever appear because the remote config
 * explicitly enabled them.
 *
 * Flag names in the Varioqub console must match the string keys below.
 */
@Singleton
class AdsRemoteConfig @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val enabled = BuildConfig.VARIOQUB_CLIENT_ID.isNotBlank()

    @Volatile private var initialized = false

    /**
     * Init + async fetch. Must run after AppMetrica activation (the adapter
     * reports experiment data through it), i.e. after terms acceptance.
     * Fetched values apply from the moment they arrive; until then defaults hold.
     */
    fun init() {
        if (!enabled || initialized) return
        initialized = true
        Varioqub.init(
            VarioqubSettings.Builder(BuildConfig.VARIOQUB_CLIENT_ID).build(),
            AppMetricaAdapter(context),
            context,
        )
        Varioqub.fetchConfig(object : OnFetchCompleteListener {
            override fun onSuccess() {
                // Fetched values only take effect after activation.
                Varioqub.activateConfig {
                    if (BuildConfig.DEBUG) Log.d(TAG, "config fetched and activated")
                }
            }

            override fun onError(message: String, error: FetchError) {
                Log.w(TAG, "config fetch failed: $message ($error)")
            }
        })
    }

    /** Whether the bottom banner on the archive screen should be shown at all. */
    val bannerEnabled: Boolean
        get() = getBoolean("ads_banner_enabled", DEFAULT_BANNER_ENABLED)

    /** Master switch for fullscreen interstitials. Off until remotely enabled. */
    val interstitialEnabled: Boolean
        get() = getBoolean("ads_interstitial_enabled", DEFAULT_INTERSTITIAL_ENABLED)

    /** Show an interstitial on every N-th trigger event (detail screen close). */
    val interstitialEveryNTriggers: Int
        get() = getLong("ads_interstitial_every_n", DEFAULT_INTERSTITIAL_EVERY_N)
            .toInt().coerceAtLeast(1)

    /** Minimum seconds between two interstitial shows, whatever the counter says. */
    val interstitialMinIntervalSec: Long
        get() = getLong("ads_interstitial_min_interval_sec", DEFAULT_INTERSTITIAL_MIN_INTERVAL_SEC)
            .coerceAtLeast(0)

    private fun getBoolean(key: String, default: Boolean): Boolean =
        if (initialized) runCatching { Varioqub.getBoolean(key, default) }.getOrDefault(default)
        else default

    private fun getLong(key: String, default: Long): Long =
        if (initialized) runCatching { Varioqub.getLong(key, default) }.getOrDefault(default)
        else default

    private companion object {
        const val TAG = "NotikeepAdsConfig"

        const val DEFAULT_BANNER_ENABLED = true
        const val DEFAULT_INTERSTITIAL_ENABLED = false
        const val DEFAULT_INTERSTITIAL_EVERY_N = 3L
        const val DEFAULT_INTERSTITIAL_MIN_INTERVAL_SEC = 180L
    }
}
