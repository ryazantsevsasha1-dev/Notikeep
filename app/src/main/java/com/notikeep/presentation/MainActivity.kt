package com.notikeep.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notikeep.data.ads.InterstitialAdManager
import com.notikeep.data.service.ListenerRebinder
import com.notikeep.presentation.navigation.NotikeepNavHost
import com.notikeep.presentation.onboarding.ConsentScreen
import com.notikeep.presentation.theme.NotikeepTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var listenerRebinder: ListenerRebinder
    @Inject lateinit var interstitialAdManager: InterstitialAdManager

    /** Every foreground return is a cheap chance to heal a dead listener binding. */
    override fun onResume() {
        super.onResume()
        listenerRebinder.ensureBound()
    }

    // POST_NOTIFICATIONS is requested in the onboarding grant step, where the user
    // has context for why — not here, where the dialog would cover the consent screen.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: RootViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()

            NotikeepTheme(themeMode = state.themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (state.loaded) {
                        if (!state.termsAccepted) {
                            ConsentScreen(onAccept = viewModel::acceptTerms)
                        } else {
                            NotikeepNavHost(
                                onboardingCompleted = state.onboardingCompleted,
                                onNaturalBreak = {
                                    interstitialAdManager.onNaturalBreak(this@MainActivity)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
