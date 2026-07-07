package com.notikeep.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.notikeep.data.local.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

/** One row per app: preview of the latest notification plus unread count. */
data class AppSummaryRow(
    val packageName: String,
    val appLabel: String,
    val previewTitle: String,
    val previewText: String,
    val lastPostedAt: Long,
    val unreadCount: Int,
    val totalCount: Int,
)

@Dao
interface NotificationDao {

    @Insert
    suspend fun insert(entity: NotificationEntity)

    @Query("SELECT * FROM notifications ORDER BY postedAt DESC")
    fun observeAll(): Flow<List<NotificationEntity>>

    /**
     * Archive summary, one row per app, aggregated in SQLite instead of loading
     * every notification into memory. SQLite's bare-column-with-MAX semantics
     * guarantee title/text come from the newest row of each group.
     */
    @Query(
        """
        SELECT packageName, appLabel,
               title AS previewTitle, text AS previewText,
               MAX(postedAt) AS lastPostedAt,
               SUM(CASE WHEN isRead = 0 THEN 1 ELSE 0 END) AS unreadCount,
               COUNT(*) AS totalCount
        FROM notifications
        WHERE postedAt BETWEEN :from AND :to
        GROUP BY packageName
        ORDER BY lastPostedAt DESC
        """,
    )
    fun observeAppSummaries(from: Long, to: Long): Flow<List<AppSummaryRow>>

    @Query("SELECT * FROM notifications WHERE packageName = :packageName ORDER BY postedAt DESC")
    fun observeByPackage(packageName: String): Flow<List<NotificationEntity>>

    @Query("UPDATE notifications SET isRead = 1 WHERE packageName = :packageName AND isRead = 0")
    suspend fun markPackageRead(packageName: String)

    @Query("UPDATE notifications SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    /**
     * FTS match joined back to the base table. Uses the FTS `MATCH` operator with
     * a prefix wildcard so partial words are found. Date range narrows results.
     */
    @Query(
        """
        SELECT n.* FROM notifications AS n
        JOIN notifications_fts AS fts ON n.rowid = fts.rowid
        WHERE notifications_fts MATCH :query AND n.postedAt BETWEEN :from AND :to
        ORDER BY n.postedAt DESC
        """,
    )
    fun search(query: String, from: Long, to: Long): Flow<List<NotificationEntity>>

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM notifications")
    suspend fun clearAll()

    @Query("DELETE FROM notifications WHERE postedAt < :threshold")
    suspend fun deleteOlderThan(threshold: Long): Int
}
