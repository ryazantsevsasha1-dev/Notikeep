package com.notikeep.domain.usecase

import androidx.paging.PagingData
import com.notikeep.domain.model.NotificationRecord
import com.notikeep.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/** Full-text search over the archive. Blank queries return nothing (not everything). */
class SearchNotificationsUseCase @Inject constructor(
    private val repository: NotificationRepository,
) {
    operator fun invoke(query: String, from: Long? = null, to: Long? = null): Flow<PagingData<NotificationRecord>> {
        val trimmed = query.trim()
        return if (trimmed.isEmpty()) flowOf(PagingData.empty()) else repository.search(trimmed, from, to)
    }
}
