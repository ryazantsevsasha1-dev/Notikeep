package com.notikeep.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notikeep.domain.model.ThemeMode
import com.notikeep.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** App-level state that drives the theme, the consent gate and the onboarding gate. */
data class RootUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val termsAccepted: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val loaded: Boolean = false,
)

@HiltViewModel
class RootViewModel @Inject constructor(
    private val settings: SettingsRepository,
) : ViewModel() {

    val state = settings.observe()
        .map { RootUiState(it.themeMode, it.termsAccepted, it.onboardingCompleted, loaded = true) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RootUiState())

    fun acceptTerms() = viewModelScope.launch { settings.setTermsAccepted(true) }
}
