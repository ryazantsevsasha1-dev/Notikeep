package com.notikeep.domain.model

/** Archive notifications grouped under the app that produced them. */
data class AppNotificationGroup(
    val packageName: String,
    val appLabel: String,
    val notifications: List<NotificationRecord>,
) {
    val latestPostedAt: Long
        get() = notifications.firstOrNull()?.postedAt ?: 0L
}
