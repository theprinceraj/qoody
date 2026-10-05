package com.qoody.shared.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qoody.shared.domain.repository.SettingsRepository
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val settings: SettingsRepository,
) : ViewModel() {
    /**
     * The user chose to grant notification access. The platform permission screen is opened by the
     * caller; here we record the choice and finish onboarding so the ledger is shown on return.
     */
    fun onEnableNotificationAccess() {
        viewModelScope.launch {
            settings.setNotificationListenerEnabled(true)
            settings.completeOnboarding()
        }
    }
}
