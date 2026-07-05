package com.notikeep.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notikeep.data.service.ServiceHealthProvider
import com.notikeep.domain.model.ServiceHealth
import com.notikeep.domain.model.ThemeMode
import com.notikeep.domain.model.UserSettings
import com.notikeep.domain.repository.NotificationRepository
import com.notikeep.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
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
) : ViewModel() {

    val state = settings.observe()
        .map { SettingsUiState(settings = it, health = healthProvider.current()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { settings.setThemeMode(mode) }

    fun setRetentionDays(days: Int) = viewModelScope.launch { settings.setRetentionDays(days) }

    fun setAnalyticsEnabled(enabled: Boolean) =
        viewModelScope.launch { settings.setAnalyticsEnabled(enabled) }

    fun clearArchive() = viewModelScope.launch { notifications.clearAll() }
}
