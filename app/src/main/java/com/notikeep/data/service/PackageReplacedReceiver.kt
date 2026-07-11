package com.notikeep.data.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Fired by the system right after our APK is updated. This is the moment the
 * listener binding is most likely lost, so we immediately try to restore it —
 * even if the user never opens the app after the update.
 */
@AndroidEntryPoint
class PackageReplacedReceiver : BroadcastReceiver() {

    @Inject lateinit var rebinder: ListenerRebinder

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            rebinder.ensureBound()
        }
    }
}
