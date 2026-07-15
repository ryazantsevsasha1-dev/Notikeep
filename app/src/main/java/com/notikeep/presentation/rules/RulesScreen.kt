package com.notikeep.presentation.rules

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notikeep.R
import com.notikeep.domain.model.RuleState
import com.notikeep.presentation.common.AppIconImage
import com.notikeep.presentation.common.NotikeepSearchBar
import com.notikeep.presentation.common.SystemSettings
import com.notikeep.presentation.theme.RuleIgnoreRed
import com.notikeep.presentation.theme.RuleShadeGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(
    modifier: Modifier = Modifier,
    viewModel: RulesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // System back exits selection first, before leaving the screen.
    BackHandler(enabled = state.inSelectionMode) { viewModel.clearSelection() }

    Scaffold(
        modifier = modifier,
        topBar = {
            if (state.inSelectionMode) {
                SelectionTopBar(
                    count = state.selected.size,
                    onSelectAll = viewModel::selectAll,
                    onClose = viewModel::clearSelection,
                )
            }
        },
        bottomBar = {
            if (state.inSelectionMode) {
                SelectionActionsBar(
                    onSave = { viewModel.applyRuleToSelected(RuleState.SHADE_AND_ARCHIVE) },
                    onArchiveOnly = { viewModel.applyRuleToSelected(RuleState.ARCHIVE_ONLY) },
                    onIgnore = { viewModel.applyRuleToSelected(RuleState.IGNORE) },
                    onNotifyOn = { viewModel.setNotifyForSelected(true) },
                    onNotifyOff = { viewModel.setNotifyForSelected(false) },
                )
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (!state.inSelectionMode) {
                NotikeepSearchBar(
                    query = query,
                    onQueryChange = { viewModel.query.value = it },
                    placeholder = stringResource(R.string.rules_search_placeholder),
                )

                if (state.usagePermissionMissing) {
                    UsagePermissionBanner(
                        onGrant = { SystemSettings.openUsageAccess(context) },
                        onRefresh = { viewModel.refreshUsagePermission() },
                    )
                }
            }

            if (state.loading) {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            LazyColumn(Modifier.fillMaxSize()) {
                items(state.rows, key = { it.packageName }) { row ->
                    RuleRow(
                        row = row,
                        selectionMode = state.inSelectionMode,
                        selected = row.packageName in state.selected,
                        onSaveChange = { viewModel.setSave(row, it) },
                        onNotifyChange = { viewModel.setNotify(row, it) },
                        onToggleSelect = { viewModel.toggleSelection(row.packageName) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopBar(count: Int, onSelectAll: () -> Unit, onClose: () -> Unit) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.rules_selection_exit))
            }
        },
        title = { Text(stringResource(R.string.rules_selection_title, count)) },
        actions = {
            IconButton(onClick = onSelectAll) {
                Icon(Icons.Filled.DoneAll, contentDescription = stringResource(R.string.rules_select_all))
            }
        },
    )
}

/** Horizontally scrollable row of bulk actions for the current selection. */
@Composable
private fun SelectionActionsBar(
    onSave: () -> Unit,
    onArchiveOnly: () -> Unit,
    onIgnore: () -> Unit,
    onNotifyOn: () -> Unit,
    onNotifyOff: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AssistChip(onClick = onSave, label = { Text(stringResource(R.string.rules_bulk_save)) })
        AssistChip(onClick = onArchiveOnly, label = { Text(stringResource(R.string.rules_bulk_archive_only)) })
        AssistChip(onClick = onIgnore, label = { Text(stringResource(R.string.rules_bulk_ignore)) })
        AssistChip(onClick = onNotifyOn, label = { Text(stringResource(R.string.rules_bulk_notify_on)) })
        AssistChip(onClick = onNotifyOff, label = { Text(stringResource(R.string.rules_bulk_notify_off)) })
    }
}

/** Soft, dismiss-less banner: without the grant we just fall back to A-Z order. */
@Composable
private fun UsagePermissionBanner(onGrant: () -> Unit, onRefresh: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(
                stringResource(R.string.rules_usage_banner),
                style = MaterialTheme.typography.bodySmall,
            )
            Row {
                TextButton(onClick = onGrant) { Text(stringResource(R.string.rules_usage_grant)) }
                TextButton(onClick = onRefresh) { Text(stringResource(R.string.rules_usage_granted_already)) }
            }
        }
    }
}

/**
 * One row: [icon] [label] [bell] [save switch]. In selection mode the switches
 * are replaced by a checkbox and the whole row toggles selection; long-press
 * enters selection mode from the normal state.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RuleRow(
    row: AppRuleRow,
    selectionMode: Boolean,
    selected: Boolean,
    onSaveChange: (Boolean) -> Unit,
    onNotifyChange: (Boolean) -> Unit,
    onToggleSelect: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (selectionMode) onToggleSelect() },
                onLongClick = onToggleSelect,
            )
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIconImage(row.packageName)
        Text(
            row.label,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
        )

        if (selectionMode) {
            Checkbox(checked = selected, onCheckedChange = { onToggleSelect() })
            return@Row
        }

        IconButton(
            onClick = { onNotifyChange(!row.state.notifies) },
            enabled = row.state.saves,
        ) {
            if (row.state.notifies) {
                Icon(
                    Icons.Filled.Notifications,
                    contentDescription = stringResource(R.string.rules_notify_on),
                    tint = if (row.state.saves) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                )
            } else {
                Icon(
                    Icons.Filled.NotificationsOff,
                    contentDescription = stringResource(R.string.rules_notify_off),
                    tint = MaterialTheme.colorScheme.outline,
                )
            }
        }

        Switch(
            checked = row.state.saves,
            onCheckedChange = onSaveChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = RuleShadeGreen,
                uncheckedTrackColor = RuleIgnoreRed.copy(alpha = 0.55f),
                uncheckedBorderColor = RuleIgnoreRed,
            ),
        )
    }
}
