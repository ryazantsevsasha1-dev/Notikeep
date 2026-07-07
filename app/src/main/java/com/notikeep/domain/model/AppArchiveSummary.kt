package com.notikeep.domain.model

/**
 * Messenger-style archive row: one app with a preview of its latest
 * notification and the number of unread ones.
 */
data class AppArchiveSummary(
    val packageName: String,
    val appLabel: String,
    val previewTitle: String,
    val previewText: String,
    val lastPostedAt: Long,
    val unreadCount: Int,
    val totalCount: Int,
)
