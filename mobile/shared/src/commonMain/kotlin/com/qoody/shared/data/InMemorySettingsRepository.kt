package com.qoody.shared.data

import com.qoody.shared.domain.model.AppSettings
import com.qoody.shared.domain.model.AppTheme
import com.qoody.shared.domain.model.Currency
import com.qoody.shared.domain.model.LlmSettings
import com.qoody.shared.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Temporary [SettingsRepository] held in memory.
 * When persistence is added, the API key MUST be stored encrypted (Android Keystore), never in plain prefs.
 */
class InMemorySettingsRepository(
    initial: AppSettings = defaultSettings,
) : SettingsRepository {
    private val state = MutableStateFlow(initial)

    override val settings: Flow<AppSettings> = state

    override suspend fun completeOnboarding() = state.update { it.copy(onboardingCompleted = true) }

    override suspend fun setNotificationListenerEnabled(enabled: Boolean) =
        state.update { it.copy(notificationListenerEnabled = enabled) }

    override suspend fun setLlmApiKey(key: String?) = state.update { it.copy(llm = it.llm.copy(apiKey = key)) }

    override suspend fun setCurrency(currency: Currency) = state.update { it.copy(currency = currency) }

    override suspend fun setTheme(theme: AppTheme) = state.update { it.copy(theme = theme) }

    override suspend fun setHapticsEnabled(enabled: Boolean) = state.update { it.copy(hapticsEnabled = enabled) }

    companion object {
        private const val SAMPLE_MONITORED_APP_COUNT = 6
        private const val SAMPLE_MODEL_LABEL = "Jev v1.2"

        val defaultSettings =
            AppSettings(
                onboardingCompleted = false,
                notificationListenerEnabled = false,
                monitoredAppCount = SAMPLE_MONITORED_APP_COUNT,
                llm = LlmSettings(apiKey = null, modelLabel = SAMPLE_MODEL_LABEL, localFallbackReady = true),
                currency = Currency.Usd,
                theme = AppTheme.WarmPaper,
                hapticsEnabled = true,
            )
    }
}
