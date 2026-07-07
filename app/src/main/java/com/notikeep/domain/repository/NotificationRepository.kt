package com.notikeep.domain.repository

import com.notikeep.domain.model.AppArchiveSummary
import com.notikeep.domain.model.NotificationRecord
import kotlinx.coroutines.flow.Flow

/**
 * Persistence boundary for captured notifications. The domain depends on this
 * interface; the data layer provides the Room-backed implementation.
 */
interface NotificationRepository {

    suspend fun save(record: NotificationRecord)

    /** All notifications, newest first, as a reactive stream. */
    fun observeAll(): Flow<List<NotificationRecord>>

    /**
     * Messenger-style archive summary: one row per app, aggregated in the DB.
     * Null bounds mean "no limit" on that side.
     */
    fun observeAppSummaries(from: Long? = null, to: Long? = null): Flow<List<AppArchiveSummary>>

    /** All notifications of one app, newest first. */
    fun observeByPackage(packageName: String): Flow<List<NotificationRecord>>

    /** Marks every notification of the app as read (clears the unread badge). */
    suspend fun markPackageRead(packageName: String)

    suspend fun setFavorite(id: Long, favorite: Boolean)

    /** Full-text search over title and text, newest first, within an optional date range. */
    fun search(query: String, from: Long? = null, to: Long? = null): Flow<List<NotificationRecord>>

    suspend fun delete(id: Long)

    suspend fun clearAll()

    /** Delete everything older than [olderThanMillis]; returns rows removed. */
    suspend fun deleteOlderThan(olderThanMillis: Long): Int
}
