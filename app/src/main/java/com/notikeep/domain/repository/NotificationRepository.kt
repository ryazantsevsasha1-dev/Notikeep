package com.notikeep.domain.repository

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

    /** Count of same-app notifications with identical title+text posted at or after [since]. */
    suspend fun countRecentByText(packageName: String, title: String, text: String, since: Long): Int

    /** Count of same-app notifications with the same title posted at or after [since]. */
    suspend fun countRecentByTitle(packageName: String, title: String, since: Long): Int

    /** Newest stored record carrying [sbnKey], or null. */
    suspend fun findBySbnKey(sbnKey: String): NotificationRecord?

    /** Replaces an existing record in place (used to collapse notification updates). */
    suspend fun update(record: NotificationRecord)

    /** All notifications, newest first, as a reactive stream. */
    fun observeAll(): Flow<List<NotificationRecord>>

    /**
     * Messenger-style archive summary: one row per app, aggregated in the DB.
     * Null bounds mean "no limit" on that side.
     */
    fun observeAppSummaries(from: Long? = null, to: Long? = null): Flow<List<AppArchiveSummary>>

    /** Favorites tab: one row per app, starred notifications only. */
    fun observeFavoriteAppSummaries(): Flow<List<AppArchiveSummary>>

    /** Live captured/silenced counts since [startOfDayMillis]; powers the daily summary. */
    fun observeDailyCounts(startOfDayMillis: Long): Flow<DailyCounts>

    /** All notifications of one app, newest first. */
    fun observeByPackage(packageName: String): Flow<List<NotificationRecord>>

    /** Starred notifications of one app, newest first. */
    fun observeFavoritesByPackage(packageName: String): Flow<List<NotificationRecord>>

    /** Marks every notification of the app as read (clears the unread badge). */
    suspend fun markPackageRead(packageName: String)

    suspend fun setFavorite(id: Long, favorite: Boolean)

    /** Full-text search over title and text, newest first, within an optional date range. */
    fun search(query: String, from: Long? = null, to: Long? = null): Flow<List<NotificationRecord>>

    suspend fun delete(id: Long)

    /** Deletes every notification of one app (long-press delete in the archive). */
    suspend fun deleteByPackage(packageName: String)

    suspend fun clearAll()

    /** Delete everything older than [olderThanMillis]; returns rows removed. */
    suspend fun deleteOlderThan(olderThanMillis: Long): Int
}
