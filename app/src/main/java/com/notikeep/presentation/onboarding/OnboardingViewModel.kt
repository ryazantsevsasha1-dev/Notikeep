package com.notikeep.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notikeep.data.service.ServiceHealthProvider
import com.notikeep.domain.port.Analytics
import com.notikeep.domain.port.AnalyticsEvent
import com.notikeep.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val healthProvider: ServiceHealthProvider,
    private val analytics: Analytics,
) : ViewModel() {

    fun onStepViewed(step: Int) = analytics.track(AnalyticsEvent.OnboardingStepViewed(step))

    fun isAccessGranted(): Boolean = healthProvider.current().notificationAccessGranted

    fun isBatteryOptimizationIgnored(): Boolean =
        healthProvider.current().batteryOptimizationIgnored

    fun complete(onDone: () -> Unit) {
        viewModelScope.launch {
            if (isAccessGranted()) analytics.track(AnalyticsEvent.NotificationAccessGranted)
            if (isBatteryOptimizationIgnored()) analytics.track(AnalyticsEvent.BatteryOptimizationDisabled)
            settings.setOnboardingCompleted(true)
            onDone()
        }
    }
}
