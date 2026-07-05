package com.notikeep.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notikeep.domain.model.NotificationRecord
import com.notikeep.domain.port.Analytics
import com.notikeep.domain.port.AnalyticsEvent
import com.notikeep.domain.usecase.SearchNotificationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<NotificationRecord> = emptyList(),
) {
    val showNoResults: Boolean get() = query.isNotBlank() && results.isEmpty()
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val search: SearchNotificationsUseCase,
    private val analytics: Analytics,
) : ViewModel() {

    private val query = MutableStateFlow("")

    private val results = query
        .debounce(200)
        .flatMapLatest { search(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val state = kotlinx.coroutines.flow.combine(query, results) { q, r ->
        SearchUiState(query = q, results = r)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun onQueryChange(value: String) {
        val wasBlank = query.value.isBlank()
        query.value = value
        if (wasBlank && value.isNotBlank()) analytics.track(AnalyticsEvent.SearchUsed)
    }
}
