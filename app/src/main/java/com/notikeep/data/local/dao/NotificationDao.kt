package com.notikeep.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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

    /** IGNORE + the unique dedup index make repeated captures of the same notification a no-op. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: NotificationEntity): Long

    @Update
    suspend fun update(entity: NotificationEntity)

    // --- Dedup-strategy support (see DeduplicateUseCase) ---

    /** Rows with identical title+text from one app since [since]; drives EXACT_TEXT_WINDOW. */
    @Query(
        "SELECT COUNT(*) FROM notifications WHERE packageName = :packageName " +
            "AND title = :title AND text = :text AND postedAt >= :since",
    )
    suspend fun countRecentByText(packageName: String, title: String, text: String, since: Long): Int

    /** Rows with the same title from one app since [since]; drives TITLE_ONLY_WINDOW. */
    @Query(
        "SELECT COUNT(*) FROM notifications WHERE packageName = :packageName " +
            "AND title = :title AND postedAt >= :since",
    )
    suspend fun countRecentByTitle(packageName: String, title: String, since: Long): Int

    /** Newest row carrying [sbnKey], or null; drives BY_KEY (update-in-place). */
    @Query("SELECT * FROM notifications WHERE sbnKey = :sbnKey ORDER BY postedAt DESC LIMIT 1")
    suspend fun findBySbnKey(sbnKey: String): NotificationEntity?

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

    /** Same messenger-style summary, restricted to starred notifications. */
    @Query(
        """
        SELECT packageName, appLabel,
               title AS previewTitle, text AS previewText,
               MAX(postedAt) AS lastPostedAt,
               SUM(CASE WHEN isRead = 0 THEN 1 ELSE 0 END) AS unreadCount,
               COUNT(*) AS totalCount
        FROM notifications
        WHERE isFavorite = 1
        GROUP BY packageName
        ORDER BY lastPostedAt DESC
        """,
    )
    fun observeFavoriteAppSummaries(): Flow<List<AppSummaryRow>>

    @Query("SELECT * FROM notifications WHERE packageName = :packageName ORDER BY postedAt DESC")
    fun observeByPackage(packageName: String): Flow<List<NotificationEntity>>

    @Query(
        "SELECT * FROM notifications WHERE packageName = :packageName AND isFavorite = 1 ORDER BY postedAt DESC",
    )
    fun observeFavoritesByPackage(packageName: String): Flow<List<NotificationEntity>>

    @Query("UPDATE notifications SET isRead = 1 WHERE packageName = :packageName AND isRead = 0")
    suspend fun markPackageRead(packageName: String)

    @Query("UPDATE notifications SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    /**
     * Case-insensitive search using LIKE with wildcards. Matches partial words
     * anywhere in the title or text. Date range narrows results.
     */
    @Query(
        """
        SELECT * FROM notifications
        WHERE (LOWER(title) LIKE '%' || LOWER(:query) || '%' OR LOWER(text) LIKE '%' || LOWER(:query) || '%')
          AND postedAt BETWEEN :from AND :to
        ORDER BY postedAt DESC
        """,
    )
    fun search(query: String, from: Long, to: Long): Flow<List<NotificationEntity>>

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun delete(id: Long)

    /** Deletes every notification of one app; backs long-press delete in the archive. */
    @Query("DELETE FROM notifications WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)

    @Query("DELETE FROM notifications")
    suspend fun clearAll()

    @Query("DELETE FROM notifications WHERE postedAt < :threshold")
    suspend fun deleteOlderThan(threshold: Long): Int

    /** Today's captured/silenced counts for the summary notification. */
    @Query(
        """
        SELECT COUNT(*) AS total,
               SUM(CASE WHEN wasSilenced = 1 THEN 1 ELSE 0 END) AS silenced
        FROM notifications
        WHERE postedAt >= :startOfDay
        """,
    )
    fun observeDailyCounts(startOfDay: Long): Flow<DailyCountsRow>
}

/** Aggregated daily totals; `silenced` is null when there are no rows yet. */
data class DailyCountsRow(val total: Int, val silenced: Int?)
