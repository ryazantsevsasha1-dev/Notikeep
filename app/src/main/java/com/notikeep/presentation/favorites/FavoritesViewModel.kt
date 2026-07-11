package com.notikeep.presentation.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notikeep.domain.model.AppArchiveSummary
import com.notikeep.domain.repository.NotificationRepository
import com.notikeep.domain.usecase.ObserveFavoritesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoritesUiState(
    val loading: Boolean = true,
    /** One row per app, starred notifications only. */
    val summaries: List<AppArchiveSummary> = emptyList(),
) {
    val isEmpty: Boolean get() = !loading && summaries.isEmpty()
}

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    observeFavorites: ObserveFavoritesUseCase,
    private val repository: NotificationRepository,
) : ViewModel() {

    val state: StateFlow<FavoritesUiState> = observeFavorites()
        .map { FavoritesUiState(loading = false, summaries = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FavoritesUiState())

    /** Long-press delete: removes every notification of the app. */
    fun deleteApp(packageName: String) = viewModelScope.launch { repository.deleteByPackage(packageName) }
}
