package com.qoody.shared.feature.receipt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qoody.shared.capture.MerchantCategoriser
import com.qoody.shared.core.DateProvider
import com.qoody.shared.core.endOfMonth
import com.qoody.shared.core.localDate
import com.qoody.shared.core.spentBetween
import com.qoody.shared.core.startOfMonth
import com.qoody.shared.core.stateInViewModel
import com.qoody.shared.domain.format.MoneyFormatter
import com.qoody.shared.domain.format.MoneyInput
import com.qoody.shared.domain.format.toReceiptCode
import com.qoody.shared.domain.model.BudgetProgress
import com.qoody.shared.domain.model.Categorization
import com.qoody.shared.domain.model.Category
import com.qoody.shared.domain.model.Currency
import com.qoody.shared.domain.model.EntryDetails
import com.qoody.shared.domain.model.Money
import com.qoody.shared.domain.model.Transaction
import com.qoody.shared.domain.model.TransactionId
import com.qoody.shared.domain.repository.BudgetRepository
import com.qoody.shared.domain.repository.LedgerRepository
import com.qoody.shared.domain.repository.MerchantCategoryRepository
import com.qoody.shared.domain.repository.SettingsRepository
import com.qoody.shared.feature.ledger.MERCHANT_MAX_LENGTH
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
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
    private val merchantCategories: MerchantCategoryRepository,
    budgets: BudgetRepository,
) : ViewModel() {
    private val categoriser = MerchantCategoriser(merchantCategories)
    private val noteDraft = MutableStateFlow<String?>(null)
    private val isCategoryPickerOpen = MutableStateFlow(false)
    private val editor = MutableStateFlow<EntryEditor?>(null)
    private val eventChannel = Channel<ReceiptEvent>(Channel.BUFFERED)

    val events: Flow<ReceiptEvent> = eventChannel.receiveAsFlow()

    val uiState: StateFlow<ReceiptUiState> =
        combine(
            ledger.observe(id),
            ledger.transactions,
            settings.settings,
            combine(noteDraft, isCategoryPickerOpen, editor, ::Overlays),
            budgets.budgets,
        ) { transaction, settled, appSettings, overlays, limits ->
            if (transaction == null) {
                ReceiptUiState.NotFound
            } else {
                content(transaction, settled, appSettings.currency, overlays, limits)
            }
        }.stateInViewModel(viewModelScope, ReceiptUiState.Loading)

    /** Screen-only state layered over the stored transaction. */
    private data class Overlays(
        val noteDraft: String?,
        val isCategoryPickerOpen: Boolean,
        val editor: EntryEditor?,
    )

    fun onNoteChanged(text: String) = noteDraft.update { text }

    /** Persists the edited note, if it changed. Safe to call repeatedly. */
    fun onNoteCommitted() {
        val draft = noteDraft.value ?: return
        viewModelScope.launch { ledger.updateNote(id, draft) }
    }

    fun onCategoryPickerRequested() = isCategoryPickerOpen.update { true }

    fun onCategoryPickerDismissed() = isCategoryPickerOpen.update { false }

    /** Also remembers the choice, so this merchant's future captures get the same category. */
    fun onCategorySelected(category: Category) {
        isCategoryPickerOpen.value = false
        viewModelScope.launch {
            ledger.updateCategory(id, category)
            ledger.observe(id).first()?.let { merchantCategories.remember(it.merchant, category) }
        }
    }

    fun onEditRequested() {
        viewModelScope.launch {
            val transaction = ledger.observe(id).first() ?: return@launch
            editor.value =
                EntryEditor(
                    amountInput = MoneyInput.sanitize(MoneyFormatter.formatPlain(transaction.amount)),
                    merchant = transaction.merchant,
                    date = transaction.localDate(dates.zone),
                    maxDate = dates.today(),
                )
        }
    }

    fun onEditAmountChanged(raw: String) = editor.update { it?.copy(amountInput = MoneyInput.sanitize(raw)) }

    fun onEditMerchantChanged(raw: String) = editor.update { it?.copy(merchant = raw.take(MERCHANT_MAX_LENGTH)) }

    fun onEditDateChanged(date: LocalDate) = editor.update { it?.copy(date = date) }

    fun onEditDismissed() = editor.update { null }

    /** Saves the corrected details; the time of day is kept when only the date changes. */
    fun onEditSaved() {
        val draft = editor.value?.takeIf { it.canSave } ?: return
        val amount = MoneyInput.parse(draft.amountInput) ?: return
        viewModelScope.launch {
            val transaction = ledger.observe(id).first() ?: return@launch
            val time = transaction.occurredAt.toLocalDateTime(dates.zone).time
            val merchant = draft.merchant.trim()
            ledger.updateDetails(
                id,
                EntryDetails(
                    merchant = merchant,
                    amount = amount,
                    occurredAt = draft.date.atTime(time).toInstant(dates.zone),
                    recategorised = recategorise(transaction, merchant),
                ),
            )
            editor.value = null
        }
    }

    /**
     * A corrected merchant on an uncategorized entry (typically a bank SMS that named no payee) gets the
     * same category a capture would: the remembered choice or a keyword rule. A category the user or
     * Qoody already set is left alone.
     */
    private suspend fun recategorise(
        transaction: Transaction,
        merchant: String,
    ): Pair<Category, Categorization>? {
        if (transaction.category != Category.Uncategorized || merchant == transaction.merchant) return null
        return categoriser.categorise(merchant).takeIf { (category, _) -> category != Category.Uncategorized }
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

    fun onRestoreToLedger() {
        viewModelScope.launch {
            ledger.restore(id)
            eventChannel.send(ReceiptEvent.Close)
        }
    }

    private fun content(
        transaction: Transaction,
        settled: List<Transaction>,
        currency: Currency,
        overlays: Overlays,
        limits: Map<Category, Money>,
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
            note = overlays.noteDraft ?: transaction.note,
            notification = transaction.notification,
            paymentMethod = transaction.paymentMethod,
            referenceCode = transaction.referenceCode,
            budgetImpact = budgetImpact(transaction, settled, limits, zone),
            isCategoryPickerOpen = overlays.isCategoryPickerOpen,
            editor = overlays.editor,
        )
    }

    /** The entry's category budget for the month the entry happened in. */
    private fun budgetImpact(
        transaction: Transaction,
        settled: List<Transaction>,
        limits: Map<Category, Money>,
        zone: TimeZone,
    ): BudgetImpact? {
        if (transaction.category == Category.Uncategorized) return null
        val monthStart = transaction.localDate(zone).startOfMonth()
        val spent =
            settled
                .filter { it.category == transaction.category }
                .spentBetween(monthStart..monthStart.endOfMonth(), zone)
        return BudgetImpact(transaction.category, BudgetProgress(spent, limits[transaction.category]))
    }
}
