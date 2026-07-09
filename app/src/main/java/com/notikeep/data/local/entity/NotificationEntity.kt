package com.notikeep.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room row for a captured notification. Indexed for grouping and time queries.
 * The unique index makes inserts idempotent: backfilling active notifications on
 * every listener reconnect must not create duplicates.
 */
@Entity(
    tableName = "notifications",
    indices = [
        Index("packageName"),
        Index("postedAt"),
        Index(value = ["packageName", "postedAt", "title", "text"], unique = true),
    ],
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appLabel: String,
    val title: String,
    val text: String,
    val postedAt: Long,
    val wasSilenced: Boolean,
    /** Cleared when the user opens the per-app screen; drives unread badges. */
    val isRead: Boolean = false,
    /** User-starred notification. */
    val isFavorite: Boolean = false,
)
