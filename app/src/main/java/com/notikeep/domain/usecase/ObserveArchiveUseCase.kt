package com.notikeep.domain.usecase

import com.notikeep.domain.model.AppArchiveSummary
import com.notikeep.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Streams the archive as per-app summaries (latest preview + unread count),
 * ordered by most recent activity. Aggregation happens in the database, so the
 * archive stays fast no matter how large it grows.
 */
class ObserveArchiveUseCase @Inject constructor(
    private val repository: NotificationRepository,
) {
    operator fun invoke(from: Long? = null, to: Long? = null): Flow<List<AppArchiveSummary>> =
        repository.observeAppSummaries(from, to)
}
