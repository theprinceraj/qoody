package com.qoody.shared.feature.receipt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qoody.shared.core.DateProvider
import com.qoody.shared.core.endOfMonth
import com.qoody.shared.core.localDate
import com.qoody.shared.core.spentBetween
import com.qoody.shared.core.startOfMonth
import com.qoody.shared.core.stateInViewModel
import com.qoody.shared.domain.format.toReceiptCode
import com.qoody.shared.domain.model.Category
import com.qoody.shared.domain.model.Currency
import com.qoody.shared.domain.model.Money
import com.qoody.shared.domain.model.Permille
import com.qoody.shared.domain.model.Transaction
import com.qoody.shared.domain.model.TransactionId
import com.qoody.shared.domain.repository.LedgerRepository
import com.qoody.shared.domain.repository.SettingsRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** One-off instructions from the ViewModel to the screen. */
sealed interface ReceiptEvent {
    /** The user is done with this receipt; leave the screen. */
    data object Close : ReceiptEvent
}

class ReceiptViewModel(
    private val id: TransactionId,
    private val ledger: LedgerRepository,
    settings: SettingsRepository,
    private val dates: DateProvider,
) : ViewModel() {
    private val noteDraft = MutableStateFlow<String?>(null)
    private val isCategoryPickerOpen = MutableStateFlow(false)
    private val eventChannel = Channel<ReceiptEvent>(Channel.BUFFERED)

    val events: Flow<ReceiptEvent> = eventChannel.receiveAsFlow()

    val uiState: StateFlow<ReceiptUiState> =
        combine(
            ledger.observe(id),
            ledger.transactions,
            settings.settings,
            noteDraft,
            isCategoryPickerOpen,
        ) { transaction, settled, appSettings, draft, pickerOpen ->
            if (transaction == null) {
                ReceiptUiState.NotFound
            } else {
                content(transaction, settled, appSettings.currency, draft, pickerOpen)
            }
        }.stateInViewModel(viewModelScope, ReceiptUiState.Loading)

    fun onNoteChanged(text: String) = noteDraft.update { text }

    /** Persists the edited note, if it changed. Safe to call repeatedly. */
    fun onNoteCommitted() {
        val draft = noteDraft.value ?: return
        viewModelScope.launch { ledger.updateNote(id, draft) }
    }

    fun onCategoryPickerRequested() = isCategoryPickerOpen.update { true }

    fun onCategoryPickerDismissed() = isCategoryPickerOpen.update { false }

    fun onCategorySelected(category: Category) {
        isCategoryPickerOpen.value = false
        viewModelScope.launch { ledger.updateCategory(id, category) }
    }

    fun onKeepEntry() {
        viewModelScope.launch {
            noteDraft.value?.let { ledger.updateNote(id, it) }
            eventChannel.send(ReceiptEvent.Close)
        }
    }

    fun onExcludeFromLedger() {
        viewModelScope.launch {
            ledger.exclude(id)
            eventChannel.send(ReceiptEvent.Close)
        }
    }

    private fun content(
        transaction: Transaction,
        settled: List<Transaction>,
        currency: Currency,
        draft: String?,
        pickerOpen: Boolean,
    ): ReceiptUiState.Content {
        val zone = dates.zone
        val local = transaction.occurredAt.toLocalDateTime(zone)
        return ReceiptUiState.Content(
            code = id.toReceiptCode(),
            merchant = transaction.merchant,
            amount = transaction.amount,
            currency = currency,
            date = local.date,
            time = local.time,
            paymentApp = transaction.paymentApp,
            status = transaction.status,
            source = transaction.source,
            category = transaction.category,
            categorization = transaction.categorization,
            note = draft ?: transaction.note,
            notification = transaction.notification,
            paymentMethod = transaction.paymentMethod,
            referenceCode = transaction.referenceCode,
            categoryShare = categoryShare(transaction, settled, zone),
            isCategoryPickerOpen = pickerOpen,
        )
    }

    /** This amount as a share of everything in the same category during the same month. */
    private fun categoryShare(
        transaction: Transaction,
        settled: List<Transaction>,
        zone: TimeZone,
    ): CategoryShare {
        val monthStart = transaction.localDate(zone).startOfMonth()
        val categoryTotal: Money =
            settled
                .filter { it.category == transaction.category }
                .spentBetween(monthStart..monthStart.endOfMonth(), zone)
        val share = Permille.of(transaction.amount, categoryTotal).coerceAtMost(Permille.Full)
        return CategoryShare(transaction.category, share)
    }
}
