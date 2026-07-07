package com.notikeep.presentation.appdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notikeep.domain.model.NotificationRecord
import com.notikeep.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppNotificationsUiState(
    val loading: Boolean = true,
    val appLabel: String = "",
    val notifications: List<NotificationRecord> = emptyList(),
)

/** All notifications of one app, newest first; opening the screen clears its unread badge. */
@HiltViewModel
class AppNotificationsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: NotificationRepository,
) : ViewModel() {

    val packageName: String = checkNotNull(savedStateHandle["packageName"])

    val state: StateFlow<AppNotificationsUiState> =
        repository.observeByPackage(packageName)
            .map { records ->
                AppNotificationsUiState(
                    loading = false,
                    appLabel = records.firstOrNull()?.appLabel ?: packageName,
                    notifications = records,
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppNotificationsUiState())

    init {
        // Entering the screen means the user "saw" these — clear the badge.
        viewModelScope.launch { repository.markPackageRead(packageName) }
    }

    fun toggleFavorite(record: NotificationRecord) = viewModelScope.launch {
        repository.setFavorite(record.id, !record.isFavorite)
    }

    fun delete(id: Long) = viewModelScope.launch { repository.delete(id) }
}
