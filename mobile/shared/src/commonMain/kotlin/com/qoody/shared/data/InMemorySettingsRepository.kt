package com.qoody.shared.data

import com.qoody.shared.domain.model.AppSettings
import com.qoody.shared.domain.model.AppTheme
import com.qoody.shared.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** [SettingsRepository] held in memory, used by tests and screenshot fixtures. */
class InMemorySettingsRepository(
    initial: AppSettings = defaultSettings,
) : SettingsRepository {
    private val state = MutableStateFlow(initial)

    override val settings: Flow<AppSettings> = state

    override suspend fun completeOnboarding() = state.update { it.copy(onboardingCompleted = true) }

    override suspend fun setNotificationListenerEnabled(enabled: Boolean) =
        state.update { it.copy(notificationListenerEnabled = enabled) }

    override suspend fun setTheme(theme: AppTheme) = state.update { it.copy(theme = theme) }

    override suspend fun setHapticsEnabled(enabled: Boolean) = state.update { it.copy(hapticsEnabled = enabled) }

    companion object {
        private const val SAMPLE_MONITORED_APP_COUNT = 6

        val defaultSettings =
            AppSettings(
                onboardingCompleted = false,
                notificationListenerEnabled = false,
                monitoredAppCount = SAMPLE_MONITORED_APP_COUNT,
                theme = AppTheme.WarmPaper,
                hapticsEnabled = true,
            )
    }
}
