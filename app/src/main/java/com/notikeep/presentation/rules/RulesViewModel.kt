package com.notikeep.presentation.rules

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notikeep.data.service.InstalledApp
import com.notikeep.data.service.InstalledAppsProvider
import com.notikeep.data.service.UsageStatsProvider
import com.notikeep.domain.model.RuleState
import com.notikeep.domain.port.Analytics
import com.notikeep.domain.port.AnalyticsEvent
import com.notikeep.domain.repository.RuleRepository
import com.notikeep.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers

/** One row: an app and its current effective rule state. */
@Immutable
data class AppRuleRow(
    val packageName: String,
    val label: String,
    val state: RuleState,
)

data class RulesUiState(
    val loading: Boolean = true,
    val rows: List<AppRuleRow> = emptyList(),
    /** True when we should show the "grant usage access" banner. */
    val usagePermissionMissing: Boolean = false,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class RulesViewModel @Inject constructor(
    private val ruleRepository: RuleRepository,
    private val installedApps: InstalledAppsProvider,
    private val usageStats: UsageStatsProvider,
    private val settings: SettingsRepository,
    private val analytics: Analytics,
) : ViewModel() {

    private val apps = MutableStateFlow<List<InstalledApp>>(emptyList())
    private val usage = MutableStateFlow<Map<String, Long>>(emptyMap())
    private val usagePermission = MutableStateFlow(usageStats.hasPermission())

    val query = MutableStateFlow("")

    /**
     * Debounced query keeps typing smooth: filtering runs at most every 200 ms
     * and always on a background dispatcher.
     */
    private val debouncedQuery = query
        .debounce(200)
        .onStart { emit("") }

    val state = combine(apps, usage, ruleRepository.observeAll(), debouncedQuery, usagePermission) {
            installed, usageMap, rules, q, hasUsage ->
        val stateByPackage = rules.associate { it.packageName to it.state }
        val filtered = if (q.isBlank()) installed else installed.filter {
            it.label.contains(q.trim(), ignoreCase = true)
        }
        // Most-used first; unused/unknown apps fall back to alphabetical tail.
        val sorted = if (usageMap.isEmpty()) filtered else filtered.sortedWith(
            compareByDescending<InstalledApp> { usageMap[it.packageName] ?: 0L }
                .thenBy { it.label.lowercase() },
        )
        RulesUiState(
            loading = installed.isEmpty(),
            rows = sorted.map { app ->
                AppRuleRow(
                    packageName = app.packageName,
                    label = app.label,
                    state = stateByPackage[app.packageName] ?: RuleState.SHADE_AND_ARCHIVE,
                )
            },
            usagePermissionMissing = !hasUsage,
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RulesUiState())

    init {
        viewModelScope.launch {
            apps.value = installedApps.listLaunchable()
            usage.value = usageStats.foregroundTimeByPackage()
            seedTopAppsIfNeeded()
        }
    }

    /** Re-check after the user returns from the system usage-access screen. */
    fun refreshUsagePermission() {
        val granted = usageStats.hasPermission()
        usagePermission.value = granted
        if (granted && usage.value.isEmpty()) {
            viewModelScope.launch {
                usage.value = usageStats.foregroundTimeByPackage()
                seedTopAppsIfNeeded()
            }
        }
    }

    fun setState(row: AppRuleRow, state: RuleState) {
        viewModelScope.launch {
            ruleRepository.setState(row.packageName, row.label, state)
            analytics.track(AnalyticsEvent.RuleChanged(state.name))
        }
    }

    /**
     * First-run seeding: the 20 most-used apps get an explicit SHADE_AND_ARCHIVE
     * rule. With the current global default this is a no-op visually, but it
     * pins the choice so a future default change won't silently downgrade them.
     */
    private suspend fun seedTopAppsIfNeeded() {
        if (settings.observe().first().defaultRulesSeeded) return
        val usageMap = usage.value
        if (usageMap.isEmpty()) return
        apps.value
            .sortedByDescending { usageMap[it.packageName] ?: 0L }
            .take(TOP_APPS_TO_SEED)
            .forEach { ruleRepository.setState(it.packageName, it.label, RuleState.SHADE_AND_ARCHIVE) }
        settings.setDefaultRulesSeeded(true)
    }

    private companion object {
        const val TOP_APPS_TO_SEED = 20
    }
}
