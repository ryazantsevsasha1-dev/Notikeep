package com.notikeep.presentation.archive

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.notikeep.R
import com.notikeep.domain.model.NotificationRecord
import com.notikeep.presentation.common.AppIconImage
import com.notikeep.presentation.common.AppSummaryListItem
import com.notikeep.presentation.common.NotikeepSearchBar
import com.notikeep.presentation.common.formatTimestamp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    onOpenApp: (packageName: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArchiveViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    var detail by remember { mutableStateOf<NotificationRecord?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    Column(modifier.fillMaxSize()) {
        NotikeepSearchBar(
            query = query,
            onQueryChange = { viewModel.query.value = it },
            placeholder = stringResource(R.string.archive_search_placeholder),
            trailingExtra = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Filled.DateRange, contentDescription = stringResource(R.string.archive_date_filter))
                }
            },
        )

        if (state.dateRange.isActive) {
            AssistChip(
                onClick = { viewModel.clearDateRange() },
                label = {
                    val from = state.dateRange.from?.let { formatTimestamp(it) } ?: "…"
                    val to = state.dateRange.to?.let { formatTimestamp(it) } ?: "…"
                    Text("$from — $to")
                },
                trailingIcon = { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.archive_date_clear), Modifier.size(16.dp)) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        when {
            state.loading -> LoadingState(Modifier.weight(1f))
            state.searching -> SearchResults(
                results = state.searchResults,
                onClick = { detail = it; viewModel.onDetailOpened() },
                modifier = Modifier.weight(1f),
            )
            state.isEmpty -> EmptyState(state.captureStartedAt, Modifier.weight(1f))
            else -> LazyColumn(Modifier.weight(1f).fillMaxSize()) {
                items(state.summaries, key = { it.packageName }) { summary ->
                    AppSummaryListItem(summary, onClick = { onOpenApp(summary.packageName) })
                    HorizontalDivider()
                }
            }
        }
    }

    if (showDatePicker) {
        DateRangeDialog(
            onConfirm = { from, to ->
                viewModel.setDateRange(from, to)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }

    detail?.let { record ->
        ModalBottomSheet(onDismissRequest = { detail = null }) {
            NotificationDetail(record)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangeDialog(onConfirm: (Long?, Long?) -> Unit, onDismiss: () -> Unit) {
    val pickerState = rememberDateRangePickerState()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(pickerState.selectedStartDateMillis, pickerState.selectedEndDateMillis)
                },
            ) { Text(stringResource(R.string.archive_date_apply)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.archive_date_cancel)) } },
    ) {
        DateRangePicker(state = pickerState, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SearchResults(
    results: List<NotificationRecord>,
    onClick: (NotificationRecord) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (results.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.archive_search_empty), style = MaterialTheme.typography.bodyMedium)
        }
        return
    }
    LazyColumn(modifier.fillMaxSize()) {
        items(results, key = { it.id }) { record ->
            SearchResultRow(record, onClick = { onClick(record) })
            HorizontalDivider()
        }
    }
}

@Composable
private fun SearchResultRow(record: NotificationRecord, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp, 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIconImage(record.packageName, size = 36.dp)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            if (record.title.isNotBlank()) {
                Text(record.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (record.text.isNotBlank()) {
                Text(
                    record.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(formatTimestamp(record.postedAt), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun NotificationDetail(record: NotificationRecord) {
    Column(Modifier.fillMaxWidth().padding(24.dp)) {
        Text(record.appLabel, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.size(8.dp))
        if (record.title.isNotBlank()) {
            Text(record.title, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.size(4.dp))
        Text(record.text, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.size(16.dp))
        Text(formatTimestamp(record.postedAt), style = MaterialTheme.typography.labelSmall)
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
            stringResource(R.string.archive_empty_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = stringResource(
                if (captureStartedAt != null) R.string.archive_empty_capturing
                else R.string.archive_empty_no_access,
            ),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}
