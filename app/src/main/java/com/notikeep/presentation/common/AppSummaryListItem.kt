package com.notikeep.presentation.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notikeep.domain.model.AppArchiveSummary
import com.notikeep.presentation.theme.SilencedRowDark
import com.notikeep.presentation.theme.SilencedRowLight

/**
 * Messenger-style row: icon, app name + preview of the latest notification,
 * time, unread badge. Shared by the Archive and Favorites tabs.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppSummaryListItem(
    summary: AppArchiveSummary,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    // Apps with silenced (hidden-from-shade) notifications get a soft blue wash so the
    // user can see at a glance which rows were quietly captured. Themed for light/dark.
    // Derive dark from the app's own color scheme (not isSystemInDarkTheme): the user's
    // in-app theme override can differ from the system, and mismatching would put a pale
    // light wash under light-on-dark text (or vice-versa), making the row unreadable.
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val highlight = if (summary.hasSilenced) {
        if (dark) SilencedRowDark else SilencedRowLight
    } else {
        Color.Transparent
    }
    // Cache the per-row derived strings so a fling doesn't re-run SimpleDateFormat and
    // rebuild the preview on every recomposition of every visible row.
    val timeLabel = remember(summary.lastPostedAt) { formatTimestamp(summary.lastPostedAt) }
    val preview = remember(summary.previewTitle, summary.previewText) {
        listOf(summary.previewTitle, summary.previewText)
            .filter { it.isNotBlank() }
            .joinToString(": ")
    }
    // Cap the badge text so a chatty app with thousands unread doesn't force a wide
    // remeasure/relayout of the row on every frame.
    val unreadLabel = remember(summary.unreadCount) {
        if (summary.unreadCount > 99) "99+" else summary.unreadCount.toString()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(highlight)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(16.dp, 10.dp),
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
            Text(timeLabel, style = MaterialTheme.typography.labelSmall)
            if (summary.unreadCount > 0) {
                Spacer(Modifier.size(4.dp))
                Badge(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                ) { Text(unreadLabel) }
            }
        }
    }
}
