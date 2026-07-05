package com.notikeep.domain.port

/**
 * Behavioural analytics port. Implementations must stay anonymous — never pass
 * notification content here, only UX events (RESEARCH.md, anti-pattern #3).
 *
 * The domain depends on this abstraction; swapping the local implementation for
 * a RuStore-compatible one later is a one-line DI change, not a rewrite.
 */
interface Analytics {
    fun track(event: AnalyticsEvent)
}

/**
 * The closed set of UX events Notikeep records. A sealed hierarchy keeps events
 * type-safe and self-documenting instead of raw strings scattered across the app.
 */
sealed interface AnalyticsEvent {
    val name: String

    data class OnboardingStepViewed(val step: Int) : AnalyticsEvent {
        override val name = "onboarding_step_viewed"
    }

    data object NotificationAccessGranted : AnalyticsEvent {
        override val name = "notification_access_granted"
    }

    data object BatteryOptimizationDisabled : AnalyticsEvent {
        override val name = "battery_opt_disabled"
    }

    data object ArchiveOpened : AnalyticsEvent {
        override val name = "archive_opened"
    }

    data object SearchUsed : AnalyticsEvent {
        override val name = "search_used"
    }

    data object NotificationDetailOpened : AnalyticsEvent {
        override val name = "notification_detail_opened"
    }

    /** Only the target state is recorded, never which app it was. */
    data class RuleChanged(val state: String) : AnalyticsEvent {
        override val name = "rule_changed"
    }

    data class ServiceHealthChanged(val healthy: Boolean) : AnalyticsEvent {
        override val name = "service_health"
    }
}
