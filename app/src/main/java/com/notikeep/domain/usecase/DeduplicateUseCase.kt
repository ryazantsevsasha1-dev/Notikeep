package com.notikeep.domain.usecase

import com.notikeep.domain.model.DedupStrategy
import com.notikeep.domain.model.NotificationRecord
import com.notikeep.domain.repository.NotificationRepository
import javax.inject.Inject

/**
 * Suppresses duplicate/spammy captures before they reach the archive, according
 * to the user-selected [DedupStrategy]. Apps such as VPNs or Telegram re-post or
 * update the same notification repeatedly; without this the archive fills with
 * near-identical rows.
 *
 * Pure enough to unit-test without Android: the clock is injected and the only
 * side effects (insert/update) go through the repository interface.
 */
class DeduplicateUseCase @Inject constructor(
    private val repository: NotificationRepository,
) {
    /** Overridable in tests so the dedup window is deterministic. */
    var now: () -> Long = { System.currentTimeMillis() }

    /**
     * Persists [record] honouring [strategy], or skips it if it's a duplicate.
     * Returns true if something was written (inserted or updated), false if skipped.
     */
    suspend operator fun invoke(record: NotificationRecord, strategy: DedupStrategy): Boolean {
        val since = now() - DedupStrategy.DEDUP_WINDOW_MS
        return when (strategy) {
            DedupStrategy.OFF -> insert(record)

            DedupStrategy.EXACT_TEXT_WINDOW ->
                if (hasRecentSameText(record, since)) false else insert(record)

            DedupStrategy.TITLE_ONLY_WINDOW ->
                if (hasRecentSameTitle(record, since)) false else insert(record)

            DedupStrategy.BY_KEY -> collapseByKey(record)

            DedupStrategy.COMBINED -> {
                // Prefer collapsing an update of the same OS notification; otherwise
                // fall back to dropping a rapid identical repost.
                if (record.sbnKey != null && collapseByKey(record)) true
                else if (hasRecentSameText(record, since)) false
                else insert(record)
            }
        }
    }

    private suspend fun hasRecentSameText(record: NotificationRecord, since: Long): Boolean =
        repository.existsRecentByText(record.packageName, record.title, record.text, since)

    private suspend fun hasRecentSameTitle(record: NotificationRecord, since: Long): Boolean =
        repository.existsRecentByTitle(record.packageName, record.title, since)

    /**
     * If a row with the same [NotificationRecord.sbnKey] exists, replace it in
     * place (keeping its id/favorite/read state) so an update doesn't spawn a new
     * row. Falls back to a plain insert when there's no key or no prior row.
     */
    private suspend fun collapseByKey(record: NotificationRecord): Boolean {
        val key = record.sbnKey ?: return insert(record)
        val existing = repository.findBySbnKey(key) ?: return insert(record)
        repository.update(
            existing.copy(
                title = record.title,
                text = record.text,
                postedAt = record.postedAt,
                wasSilenced = record.wasSilenced,
            ),
        )
        return true
    }

    private suspend fun insert(record: NotificationRecord): Boolean {
        repository.save(record)
        return true
    }
}
