package com.notikeep.presentation.rules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.notikeep.presentation.common.AppIconImage
import com.notikeep.presentation.common.NotikeepSearchBar
import com.notikeep.presentation.common.SystemSettings
import com.notikeep.presentation.theme.RuleIgnoreRed
import com.notikeep.presentation.theme.RuleShadeGreen

@Composable
fun RulesScreen(
    modifier: Modifier = Modifier,
    viewModel: RulesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(modifier.fillMaxSize()) {
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

        if (state.loading) {
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
            }
            return
        }

        LazyColumn(Modifier.fillMaxSize()) {
            items(state.rows, key = { it.packageName }) { row ->
                RuleRow(
                    row = row,
                    onSaveChange = { viewModel.setSave(row, it) },
                    onNotifyChange = { viewModel.setNotify(row, it) },
                )
                HorizontalDivider()
            }
        }
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
 * One row: [icon] [label] [bell] [save switch]. The rule is presented as two
 * decisions — "save?" (main, the switch) and "notify?" (secondary, the bell).
 * The bell fades out while saving is off because it has no effect then.
 */
@Composable
private fun RuleRow(
    row: AppRuleRow,
    onSaveChange: (Boolean) -> Unit,
    onNotifyChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
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
