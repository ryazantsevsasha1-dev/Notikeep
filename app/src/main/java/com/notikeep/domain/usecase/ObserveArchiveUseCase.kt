package com.notikeep.domain.usecase

import com.notikeep.domain.model.AppNotificationGroup
import com.notikeep.domain.model.NotificationRecord
import com.notikeep.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Streams the archive grouped by app, groups ordered by most recent activity. */
class ObserveArchiveUseCase @Inject constructor(
    private val repository: NotificationRepository,
) {
    operator fun invoke(): Flow<List<AppNotificationGroup>> =
        repository.observeAll().map { records -> groupByApp(records) }

    private fun groupByApp(records: List<NotificationRecord>): List<AppNotificationGroup> =
        records
            .groupBy { it.packageName }
            .map { (pkg, items) ->
                AppNotificationGroup(
                    packageName = pkg,
                    appLabel = items.first().appLabel,
                    notifications = items,
                )
            }
            .sortedByDescending { it.latestPostedAt }
}
