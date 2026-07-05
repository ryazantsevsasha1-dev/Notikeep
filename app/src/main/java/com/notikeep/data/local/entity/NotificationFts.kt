package com.notikeep.data.local.entity

import androidx.room.Entity
import androidx.room.Fts4

/**
 * FTS4 shadow table over [NotificationEntity] title+text. `contentEntity` keeps
 * it in sync automatically, so search stays fast even on a large archive.
 */
@Fts4(contentEntity = NotificationEntity::class)
@Entity(tableName = "notifications_fts")
data class NotificationFts(
    val title: String,
    val text: String,
)
