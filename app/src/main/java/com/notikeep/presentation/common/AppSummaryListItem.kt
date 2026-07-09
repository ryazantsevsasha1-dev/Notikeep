package com.notikeep.presentation.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notikeep.domain.model.AppArchiveSummary

/**
 * Messenger-style row: icon, app name + preview of the latest notification,
 * time, unread badge. Shared by the Archive and Favorites tabs.
 */
@Composable
fun AppSummaryListItem(summary: AppArchiveSummary, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp, 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIconImage(summary.packageName, size = 44.dp)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                summary.appLabel,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val preview = listOf(summary.previewTitle, summary.previewText)
                .filter { it.isNotBlank() }
                .joinToString(": ")
            if (preview.isNotBlank()) {
                Text(
                    preview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(formatTimestamp(summary.lastPostedAt), style = MaterialTheme.typography.labelSmall)
            if (summary.unreadCount > 0) {
                Spacer(Modifier.size(4.dp))
                Badge { Text(summary.unreadCount.toString()) }
            }
        }
    }
}
