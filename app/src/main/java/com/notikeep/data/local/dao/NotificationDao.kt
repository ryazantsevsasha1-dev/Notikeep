package com.notikeep.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.notikeep.data.local.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Insert
    suspend fun insert(entity: NotificationEntity)

    @Query("SELECT * FROM notifications ORDER BY postedAt DESC")
    fun observeAll(): Flow<List<NotificationEntity>>

    /**
     * FTS match joined back to the base table. Uses the FTS `MATCH` operator with
     * a prefix wildcard so partial words are found.
     */
    @Query(
        """
        SELECT n.* FROM notifications AS n
        JOIN notifications_fts AS fts ON n.rowid = fts.rowid
        WHERE notifications_fts MATCH :query
        ORDER BY n.postedAt DESC
        """,
    )
    fun search(query: String): Flow<List<NotificationEntity>>

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM notifications")
    suspend fun clearAll()

    @Query("DELETE FROM notifications WHERE postedAt < :threshold")
    suspend fun deleteOlderThan(threshold: Long): Int
}
