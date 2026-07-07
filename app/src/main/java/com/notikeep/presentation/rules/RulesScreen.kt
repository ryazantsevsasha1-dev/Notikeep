package com.notikeep.presentation.rules

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notikeep.domain.model.RuleState
import com.notikeep.presentation.common.AppIconImage
import com.notikeep.presentation.common.NotikeepSearchBar
import com.notikeep.presentation.common.SystemSettings
import com.notikeep.presentation.theme.RuleArchiveOrange
import com.notikeep.presentation.theme.RuleIgnoreRed
import com.notikeep.presentation.theme.RuleShadeGreen

/** Left→right: ignore (red), archive (orange), shade (green). Color-only, no captions. */
private val STATES = listOf(
    RuleState.IGNORE to RuleIgnoreRed,
    RuleState.ARCHIVE_ONLY to RuleArchiveOrange,
    RuleState.SHADE_AND_ARCHIVE to RuleShadeGreen,
)

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
            placeholder = "Поиск приложений",
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
                RuleRow(row, onSelect = { viewModel.setState(row, it) })
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
                "Разрешите доступ к статистике использования, чтобы сортировать список по частоте использования приложений.",
                style = MaterialTheme.typography.bodySmall,
            )
            Row {
                TextButton(onClick = onGrant) { Text("Разрешить") }
                TextButton(onClick = onRefresh) { Text("Уже разрешил") }
            }
        }
    }
}

/**
 * Compact row: [icon] [label] [tri-state color indicator]. The heavy segmented
 * buttons are gone; the whole row is ~56dp tall and cheap to compose.
 */
@Composable
private fun RuleRow(row: AppRuleRow, onSelect: (RuleState) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
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
        TriStateIndicator(selected = row.state, onSelect = onSelect)
    }
}

/** Three tappable dots; the selected one is filled with its state color. */
@Composable
private fun TriStateIndicator(selected: RuleState, onSelect: (RuleState) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        STATES.forEach { (ruleState, color) ->
            val isSelected = ruleState == selected
            androidx.compose.foundation.layout.Box(
                Modifier
                    .size(if (isSelected) 22.dp else 16.dp)
                    .clickable { onSelect(ruleState) }
                    .background(
                        color = if (isSelected) color else Color.Transparent,
                        shape = CircleShape,
                    )
                    .border(
                        width = if (isSelected) 0.dp else 2.dp,
                        color = color.copy(alpha = if (isSelected) 1f else 0.45f),
                        shape = CircleShape,
                    ),
            )
        }
    }
}
