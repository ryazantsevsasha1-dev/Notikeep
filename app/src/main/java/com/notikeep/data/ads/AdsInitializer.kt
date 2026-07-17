package com.notikeep.data.ads

import android.content.Context
import com.yandex.mobile.ads.common.MobileAds
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-time init of the Yandex Mobile Ads (РСЯ) SDK. Kept behind an interface-free
 * singleton so [com.notikeep.NotikeepApp] can call it without knowing SDK details.
 *
 * User consent for ad personalisation is tied to the same privacy toggle as
 * analytics: when the user opts out, we tell the SDK not to gather personalised data.
 */
@Singleton
class AdsInitializer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    @Volatile private var initialized = false

    fun init() {
        if (initialized) return
        initialized = true
        MobileAds.initialize(context) { /* SDK ready; banners can load lazily */ }
    }

    /** Reflects the user's privacy choice into the ad SDK's data-collection consent. */
    fun setUserConsent(granted: Boolean) {
        MobileAds.setUserConsent(granted)
    }
}
