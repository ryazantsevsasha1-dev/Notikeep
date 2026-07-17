package com.notikeep.domain.model

/** All user-tunable preferences in one immutable snapshot. */
data class UserSettings(
    val themeMode: ThemeMode,
    val retentionDays: Int,
    /**
     * Analytics and ads are part of the product and always on once the user has
     * accepted the terms on first launch (their legal basis is that acceptance,
     * not a toggle). Kept as a field so downstream consumers stay unchanged, but
     * there is no setter and no UI switch — it is constant `true`.
     */
    val analyticsEnabled: Boolean = true,
    /** True once the user accepted the terms/privacy policy on the first-run screen. */
    val termsAccepted: Boolean = false,
    /** True once the onboarding has been completed at least once. */
    val onboardingCompleted: Boolean,
    /** Epoch millis when notification access was first granted, or null. */
    val firstAccessGrantedAt: Long?,
    /** True once the top-20 most-used apps were auto-assigned the default rule. */
    val defaultRulesSeeded: Boolean = false,
    /** Post a running notification summarising today's captured/silenced counts. */
    val dailySummaryEnabled: Boolean = true,
    /** Which duplicate-suppression strategy to apply on capture. */
    val dedupStrategy: DedupStrategy = DedupStrategy.DEFAULT,
) {
    companion object {
        const val DEFAULT_RETENTION_DAYS = 30

        /** Sentinel for "keep notifications forever" — retention cleanup is skipped. */
        const val RETENTION_FOREVER = 0
    }
}
