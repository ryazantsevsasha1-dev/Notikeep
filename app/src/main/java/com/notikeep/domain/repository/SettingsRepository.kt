package com.notikeep.domain.repository

import com.notikeep.domain.model.DedupStrategy
import com.notikeep.domain.model.ThemeMode
import com.notikeep.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

/** Persistence boundary for user preferences (DataStore-backed in the data layer). */
interface SettingsRepository {

    fun observe(): Flow<UserSettings>

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setRetentionDays(days: Int)

    suspend fun setAnalyticsEnabled(enabled: Boolean)

    suspend fun setOnboardingCompleted(completed: Boolean)

    /** Records the first grant timestamp once; later calls are ignored. */
    suspend fun markFirstAccessGranted(atMillis: Long)

    /** Set once after the top-20 usage-based rules were seeded. */
    suspend fun setDefaultRulesSeeded(seeded: Boolean)

    suspend fun setDailySummaryEnabled(enabled: Boolean)

    suspend fun setDedupStrategy(strategy: DedupStrategy)
}
