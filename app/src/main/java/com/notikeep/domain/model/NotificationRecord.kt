package com.notikeep.domain.model

/**
 * A single captured notification, independent of how it is stored or displayed.
 *
 * This is a pure domain model: no Android or Room types leak in here so the
 * business rules stay testable without an emulator.
 */
data class NotificationRecord(
    val id: Long,
    val packageName: String,
    val appLabel: String,
    val title: String,
    val text: String,
    val postedAt: Long,
    /** True when this notification was silenced from the shade but still archived. */
    val wasSilenced: Boolean,
    /** False until the user opens the app's notification list. */
    val isRead: Boolean = false,
    /** User-starred notification. */
    val isFavorite: Boolean = false,
    /** The OS notification key (sbn.key); lets dedup collapse updates of one notification. */
    val sbnKey: String? = null,
)
