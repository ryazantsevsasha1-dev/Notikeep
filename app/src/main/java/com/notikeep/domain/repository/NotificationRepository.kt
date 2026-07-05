package com.notikeep.domain.repository

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

    /** Full-text search over title and text, newest first. */
    fun search(query: String): Flow<List<NotificationRecord>>

    suspend fun delete(id: Long)

    suspend fun clearAll()

    /** Delete everything older than [olderThanMillis]; returns rows removed. */
    suspend fun deleteOlderThan(olderThanMillis: Long): Int
}
