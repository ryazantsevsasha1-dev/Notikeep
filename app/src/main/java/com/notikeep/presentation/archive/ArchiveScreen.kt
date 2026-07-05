package com.notikeep.presentation.archive

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notikeep.domain.model.NotificationRecord
import com.notikeep.presentation.common.formatTimestamp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    modifier: Modifier = Modifier,
    viewModel: ArchiveViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var detail by remember { mutableStateOf<NotificationRecord?>(null) }

    when {
        state.loading -> LoadingState(modifier)
        state.isEmpty -> EmptyState(state.captureStartedAt, modifier)
        else -> LazyColumn(modifier = modifier.fillMaxSize()) {
            state.groups.forEach { group ->
                item(key = group.packageName) {
                    Text(
                        text = group.appLabel,
                        style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                        modifier = Modifier.fillMaxWidth().padding(16.dp, 12.dp, 16.dp, 4.dp),
                    )
                }
                items(group.notifications, key = { it.id }) { record ->
                    NotificationRow(
                        record = record,
                        onClick = {
                            detail = record
                            viewModel.onDetailOpened()
                        },
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    detail?.let { record ->
        ModalBottomSheet(onDismissRequest = { detail = null }) {
            NotificationDetail(record)
        }
    }
}

@Composable
private fun NotificationRow(record: NotificationRecord, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp, 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            if (record.title.isNotBlank()) {
                Text(record.title, style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
            }
            if (record.text.isNotBlank()) {
                Text(record.text, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
            }
        }
        Text(
            formatTimestamp(record.postedAt),
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun NotificationDetail(record: NotificationRecord) {
    Column(Modifier.fillMaxWidth().padding(24.dp)) {
        Text(record.appLabel, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
        Spacer(Modifier.size(8.dp))
        if (record.title.isNotBlank()) {
            Text(record.title, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.size(4.dp))
        Text(record.text, style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.size(16.dp))
        Text(formatTimestamp(record.postedAt), style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun LoadingState(modifier: Modifier) {
    Column(modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
        CircularProgressIndicator()
    }
}

/**
 * Honest empty-state. Repeats the onboarding promise so a first-day empty list
 * never reads as "the app deleted everything" (RESEARCH.md, anti-pattern #1).
 */
@Composable
private fun EmptyState(captureStartedAt: Long?, modifier: Modifier) {
    Column(
        modifier.fillMaxSize().padding(32.dp),
        Arrangement.Center,
        Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Filled.NotificationsOff, contentDescription = null, modifier = Modifier.size(48.dp))
        Spacer(Modifier.size(16.dp))
        Text(
            "Пока пусто",
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = if (captureStartedAt != null) {
                "Notikeep сохраняет уведомления с момента, когда вы дали доступ. Новые уведомления появятся здесь."
            } else {
                "Включите доступ к уведомлениям, чтобы Notikeep начал их сохранять. Показать уведомления, пришедшие до установки, технически невозможно."
            },
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}
