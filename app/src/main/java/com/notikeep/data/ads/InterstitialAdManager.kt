package com.notikeep.data.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.notikeep.BuildConfig
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequestConfiguration
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.interstitial.InterstitialAd
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fullscreen (interstitial) РСЯ ads, shown only at natural breaks — currently
 * when the user leaves a per-app notification list back to the archive.
 *
 * Whether and how often anything appears is governed by [AdsRemoteConfig]
 * (default: disabled), with two caps combined:
 *  - a counter: at most every N-th natural break,
 *  - a cooldown: at least `interstitialMinIntervalSec` between shows.
 *
 * Ads are loaded lazily — the first request goes out only once the feature is
 * remotely enabled and a break actually happens — and everything fails silently:
 * no fill / no network / show error just means no ad this time.
 */
@Singleton
class InterstitialAdManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val config: AdsRemoteConfig,
) {
    private var loader: InterstitialAdLoader? = null
    private var loadedAd: InterstitialAd? = null
    private var loading = false
    private var breaksSinceLastShow = 0
    private var lastShownAtMs = 0L
    private var showing = false

    /**
     * Call on a natural UI break (main thread). Decides whether this break
     * produces an ad; if not, may kick off a background load for a future one.
     */
    fun onNaturalBreak(activity: Activity) {
        if (!config.interstitialEnabled || showing) return
        breaksSinceLastShow++

        val counterReady = breaksSinceLastShow >= config.interstitialEveryNTriggers
        val cooldownOver = lastShownAtMs == 0L ||
            SystemClock.elapsedRealtime() - lastShownAtMs >= config.interstitialMinIntervalSec * 1000

        val ad = loadedAd
        if (counterReady && cooldownOver && ad != null) {
            show(ad, activity)
        } else if (ad == null) {
            // Load ahead of time so the ad is ready when the caps next allow it.
            loadAd()
        }
    }

    private fun show(ad: InterstitialAd, activity: Activity) {
        showing = true
        loadedAd = null
        ad.setAdEventListener(object : InterstitialAdEventListener {
            override fun onAdShown() {
                lastShownAtMs = SystemClock.elapsedRealtime()
                breaksSinceLastShow = 0
            }

            override fun onAdFailedToShow(adError: AdError) {
                Log.w(TAG, "interstitial failed to show: ${adError.description}")
                release(ad)
            }

            override fun onAdDismissed() = release(ad)
            override fun onAdClicked() {}
            override fun onAdImpression(impressionData: ImpressionData?) {}
        })
        ad.show(activity)
    }

    private fun release(ad: InterstitialAd) {
        ad.setAdEventListener(null)
        showing = false
    }

    private fun loadAd() {
        if (loading) return
        loading = true
        val loader = loader ?: InterstitialAdLoader(context).also { loader = it }
        loader.setAdLoadListener(object : InterstitialAdLoadListener {
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                loading = false
                loadedAd = interstitialAd
            }

            override fun onAdFailedToLoad(error: AdRequestError) {
                // No retry loop: the next natural break will try again.
                loading = false
                if (BuildConfig.DEBUG) Log.d(TAG, "interstitial load failed: ${error.description}")
            }
        })
        loader.loadAd(
            AdRequestConfiguration.Builder(BuildConfig.ADS_INTERSTITIAL_UNIT_ID).build(),
        )
    }

    private companion object {
        const val TAG = "NotikeepInterstitial"
    }
}
