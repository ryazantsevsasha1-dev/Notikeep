package com.notikeep.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.notikeep.domain.model.DedupStrategy
import com.notikeep.domain.model.ThemeMode
import com.notikeep.domain.model.UserSettings
import com.notikeep.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override fun observe(): Flow<UserSettings> = dataStore.data.map { prefs ->
        UserSettings(
            themeMode = prefs[Keys.THEME]?.let(ThemeMode::valueOf) ?: ThemeMode.SYSTEM,
            retentionDays = prefs[Keys.RETENTION] ?: UserSettings.DEFAULT_RETENTION_DAYS,
            analyticsEnabled = prefs[Keys.ANALYTICS] ?: true,
            onboardingCompleted = prefs[Keys.ONBOARDING_DONE] ?: false,
            firstAccessGrantedAt = prefs[Keys.FIRST_ACCESS],
            defaultRulesSeeded = prefs[Keys.DEFAULT_RULES_SEEDED] ?: false,
            dailySummaryEnabled = prefs[Keys.DAILY_SUMMARY] ?: true,
            dedupStrategy = DedupStrategy.fromNameOrDefault(prefs[Keys.DEDUP_STRATEGY]),
        )
    }

    override suspend fun setThemeMode(mode: ThemeMode) =
        edit { it[Keys.THEME] = mode.name }

    override suspend fun setRetentionDays(days: Int) =
        edit { it[Keys.RETENTION] = days }

    override suspend fun setAnalyticsEnabled(enabled: Boolean) =
        edit { it[Keys.ANALYTICS] = enabled }

    override suspend fun setOnboardingCompleted(completed: Boolean) =
        edit { it[Keys.ONBOARDING_DONE] = completed }

    override suspend fun markFirstAccessGranted(atMillis: Long) = edit { prefs ->
        if (prefs[Keys.FIRST_ACCESS] == null) prefs[Keys.FIRST_ACCESS] = atMillis
    }

    override suspend fun setDefaultRulesSeeded(seeded: Boolean) =
        edit { it[Keys.DEFAULT_RULES_SEEDED] = seeded }

    override suspend fun setDailySummaryEnabled(enabled: Boolean) =
        edit { it[Keys.DAILY_SUMMARY] = enabled }

    override suspend fun setDedupStrategy(strategy: DedupStrategy) =
        edit { it[Keys.DEDUP_STRATEGY] = strategy.name }

    private suspend inline fun edit(crossinline block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        dataStore.edit { block(it) }
    }

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val RETENTION = intPreferencesKey("retention_days")
        val ANALYTICS = booleanPreferencesKey("analytics_enabled")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_completed")
        val FIRST_ACCESS = longPreferencesKey("first_access_granted_at")
        val DEFAULT_RULES_SEEDED = booleanPreferencesKey("default_rules_seeded")
        val DAILY_SUMMARY = booleanPreferencesKey("daily_summary_enabled")
        val DEDUP_STRATEGY = stringPreferencesKey("dedup_strategy")
    }
}
