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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notikeep.domain.model.ThemeMode
import com.notikeep.presentation.common.SystemSettings

private val THEMES = listOf(
    ThemeMode.SYSTEM to "Система",
    ThemeMode.LIGHT to "Светлая",
    ThemeMode.DARK to "Тёмная",
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

        SectionTitle("Состояние сервиса")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    if (state.health.isHealthy) "Слежение активно ✓" else "Требует внимания ⚠",
                    style = MaterialTheme.typography.titleMedium,
                )
                if (!state.health.notificationAccessGranted) {
                    TextButton(onClick = { SystemSettings.openNotificationAccess(context) }) {
                        Text("Включить доступ к уведомлениям")
                    }
                }
                if (!state.health.batteryOptimizationIgnored) {
                    TextButton(onClick = { SystemSettings.requestIgnoreBatteryOptimization(context) }) {
                        Text("Отключить оптимизацию батареи")
                    }
                }
            }
        }

        SectionTitle("Тема")
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            THEMES.forEachIndexed { index, (mode, label) ->
                SegmentedButton(
                    selected = settings.themeMode == mode,
                    onClick = { viewModel.setTheme(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index, THEMES.size),
                ) { Text(label) }
            }
        }

        SectionTitle("Срок хранения")
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            RETENTIONS.forEachIndexed { index, days ->
                SegmentedButton(
                    selected = settings.retentionDays == days,
                    onClick = { viewModel.setRetentionDays(days) },
                    shape = SegmentedButtonDefaults.itemShape(index, RETENTIONS.size),
                ) { Text("$days дн") }
            }
        }

        SectionTitle("Приватность")
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Помогать улучшать приложение", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Анонимно, без содержимого уведомлений",
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
            Text("Очистить весь архив")
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
