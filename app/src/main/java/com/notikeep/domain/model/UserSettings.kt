package com.notikeep.domain.model

/** All user-tunable preferences in one immutable snapshot. */
data class UserSettings(
    val themeMode: ThemeMode,
    val retentionDays: Int,
    val analyticsEnabled: Boolean,
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
    }
}
