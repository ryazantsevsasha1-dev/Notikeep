package com.notikeep.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.notikeep.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notikeep.domain.model.DedupStrategy
import com.notikeep.domain.model.ThemeMode
import com.notikeep.domain.model.UserSettings
import com.notikeep.presentation.common.Legal
import com.notikeep.presentation.common.SystemSettings
import com.notikeep.presentation.common.openUrl

private val THEMES = listOf(
    ThemeMode.SYSTEM to R.string.settings_theme_system,
    ThemeMode.LIGHT to R.string.settings_theme_light,
    ThemeMode.DARK to R.string.settings_theme_dark,
)

private val RETENTIONS = listOf(7, 30, 90, UserSettings.RETENTION_FOREVER)

/** Label + short description for each dedup strategy shown in the experiment picker. */
private val DEDUP_STRATEGIES = listOf(
    DedupStrategy.OFF to (R.string.settings_dedup_off to R.string.settings_dedup_off_desc),
    DedupStrategy.EXACT_TEXT_WINDOW to (R.string.settings_dedup_exact to R.string.settings_dedup_exact_desc),
    DedupStrategy.TITLE_ONLY_WINDOW to (R.string.settings_dedup_title to R.string.settings_dedup_title_desc),
    DedupStrategy.BY_KEY to (R.string.settings_dedup_key to R.string.settings_dedup_key_desc),
    DedupStrategy.COMBINED to (R.string.settings_dedup_combined to R.string.settings_dedup_combined_desc),
)

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings = state.settings ?: return
    val context = LocalContext.current

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {

        SectionTitle(stringResource(R.string.settings_service_section))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    stringResource(
                        if (state.health.isHealthy) R.string.settings_service_ok
                        else R.string.settings_service_attention,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (!state.health.notificationAccessGranted) {
                    TextButton(onClick = { SystemSettings.openNotificationAccess(context) }) {
                        Text(stringResource(R.string.settings_enable_access))
                    }
                }
                if (state.health.needsReconnect) {
                    Text(
                        stringResource(R.string.settings_reconnect_hint),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    TextButton(onClick = viewModel::reconnectListener) {
                        Text(stringResource(R.string.settings_reconnect_action))
                    }
                }
                if (!state.health.batteryOptimizationIgnored) {
                    TextButton(onClick = { SystemSettings.requestIgnoreBatteryOptimization(context) }) {
                        Text(stringResource(R.string.settings_battery))
                    }
                }
            }
        }

        SectionTitle(stringResource(R.string.settings_theme_section))
        // FlowRow + chips instead of a segmented row: at large accessibility font
        // scales whole options wrap to the next line, while segmented buttons
        // squeezed their labels letter-by-letter ("Систем/а").
        ChoiceChipRow {
            THEMES.forEach { (mode, labelRes) ->
                ChoiceChip(
                    label = stringResource(labelRes),
                    selected = settings.themeMode == mode,
                    onClick = { viewModel.setTheme(mode) },
                )
            }
        }

        SectionTitle(stringResource(R.string.settings_retention_section))
        ChoiceChipRow {
            RETENTIONS.forEach { days ->
                ChoiceChip(
                    label = if (days == UserSettings.RETENTION_FOREVER) {
                        stringResource(R.string.settings_retention_forever)
                    } else {
                        stringResource(R.string.settings_retention_days, days)
                    },
                    selected = settings.retentionDays == days,
                    onClick = { viewModel.setRetentionDays(days) },
                )
            }
        }

        SectionTitle(stringResource(R.string.settings_privacy_section))
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_daily_summary_title), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.settings_daily_summary_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(
                checked = settings.dailySummaryEnabled,
                onCheckedChange = viewModel::setDailySummaryEnabled,
            )
        }

        Text(
            stringResource(R.string.settings_privacy_policy),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { openUrl(context, Legal.PRIVACY_POLICY_URL) }
                .padding(vertical = 12.dp),
        )

        SectionTitle(stringResource(R.string.settings_dedup_section))
        Text(
            stringResource(R.string.settings_dedup_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        DEDUP_STRATEGIES.forEach { (strategy, labels) ->
            val (titleRes, descRes) = labels
            DedupStrategyCard(
                title = stringResource(titleRes),
                description = stringResource(descRes),
                selected = settings.dedupStrategy == strategy,
                onSelect = { viewModel.setDedupStrategy(strategy) },
            )
            Spacer(Modifier.height(8.dp))
        }

        HorizontalDivider(Modifier.padding(vertical = 12.dp))
        TextButton(onClick = viewModel::clearArchive) {
            Text(stringResource(R.string.settings_clear_archive))
        }
    }
}

/** Chips that wrap whole options to the next line at large font scales. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoiceChipRow(content: @Composable () -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (selected) {
            {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            }
        } else {
            null
        },
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
    )
}

/** Selectable card for one dedup strategy; the chosen one is highlighted. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DedupStrategyCard(
    title: String,
    description: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val border = if (selected) {
        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }
    val colors = if (selected) {
        CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        )
    } else {
        CardDefaults.outlinedCardColors()
    }
    OutlinedCard(
        onClick = onSelect,
        border = border,
        colors = colors,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = onSelect)
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
