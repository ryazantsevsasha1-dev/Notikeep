package com.notikeep.presentation.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notikeep.domain.model.AppNotificationGroup
import com.notikeep.domain.port.Analytics
import com.notikeep.domain.port.AnalyticsEvent
import com.notikeep.domain.repository.NotificationRepository
import com.notikeep.domain.repository.SettingsRepository
import com.notikeep.domain.usecase.ObserveArchiveUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArchiveUiState(
    val loading: Boolean = true,
    val groups: List<AppNotificationGroup> = emptyList(),
    /** Epoch millis capture began; powers the honest empty-state message. */
    val captureStartedAt: Long? = null,
) {
    val isEmpty: Boolean get() = !loading && groups.isEmpty()
}

@HiltViewModel
class ArchiveViewModel @Inject constructor(
    observeArchive: ObserveArchiveUseCase,
    settings: SettingsRepository,
    private val repository: NotificationRepository,
    private val analytics: Analytics,
) : ViewModel() {

    val state: kotlinx.coroutines.flow.StateFlow<ArchiveUiState> =
        combine(observeArchive(), settings.observe()) { groups, userSettings ->
            ArchiveUiState(
                loading = false,
                groups = groups,
                captureStartedAt = userSettings.firstAccessGrantedAt,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ArchiveUiState())

    init {
        analytics.track(AnalyticsEvent.ArchiveOpened)
    }

    fun onDetailOpened() = analytics.track(AnalyticsEvent.NotificationDetailOpened)

    fun delete(id: Long) = viewModelScope.launch { repository.delete(id) }
}
