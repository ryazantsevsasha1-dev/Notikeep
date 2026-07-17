package com.notikeep.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notikeep.data.service.ListenerRebinder
import com.notikeep.presentation.navigation.NotikeepNavHost
import com.notikeep.presentation.onboarding.ConsentScreen
import com.notikeep.presentation.theme.NotikeepTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var listenerRebinder: ListenerRebinder

    /** Android 13+ gate for showing our own notifications (future push campaigns). */
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* optional feature; no-op on deny */ }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    /** Every foreground return is a cheap chance to heal a dead listener binding. */
    override fun onResume() {
        super.onResume()
        listenerRebinder.ensureBound()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        setContent {
            val viewModel: RootViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()

            NotikeepTheme(themeMode = state.themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (state.loaded) {
                        if (!state.termsAccepted) {
                            ConsentScreen(onAccept = viewModel::acceptTerms)
                        } else {
                            NotikeepNavHost(onboardingCompleted = state.onboardingCompleted)
                        }
                    }
                }
            }
        }
    }
}
