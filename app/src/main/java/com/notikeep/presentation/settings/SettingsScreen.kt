package com.notikeep.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import com.notikeep.domain.model.ThemeMode
import com.notikeep.presentation.common.SystemSettings

private val THEMES = listOf(
    ThemeMode.SYSTEM to R.string.settings_theme_system,
    ThemeMode.LIGHT to R.string.settings_theme_light,
    ThemeMode.DARK to R.string.settings_theme_dark,
)

private val RETENTIONS = listOf(7, 30, 90)

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
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            THEMES.forEachIndexed { index, (mode, labelRes) ->
                SegmentedButton(
                    selected = settings.themeMode == mode,
                    onClick = { viewModel.setTheme(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index, THEMES.size),
                ) { Text(stringResource(labelRes)) }
            }
        }

        SectionTitle(stringResource(R.string.settings_retention_section))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            RETENTIONS.forEachIndexed { index, days ->
                SegmentedButton(
                    selected = settings.retentionDays == days,
                    onClick = { viewModel.setRetentionDays(days) },
                    shape = SegmentedButtonDefaults.itemShape(index, RETENTIONS.size),
                ) { Text(stringResource(R.string.settings_retention_days, days)) }
            }
        }

        SectionTitle(stringResource(R.string.settings_privacy_section))
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_analytics_title), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.settings_analytics_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(
                checked = settings.analyticsEnabled,
                onCheckedChange = viewModel::setAnalyticsEnabled,
            )
        }

        HorizontalDivider(Modifier.padding(vertical = 12.dp))
        TextButton(onClick = viewModel::clearArchive) {
            Text(stringResource(R.string.settings_clear_archive))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
    )
}
