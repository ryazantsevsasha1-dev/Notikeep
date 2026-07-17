package com.notikeep.presentation.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.notikeep.R
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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

    // Permission status is re-read every time the user returns from a system
    // settings screen, so the grant step reflects reality without a manual refresh.
    var accessGranted by remember { mutableStateOf(viewModel.isAccessGranted()) }
    var batteryIgnored by remember { mutableStateOf(viewModel.isBatteryOptimizationIgnored()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accessGranted = viewModel.isAccessGranted()
                batteryIgnored = viewModel.isBatteryOptimizationIgnored()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Android 13+ needs a runtime grant to post our own notifications (daily
    // summary + future push). Asked once here; a refusal is silent — the app
    // still works, those notifications just won't show.
    val notificationsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* result ignored: absence is handled gracefully at post time */ }

    fun requestPostNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(step) { viewModel.onStepViewed(step) }

    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (step) {
            0 -> Step(
                title = stringResource(R.string.onboarding_step1_title),
                body = stringResource(R.string.onboarding_step1_body),
                primary = stringResource(R.string.onboarding_step1_action) to { step = 1 },
            )
            1 -> Step(
                title = stringResource(R.string.onboarding_step2_title),
                body = stringResource(R.string.onboarding_step2_body),
                primary = stringResource(R.string.onboarding_step2_action) to { step = 2 },
            )
            else -> {
                // Ask for POST_NOTIFICATIONS as soon as the grant step appears.
                LaunchedEffect(Unit) { requestPostNotifications() }
                GrantStep(
                    accessGranted = accessGranted,
                    batteryIgnored = batteryIgnored,
                    onOpenAccess = { SystemSettings.openNotificationAccess(context) },
                    onOpenBattery = { SystemSettings.requestIgnoreBatteryOptimization(context) },
                    onFinish = { viewModel.complete(onFinished) },
                )
            }
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
    accessGranted: Boolean,
    batteryIgnored: Boolean,
    onOpenAccess: () -> Unit,
    onOpenBattery: () -> Unit,
    onFinish: () -> Unit,
) {
    Text(stringResource(R.string.onboarding_grant_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
    Text(
        stringResource(R.string.onboarding_grant_body),
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(vertical = 16.dp),
    )

    GrantButton(
        granted = accessGranted,
        pendingText = stringResource(R.string.onboarding_grant_access),
        doneText = stringResource(R.string.onboarding_grant_access_done),
        onClick = onOpenAccess,
    )
    Spacer(Modifier.size(8.dp))
    GrantButton(
        granted = batteryIgnored,
        pendingText = stringResource(R.string.onboarding_grant_battery),
        doneText = stringResource(R.string.onboarding_grant_battery_done),
        onClick = onOpenBattery,
    )

    if (!accessGranted) {
        Text(
            stringResource(R.string.onboarding_grant_hint_pending),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
    }

    Button(onClick = onFinish, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Text(
            stringResource(
                if (accessGranted) R.string.onboarding_grant_finish_ready
                else R.string.onboarding_grant_finish,
            ),
        )
    }
}

/** Outlined action that flips to a checkmarked "done" state once granted. */
@Composable
private fun GrantButton(
    granted: Boolean,
    pendingText: String,
    doneText: String,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = !granted,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (granted) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(8.dp))
        }
        Text(if (granted) doneText else pendingText)
    }
}
