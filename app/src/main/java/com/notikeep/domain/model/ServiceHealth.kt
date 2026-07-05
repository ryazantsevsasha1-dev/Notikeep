package com.notikeep.domain.model

/**
 * Whether the background capture is actually working. Surfaced to the user so a
 * dead listener is visible immediately instead of a week later (RESEARCH.md #4).
 */
data class ServiceHealth(
    val notificationAccessGranted: Boolean,
    val batteryOptimizationIgnored: Boolean,
) {
    /** Capture is reliable only when both conditions hold. */
    val isHealthy: Boolean
        get() = notificationAccessGranted && batteryOptimizationIgnored
}
