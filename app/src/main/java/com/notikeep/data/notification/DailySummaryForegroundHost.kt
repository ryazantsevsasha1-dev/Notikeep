package com.notikeep.data.notification

import android.app.Notification
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridges the daily-summary lifecycle to whichever always-alive service is currently
 * bound. The [NotikeepListenerService][com.notikeep.data.service.NotikeepListenerService]
 * registers a [Delegate] while connected; the summary is then shown via `startForeground`
 * so it survives the app being swiped away (a plain notification is torn down with the
 * process). When no service is bound the [DailySummaryController] falls back to a plain post.
 */
@Singleton
class DailySummaryForegroundHost @Inject constructor() {

    /** Implemented by the listener service to promote/relinquish its foreground notification. */
    interface Delegate {
        fun showForeground(notification: Notification)
        fun clearForeground()
    }

    private val delegate = AtomicReference<Delegate?>(null)

    val isBound: Boolean get() = delegate.get() != null

    fun register(delegate: Delegate) = this.delegate.set(delegate)

    /** Only unregister if [delegate] is still the current one, to avoid a stale service clearing a newer bind. */
    fun unregister(delegate: Delegate) = this.delegate.compareAndSet(delegate, null)

    fun show(notification: Notification): Boolean {
        val d = delegate.get() ?: return false
        d.showForeground(notification)
        return true
    }

    fun clear(): Boolean {
        val d = delegate.get() ?: return false
        d.clearForeground()
        return true
    }
}
