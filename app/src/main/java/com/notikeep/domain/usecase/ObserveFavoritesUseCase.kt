package com.notikeep.domain.usecase

import com.notikeep.domain.model.AppArchiveSummary
import com.notikeep.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Streams starred notifications grouped per app — the Favorites tab mirrors the
 * archive's structure but only over what the user explicitly kept.
 */
class ObserveFavoritesUseCase @Inject constructor(
    private val repository: NotificationRepository,
) {
    operator fun invoke(): Flow<List<AppArchiveSummary>> =
        repository.observeFavoriteAppSummaries()
}
