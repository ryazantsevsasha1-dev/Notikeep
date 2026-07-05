package com.notikeep.presentation.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.LaunchedEffect
import com.notikeep.presentation.common.SystemSettings
import androidx.compose.ui.platform.LocalContext

/** Three honest steps: value → "we save from now on" → grant access + battery. */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    var step by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    LaunchedEffect(step) { viewModel.onStepViewed(step) }

    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (step) {
            0 -> Step(
                title = "Ничего не потеряется",
                body = "Notikeep сохраняет каждое уведомление в локальный архив на вашем телефоне. Без облака и без рекламы.",
                primary = "Далее" to { step = 1 },
            )
            1 -> Step(
                title = "Сохраняем с этого момента",
                body = "Notikeep начнёт сохранять уведомления после того, как вы дадите доступ. Показать те, что пришли до установки, технически невозможно.",
                primary = "Понятно" to { step = 2 },
            )
            else -> GrantStep(
                onOpenAccess = { SystemSettings.openNotificationAccess(context) },
                onOpenBattery = { SystemSettings.requestIgnoreBatteryOptimization(context) },
                onFinish = { viewModel.complete(onFinished) },
            )
        }
    }
}

@Composable
private fun Step(title: String, body: String, primary: Pair<String, () -> Unit>) {
    Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
    Text(
        body,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(vertical = 16.dp),
    )
    Button(onClick = primary.second, modifier = Modifier.fillMaxWidth()) { Text(primary.first) }
}

@Composable
private fun GrantStep(
    onOpenAccess: () -> Unit,
    onOpenBattery: () -> Unit,
    onFinish: () -> Unit,
) {
    Text("Дайте доступ", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
    Text(
        "Notikeep нужен доступ к уведомлениям, чтобы их сохранять. Отключите оптимизацию батареи, чтобы слежение не прерывалось.",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(vertical = 16.dp),
    )
    OutlinedButton(onClick = onOpenAccess, modifier = Modifier.fillMaxWidth()) {
        Text("Доступ к уведомлениям")
    }
    OutlinedButton(
        onClick = onOpenBattery,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        Text("Оптимизация батареи")
    }
    Button(onClick = onFinish, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Text("Готово")
    }
}
