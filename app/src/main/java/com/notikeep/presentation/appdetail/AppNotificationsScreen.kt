package com.notikeep.presentation.appdetail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.paging.LoadState
import com.notikeep.R
import com.notikeep.domain.model.NotificationRecord
import com.notikeep.presentation.common.AppIconImage
import com.notikeep.presentation.common.formatTimestamp

/** All notifications of one app, newest first, with favorite toggles. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNotificationsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppNotificationsViewModel = hiltViewModel(),
) {
    val appLabel by viewModel.appLabel.collectAsStateWithLifecycle()
    val items = viewModel.items.collectAsLazyPagingItems()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val selectionMode = selected.isNotEmpty()
    var confirmDelete by remember { mutableStateOf(false) }

    // System back leaves selection mode first, before leaving the screen.
    BackHandler(enabled = selectionMode) { viewModel.clearSelection() }

    Scaffold(
        modifier = modifier,
        topBar = {
            if (selectionMode) {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = viewModel::clearSelection) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.appdetail_selection_exit))
                        }
                    },
                    title = { Text(stringResource(R.string.appdetail_selection_title, selected.size)) },
                    actions = {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.appdetail_delete_selected))
                        }
                    },
                )
            } else {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.appdetail_back))
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AppIconImage(viewModel.packageName, size = 32.dp)
                            Text(
                                appLabel,
                                modifier = Modifier.padding(start = 10.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                )
            }
        },
    ) { padding ->
        // Only the very first page load blocks with a spinner; later pages stream in.
        if (items.loadState.refresh is LoadState.Loading && items.itemCount == 0) {
            Column(
                Modifier.fillMaxSize().padding(padding),
                Arrangement.Center,
                Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            items(count = items.itemCount, key = items.itemKey { it.id }) { index ->
                val record = items[index] ?: return@items
                NotificationRow(
                    record,
                    selectionMode = selectionMode,
                    selected = record.id in selected,
                    onToggleFavorite = { viewModel.toggleFavorite(record) },
                    onToggleSelect = { viewModel.toggleSelection(record.id) },
                )
                HorizontalDivider()
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.appdetail_delete_title)) },
            text = { Text(stringResource(R.string.appdetail_delete_selected_message, selected.size)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSelected()
                    confirmDelete = false
                }) { Text(stringResource(R.string.appdetail_delete_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.appdetail_delete_cancel))
                }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NotificationRow(
    record: NotificationRecord,
    selectionMode: Boolean,
    selected: Boolean,
    onToggleFavorite: () -> Unit,
    onToggleSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (selectionMode) onToggleSelect() },
                onLongClick = onToggleSelect,
            )
            .padding(start = 16.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            if (record.title.isNotBlank()) {
                Text(record.title, style = MaterialTheme.typography.bodyLarge)
            }
            if (record.text.isNotBlank()) {
                Text(
                    record.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(formatTimestamp(record.postedAt), style = MaterialTheme.typography.labelSmall)
        }
        if (selectionMode) {
            Checkbox(checked = selected, onCheckedChange = { onToggleSelect() })
        } else {
            IconButton(onClick = onToggleFavorite) {
                if (record.isFavorite) {
                    Icon(Icons.Filled.Star, contentDescription = stringResource(R.string.appdetail_favorite_remove), tint = MaterialTheme.colorScheme.tertiary)
                } else {
                    Icon(Icons.Outlined.StarBorder, contentDescription = stringResource(R.string.appdetail_favorite_add))
                }
            }
        }
    }
}
