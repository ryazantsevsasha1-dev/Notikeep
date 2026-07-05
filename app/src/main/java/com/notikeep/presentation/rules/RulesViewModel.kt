package com.notikeep.presentation.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notikeep.data.service.InstalledAppsProvider
import com.notikeep.domain.model.RuleState
import com.notikeep.domain.port.Analytics
import com.notikeep.domain.port.AnalyticsEvent
import com.notikeep.domain.repository.RuleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One row: an app and its current effective rule state. */
data class AppRuleRow(
    val packageName: String,
    val label: String,
    val state: RuleState,
)

data class RulesUiState(
    val loading: Boolean = true,
    val rows: List<AppRuleRow> = emptyList(),
)

@HiltViewModel
class RulesViewModel @Inject constructor(
    private val ruleRepository: RuleRepository,
    private val installedApps: InstalledAppsProvider,
    private val analytics: Analytics,
) : ViewModel() {

    private val apps = MutableStateFlow<List<com.notikeep.data.service.InstalledApp>>(emptyList())

    val state = combine(apps, ruleRepository.observeAll()) { installed, rules ->
        val stateByPackage = rules.associate { it.packageName to it.state }
        val rows = installed.map { app ->
            AppRuleRow(
                packageName = app.packageName,
                label = app.label,
                state = stateByPackage[app.packageName] ?: RuleState.SHADE_AND_ARCHIVE,
            )
        }
        RulesUiState(loading = installed.isEmpty(), rows = rows)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RulesUiState())

    init {
        viewModelScope.launch { apps.value = installedApps.listLaunchable() }
    }

    fun setState(row: AppRuleRow, state: RuleState) {
        viewModelScope.launch {
            ruleRepository.setState(row.packageName, row.label, state)
            analytics.track(AnalyticsEvent.RuleChanged(state.name))
        }
    }
}
