package com.qoody.shared.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qoody.shared.capture.CapturePolicy
import com.qoody.shared.core.DateProvider
import com.qoody.shared.core.stateInViewModel
import com.qoody.shared.domain.format.LedgerCsv
import com.qoody.shared.domain.model.AppSettings
import com.qoody.shared.domain.model.AppTheme
import com.qoody.shared.domain.model.Currency
import com.qoody.shared.domain.repository.LedgerRepository
import com.qoody.shared.domain.repository.SettingsRepository
import com.qoody.shared.domain.repository.UnparsedCaptureRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface SettingsUiState {
    data object Loading : SettingsUiState

    data class Content(
        val settings: AppSettings,
        /** Notifications in the "Failed to parse" list. */
        val unparsedCount: Int = 0,
        /** Entries the user excluded from the ledger. */
        val excludedCount: Int = 0,
    ) : SettingsUiState
}

class SettingsViewModel(
    private val settings: SettingsRepository,
    private val ledger: LedgerRepository,
    private val dates: DateProvider,
    unparsedCaptures: UnparsedCaptureRepository,
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> =
        combine(
            settings.settings,
            unparsedCaptures.captures,
            ledger.excluded,
        ) { appSettings, unparsed, excluded ->
            SettingsUiState.Content(
                settings = appSettings.copy(monitoredAppCount = CapturePolicy.supportedApps.size),
                unparsedCount = unparsed.size,
                excludedCount = excluded.size,
            )
        }.stateInViewModel(viewModelScope, SettingsUiState.Loading)

    fun onNotificationListenerToggled(enabled: Boolean) {
        viewModelScope.launch { settings.setNotificationListenerEnabled(enabled) }
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
