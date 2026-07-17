package com.notikeep.domain.repository

import androidx.paging.PagingData
import com.notikeep.domain.model.AppArchiveSummary
import com.notikeep.domain.model.DailyCounts
import com.notikeep.domain.model.NotificationRecord
import kotlinx.coroutines.flow.Flow

/**
 * Persistence boundary for captured notifications. The domain depends on this
 * interface; the data layer provides the Room-backed implementation.
 */
interface NotificationRepository {

    suspend fun save(record: NotificationRecord)

    /** Whether a same-app notification with identical title+text exists at or after [since]. */
    suspend fun existsRecentByText(packageName: String, title: String, text: String, since: Long): Boolean

    /** Whether a same-app notification with the same title exists at or after [since]. */
    suspend fun existsRecentByTitle(packageName: String, title: String, since: Long): Boolean

    /** Newest stored record carrying [sbnKey], or null. */
    suspend fun findBySbnKey(sbnKey: String): NotificationRecord?

    /** Replaces an existing record in place (used to collapse notification updates). */
    suspend fun update(record: NotificationRecord)

    /**
     * Messenger-style archive summary: one row per app, aggregated in the DB.
     * Null bounds mean "no limit" on that side.
     */
    fun observeAppSummaries(from: Long? = null, to: Long? = null): Flow<List<AppArchiveSummary>>

    /** Favorites tab: one row per app, starred notifications only. */
    fun observeFavoriteAppSummaries(): Flow<List<AppArchiveSummary>>

    /** Live captured/silenced counts since [startOfDayMillis]; powers the daily summary. */
    fun observeDailyCounts(startOfDayMillis: Long): Flow<DailyCounts>

    /** Paged notifications of one app, newest first (all, or starred only). */
    fun pagedByPackage(packageName: String, favoritesOnly: Boolean): Flow<PagingData<NotificationRecord>>

    /** An app's display label as a stream, without loading its notifications. */
    fun observeAppLabel(packageName: String): Flow<String?>

    /** Marks every notification of the app as read (clears the unread badge). */
    suspend fun markPackageRead(packageName: String)

    suspend fun setFavorite(id: Long, favorite: Boolean)

    /** Paged full-text search over title and text, newest first, within an optional date range. */
    fun search(query: String, from: Long? = null, to: Long? = null): Flow<PagingData<NotificationRecord>>

    suspend fun delete(id: Long)

    /** Deletes a specific set of notifications (multi-select delete). */
    suspend fun deleteByIds(ids: List<Long>)

    /** Deletes every notification of one app (long-press delete in the archive). */
    suspend fun deleteByPackage(packageName: String)

    suspend fun clearAll()

    /** Delete everything older than [olderThanMillis]; returns rows removed. */
    suspend fun deleteOlderThan(olderThanMillis: Long): Int
}
