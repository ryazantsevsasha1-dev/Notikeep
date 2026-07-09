package com.notikeep.domain.model

/**
 * Whether the background capture is actually working. Surfaced to the user so a
 * dead listener is visible immediately instead of a week later (RESEARCH.md #4).
 */
data class ServiceHealth(
    val notificationAccessGranted: Boolean,
    val batteryOptimizationIgnored: Boolean,
    /** Access can be granted while the system still hasn't bound the listener. */
    val listenerConnected: Boolean = false,
) {
    /** Capture is reliable only when all conditions hold. */
    val isHealthy: Boolean
        get() = notificationAccessGranted && batteryOptimizationIgnored && listenerConnected

    /** The post-update trap: permission looks fine but nothing is being captured. */
    val needsReconnect: Boolean
        get() = notificationAccessGranted && !listenerConnected
}
