package com.notikeep.data.local

import com.notikeep.data.local.dao.AppSummaryRow
import com.notikeep.data.local.entity.AppRuleEntity
import com.notikeep.data.local.entity.NotificationEntity
import com.notikeep.domain.model.AppArchiveSummary
import com.notikeep.domain.model.AppRule
import com.notikeep.domain.model.NotificationRecord

fun NotificationEntity.toDomain() = NotificationRecord(
    id = id,
    packageName = packageName,
    appLabel = appLabel,
    title = title,
    text = text,
    postedAt = postedAt,
    wasSilenced = wasSilenced,
    isRead = isRead,
    isFavorite = isFavorite,
    sbnKey = sbnKey,
)

/** id defaults to 0 so Room autogenerates it on insert. */
fun NotificationRecord.toEntity() = NotificationEntity(
    id = id,
    packageName = packageName,
    appLabel = appLabel,
    title = title,
    text = text,
    postedAt = postedAt,
    wasSilenced = wasSilenced,
    isRead = isRead,
    isFavorite = isFavorite,
    sbnKey = sbnKey,
)

fun AppSummaryRow.toDomain() = AppArchiveSummary(
    packageName = packageName,
    appLabel = appLabel,
    previewTitle = previewTitle,
    previewText = previewText,
    lastPostedAt = lastPostedAt,
    unreadCount = unreadCount,
    totalCount = totalCount,
)

fun AppRuleEntity.toDomain() = AppRule(
    packageName = packageName,
    appLabel = appLabel,
    state = state,
)
