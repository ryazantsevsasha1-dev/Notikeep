package com.notikeep.data.ads

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Hilt entry point for ad singletons in places that can't use constructor
 * injection — plain composables like AdBanner reach them via a Context.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AdsEntryPoint {
    fun adsRemoteConfig(): AdsRemoteConfig

    companion object {
        fun resolve(appContext: Context): AdsEntryPoint =
            EntryPointAccessors.fromApplication(appContext, AdsEntryPoint::class.java)
    }
}
