package com.qoody.shared.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qoody.shared.core.DateProvider
import com.qoody.shared.core.stateInViewModel
import com.qoody.shared.domain.format.LedgerCsv
import com.qoody.shared.domain.model.AppSettings
import com.qoody.shared.domain.model.AppTheme
import com.qoody.shared.domain.model.Currency
import com.qoody.shared.domain.model.KeyVerification
import com.qoody.shared.domain.repository.LedgerRepository
import com.qoody.shared.domain.repository.LlmKeyVerifier
import com.qoody.shared.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface SettingsUiState {
    data object Loading : SettingsUiState

    data class Content(
        val settings: AppSettings,
        val isKeyVisible: Boolean,
        val keyVerification: KeyVerification,
    ) : SettingsUiState {
        val hasApiKey: Boolean get() = !settings.llm.apiKey.isNullOrEmpty()
    }
}

class SettingsViewModel(
    private val settings: SettingsRepository,
    private val ledger: LedgerRepository,
    private val keyVerifier: LlmKeyVerifier,
    private val dates: DateProvider,
) : ViewModel() {
    private val isKeyVisible = MutableStateFlow(false)
    private val keyVerification = MutableStateFlow<KeyVerification>(KeyVerification.Idle)

    val uiState: StateFlow<SettingsUiState> =
        combine(settings.settings, isKeyVisible, keyVerification) { appSettings, visible, verification ->
            SettingsUiState.Content(appSettings, visible, verification)
        }.stateInViewModel(viewModelScope, SettingsUiState.Loading)

    fun onNotificationListenerToggled(enabled: Boolean) {
        viewModelScope.launch { settings.setNotificationListenerEnabled(enabled) }
    }

    fun onApiKeyPasted(text: String) {
        val key = text.trim()
        if (key.isEmpty()) return
        keyVerification.value = KeyVerification.Idle
        viewModelScope.launch { settings.setLlmApiKey(key) }
    }

    fun onKeyVisibilityToggled() = isKeyVisible.update { !it }

    fun onTestKeyRequested() {
        if (keyVerification.value == KeyVerification.Testing) return
        viewModelScope.launch {
            val key =
                settings.settings
                    .first()
                    .llm.apiKey ?: return@launch
            keyVerification.value = KeyVerification.Testing
            keyVerification.value = if (keyVerifier.verify(key)) KeyVerification.Verified else KeyVerification.Failed
        }
    }

    fun onCurrencySelected(currency: Currency) {
        viewModelScope.launch { settings.setCurrency(currency) }
    }

    fun onThemeSelected(theme: AppTheme) {
        viewModelScope.launch { settings.setTheme(theme) }
    }

    fun onHapticsToggled(enabled: Boolean) {
        viewModelScope.launch { settings.setHapticsEnabled(enabled) }
    }

    /** The whole ledger as CSV text, ready to be written to a file the user picked. */
    suspend fun buildCsvExport(): String = LedgerCsv.build(ledger.transactions.first(), dates.zone)
}
