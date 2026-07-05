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
) {
    companion object {
        const val DEFAULT_RETENTION_DAYS = 30
    }
}
