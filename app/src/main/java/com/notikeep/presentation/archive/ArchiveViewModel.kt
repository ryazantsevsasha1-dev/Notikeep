package com.notikeep.presentation.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.notikeep.domain.model.AppArchiveSummary
import com.notikeep.domain.model.NotificationRecord
import com.notikeep.domain.port.Analytics
import com.notikeep.domain.port.AnalyticsEvent
import com.notikeep.domain.repository.NotificationRepository
import com.notikeep.domain.repository.SettingsRepository
import com.notikeep.domain.usecase.ObserveArchiveUseCase
import com.notikeep.domain.usecase.SearchNotificationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Inclusive date-range filter in epoch millis; null side = unbounded. */
data class DateRange(val from: Long?, val to: Long?) {
    val isActive: Boolean get() = from != null || to != null
}

data class ArchiveUiState(
    val loading: Boolean = true,
    /** Messenger-style rows: one per app. Shown when the query is blank. */
    val summaries: List<AppArchiveSummary> = emptyList(),
    val searching: Boolean = false,
    val dateRange: DateRange = DateRange(null, null),
    /** Epoch millis capture began; powers the honest empty-state message. */
    val captureStartedAt: Long? = null,
) {
    val isEmpty: Boolean get() = !loading && !searching && summaries.isEmpty() && !dateRange.isActive
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class ArchiveViewModel @Inject constructor(
    observeArchive: ObserveArchiveUseCase,
    searchNotifications: SearchNotificationsUseCase,
    settings: SettingsRepository,
    private val repository: NotificationRepository,
    private val analytics: Analytics,
) : ViewModel() {

    val query = MutableStateFlow("")
    private val dateRange = MutableStateFlow(DateRange(null, null))

    private val debouncedQuery = query.debounce(200).onStart { emit("") }

    /** Summaries react to the date filter; the DB does the heavy lifting. */
    private val summaries = dateRange.flatMapLatest { range ->
        observeArchive(range.from, range.to)
    }

    /**
     * Search results as a paged stream (memory-flat for large result sets). Kept
     * out of [state] because PagingData must be collected by the UI directly.
     */
    val searchResults: Flow<PagingData<NotificationRecord>> =
        combine(debouncedQuery, dateRange) { q, range -> q to range }
            .flatMapLatest { (q, range) -> searchNotifications(q, range.from, range.to) }
            .cachedIn(viewModelScope)

    val state: StateFlow<ArchiveUiState> = combine(
        summaries,
        debouncedQuery,
        dateRange,
        settings.observe(),
    ) { groups, q, range, userSettings ->
        ArchiveUiState(
            loading = false,
            summaries = groups,
            searching = q.isNotBlank(),
            dateRange = range,
            captureStartedAt = userSettings.firstAccessGrantedAt,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ArchiveUiState())

    init {
        analytics.track(AnalyticsEvent.ArchiveOpened)
    }

    fun setDateRange(from: Long?, to: Long?) {
        // Extend `to` to the end of the selected day so the range is inclusive.
        dateRange.value = DateRange(from, to?.plus(DAY_MILLIS - 1))
    }

    fun clearDateRange() {
        dateRange.value = DateRange(null, null)
    }

    fun onDetailOpened() = analytics.track(AnalyticsEvent.NotificationDetailOpened)

    fun delete(id: Long) = viewModelScope.launch { repository.delete(id) }

    /** Long-press delete: removes every notification of the app. */
    fun deleteApp(packageName: String) = viewModelScope.launch { repository.deleteByPackage(packageName) }

    private companion object {
        const val DAY_MILLIS = 24 * 60 * 60 * 1000L
    }
}
