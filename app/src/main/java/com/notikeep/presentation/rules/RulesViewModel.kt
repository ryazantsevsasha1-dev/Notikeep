package com.notikeep.presentation.rules

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notikeep.domain.model.InstalledApp
import com.notikeep.domain.model.RuleState
import com.notikeep.domain.port.Analytics
import com.notikeep.domain.port.AnalyticsEvent
import com.notikeep.domain.port.AppCatalog
import com.notikeep.domain.port.AppUsageStats
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
    /** Selected package names; non-empty means the multi-select bar is shown. */
    val selected: Set<String> = emptySet(),
) {
    val inSelectionMode: Boolean get() = selected.isNotEmpty()
}

@OptIn(FlowPreview::class)
@HiltViewModel
class RulesViewModel @Inject constructor(
    private val ruleRepository: RuleRepository,
    private val installedApps: AppCatalog,
    private val usageStats: AppUsageStats,
    private val settings: SettingsRepository,
    private val analytics: Analytics,
) : ViewModel() {

    private val apps = MutableStateFlow<List<InstalledApp>>(emptyList())
    private val usage = MutableStateFlow<Map<String, Long>>(emptyMap())
    private val usagePermission = MutableStateFlow(usageStats.hasPermission())
    private val selected = MutableStateFlow<Set<String>>(emptySet())

    val query = MutableStateFlow("")

    /**
     * Debounced query keeps typing smooth: filtering runs at most every 200 ms
     * and always on a background dispatcher.
     */
    private val debouncedQuery = query
        .debounce(200)
        .onStart { emit("") }

    private val content = combine(apps, usage, ruleRepository.observeAll(), debouncedQuery, usagePermission) {
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

    val state = combine(content, selected) { ui, sel ->
        // Drop any selected packages no longer present (e.g. filtered out or uninstalled).
        val present = ui.rows.mapTo(HashSet()) { it.packageName }
        ui.copy(selected = sel intersect present)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RulesUiState())

    init {
        viewModelScope.launch {
            // Fetch usage BEFORE apps so the very first rendered list is already sorted
            // by usage. Assigning apps last is what triggers the first `content` emission
            // (the combine stays gated while apps is empty), so the list never renders in
            // A-Z order and then visibly re-sorts/jumps under the user.
            usage.value = usageStats.foregroundTimeByPackage()
            apps.value = installedApps.listLaunchable()
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

    /** "Save?" — the main toggle. Turning it back on restores shade visibility too. */
    fun setSave(row: AppRuleRow, save: Boolean) =
        setState(row, RuleState.from(save = save, notify = row.state.notifies || !save))

    /** "Notify?" — the bell. Only meaningful while saving is on. */
    fun setNotify(row: AppRuleRow, notify: Boolean) =
        setState(row, RuleState.from(save = row.state.saves, notify = notify))

    private fun setState(row: AppRuleRow, state: RuleState) {
        if (state == row.state) return
        viewModelScope.launch {
            ruleRepository.setState(row.packageName, row.label, state)
            analytics.track(AnalyticsEvent.RuleChanged(state.name))
        }
    }

    // --- Multi-select ---------------------------------------------------------

    fun toggleSelection(packageName: String) {
        selected.value = selected.value.let { if (packageName in it) it - packageName else it + packageName }
    }

    fun selectAll() {
        selected.value = state.value.rows.mapTo(HashSet()) { it.packageName }
    }

    fun clearSelection() {
        selected.value = emptySet()
    }

    /** Applies one rule to every selected app in a single batch, then exits selection. */
    fun applyRuleToSelected(newState: RuleState) {
        val targets = selectedRows()
        if (targets.isEmpty()) return
        viewModelScope.launch {
            targets.forEach { row ->
                if (row.state != newState) {
                    ruleRepository.setState(row.packageName, row.label, newState)
                }
            }
            analytics.track(AnalyticsEvent.RuleChanged(newState.name))
            clearSelection()
        }
    }

    /** Bulk toggle of the "notify" flag, preserving each app's save state. */
    fun setNotifyForSelected(notify: Boolean) {
        val targets = selectedRows()
        if (targets.isEmpty()) return
        viewModelScope.launch {
            targets.forEach { row ->
                val next = RuleState.from(save = row.state.saves, notify = notify)
                if (next != row.state) ruleRepository.setState(row.packageName, row.label, next)
            }
            clearSelection()
        }
    }

    private fun selectedRows(): List<AppRuleRow> {
        val sel = selected.value
        return state.value.rows.filter { it.packageName in sel }
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
