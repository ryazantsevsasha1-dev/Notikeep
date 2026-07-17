package com.notikeep.presentation.appdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.notikeep.domain.model.NotificationRecord
import com.notikeep.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * All notifications of one app, newest first, page-loaded so memory stays flat
 * even for very chatty apps. Opening the screen clears the app's unread badge.
 */
@HiltViewModel
class AppNotificationsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: NotificationRepository,
) : ViewModel() {

    val packageName: String = checkNotNull(savedStateHandle["packageName"])

    /** When opened from the Favorites tab, only starred notifications are listed. */
    private val favoritesOnly: Boolean = savedStateHandle["favoritesOnly"] ?: false

    val items: Flow<PagingData<NotificationRecord>> =
        repository.pagedByPackage(packageName, favoritesOnly).cachedIn(viewModelScope)

    /** Title label sourced without loading the (potentially huge) notification list. */
    val appLabel: StateFlow<String> =
        repository.observeAppLabel(packageName)
            .map { it ?: packageName }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), packageName)

    /** Selected notification ids; non-empty means the multi-select bar is shown. */
    private val _selected = MutableStateFlow<Set<Long>>(emptySet())
    val selected: StateFlow<Set<Long>> = _selected.asStateFlow()

    init {
        // Entering the screen means the user "saw" these — clear the badge.
        viewModelScope.launch { repository.markPackageRead(packageName) }
    }

    fun toggleFavorite(record: NotificationRecord) = viewModelScope.launch {
        repository.setFavorite(record.id, !record.isFavorite)
    }

    fun delete(id: Long) = viewModelScope.launch { repository.delete(id) }

    // --- Multi-select delete --------------------------------------------------

    fun toggleSelection(id: Long) {
        _selected.value = _selected.value.let { if (id in it) it - id else it + id }
    }

    fun clearSelection() {
        _selected.value = emptySet()
    }

    fun deleteSelected() {
        val ids = _selected.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.deleteByIds(ids)
            clearSelection()
        }
    }
}
