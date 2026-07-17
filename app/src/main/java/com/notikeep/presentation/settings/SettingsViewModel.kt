package com.notikeep.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notikeep.data.service.ListenerConnectionState
import com.notikeep.data.service.ListenerRebinder
import com.notikeep.data.service.ServiceHealthProvider
import com.notikeep.domain.model.DedupStrategy
import com.notikeep.domain.model.ServiceHealth
import com.notikeep.domain.model.ThemeMode
import com.notikeep.domain.model.UserSettings
import com.notikeep.domain.repository.NotificationRepository
import com.notikeep.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: UserSettings? = null,
    val health: ServiceHealth = ServiceHealth(false, false),
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val notifications: NotificationRepository,
    private val healthProvider: ServiceHealthProvider,
    private val rebinder: ListenerRebinder,
    connectionState: ListenerConnectionState,
) : ViewModel() {

    // Combining with the live connection flag re-reads health whenever the
    // listener (re)binds, so the status card updates without leaving the screen.
    val state = combine(settings.observe(), connectionState.connected) { userSettings, _ ->
        SettingsUiState(settings = userSettings, health = healthProvider.current())
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    /** Manual escape hatch for the "granted but not bound" limbo. */
    fun reconnectListener() = rebinder.ensureBound()

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { settings.setThemeMode(mode) }

    fun setRetentionDays(days: Int) = viewModelScope.launch { settings.setRetentionDays(days) }

    fun setDailySummaryEnabled(enabled: Boolean) =
        viewModelScope.launch { settings.setDailySummaryEnabled(enabled) }

    fun setDedupStrategy(strategy: DedupStrategy) =
        viewModelScope.launch { settings.setDedupStrategy(strategy) }

    fun clearArchive() = viewModelScope.launch { notifications.clearAll() }
}
