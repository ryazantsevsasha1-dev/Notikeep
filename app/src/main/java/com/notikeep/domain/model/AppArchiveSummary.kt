package com.notikeep.domain.model

import androidx.compose.runtime.Immutable

/**
 * Messenger-style archive row: one app with a preview of its latest
 * notification and the number of unread ones.
 */
@Immutable
data class AppArchiveSummary(
    val packageName: String,
    val appLabel: String,
    val previewTitle: String,
    val previewText: String,
    val lastPostedAt: Long,
    val unreadCount: Int,
    val totalCount: Int,
    /** Count of this app's silenced (hidden-from-shade) notifications; drives the archive highlight. */
    val silencedCount: Int = 0,
) {
    /** True when this app has at least one notification that was hidden from the shade. */
    val hasSilenced: Boolean get() = silencedCount > 0
}
