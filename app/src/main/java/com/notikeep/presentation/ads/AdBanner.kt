package com.notikeep.presentation.ads

import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.notikeep.BuildConfig
import com.notikeep.data.ads.AdsEntryPoint
import com.yandex.mobile.ads.banner.BannerAdEventListener
import com.yandex.mobile.ads.banner.BannerAdSize
import com.yandex.mobile.ads.banner.BannerAdView
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData

/**
 * A Yandex (РСЯ) banner rendered inside Compose. Fails silently: if the ad can't
 * load (no network, no fill), the slot collapses instead of crashing or showing
 * an empty box — the app must stay usable offline.
 *
 * The ad unit id comes from BuildConfig so debug uses the Yandex demo banner and
 * release uses the real block id (wired in build.gradle once you have it).
 *
 * The whole placement can be switched off remotely (Varioqub flag
 * `ads_banner_enabled`) — checked once per composition, no update required.
 */
@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    val appContext = LocalContext.current.applicationContext
    val adsConfig = remember { AdsEntryPoint.resolve(appContext).adsRemoteConfig() }
    if (!adsConfig.bannerEnabled) return

    var visible by remember { mutableStateOf(true) }
    if (!visible) return

    val widthDp = LocalConfiguration.current.screenWidthDp

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            val container = FrameLayout(context)
            val banner = BannerAdView(context).apply {
                setAdUnitId(BuildConfig.ADS_BANNER_UNIT_ID)
                setAdSize(BannerAdSize.stickySize(context, widthDp))
                setBannerAdEventListener(object : BannerAdEventListener {
                    override fun onAdLoaded() { /* shown */ }
                    override fun onAdFailedToLoad(error: AdRequestError) { visible = false }
                    override fun onAdClicked() {}
                    override fun onLeftApplication() {}
                    override fun onReturnedToApplication() {}
                    override fun onImpression(impressionData: ImpressionData?) {}
                })
            }
            container.addView(banner)
            banner.loadAd(AdRequest.Builder().build())
            container
        },
        // Without an explicit destroy the SDK keeps the banner (and its WebView)
        // alive after the slot leaves composition — a leak per navigation.
        onRelease = { container ->
            (container.getChildAt(0) as? BannerAdView)?.destroy()
            container.removeAllViews()
        },
    )
}
