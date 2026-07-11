package com.notikeep.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.notikeep.data.service.ListenerRebinder
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Periodic safety net: if notification access is granted but the listener is not
 * actually bound (post-update limbo, OEM battery killers), force a rebind. The
 * rebinder itself is a no-op when everything is healthy.
 */
@HiltWorker
class ListenerWatchdogWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val rebinder: ListenerRebinder,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        rebinder.ensureBound()
        return Result.success()
    }

    companion object {
        const val UNIQUE_NAME = "notikeep_listener_watchdog"
    }
}
