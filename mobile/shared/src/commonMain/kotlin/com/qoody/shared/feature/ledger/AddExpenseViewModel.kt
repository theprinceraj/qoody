package com.qoody.shared.feature.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qoody.shared.core.stateInViewModel
import com.qoody.shared.domain.format.MoneyInput
import com.qoody.shared.domain.model.Category
import com.qoody.shared.domain.model.Currency
import com.qoody.shared.domain.model.NewExpense
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

/** Longest merchant name accepted by the manual entry form. */
const val MERCHANT_MAX_LENGTH = 60

data class AddExpenseUiState(
    val amountInput: String = "",
    val merchant: String = "",
    val category: Category = Category.Uncategorized,
    val currency: Currency = Currency.Usd,
    val canSave: Boolean = false,
)

sealed interface AddExpenseEvent {
    data object Saved : AddExpenseEvent
}

class AddExpenseViewModel(
    private val ledger: LedgerRepository,
    settings: SettingsRepository,
) : ViewModel() {
    private data class Draft(
        val amountInput: String = "",
        val merchant: String = "",
        val category: Category = Category.Uncategorized,
    )

    private val draft = MutableStateFlow(Draft())
    private val eventChannel = Channel<AddExpenseEvent>(Channel.BUFFERED)

    val events: Flow<AddExpenseEvent> = eventChannel.receiveAsFlow()

    val uiState: StateFlow<AddExpenseUiState> =
        combine(draft, settings.settings) { draft, appSettings ->
            AddExpenseUiState(
                amountInput = draft.amountInput,
                merchant = draft.merchant,
                category = draft.category,
                currency = appSettings.currency,
                canSave = MoneyInput.parse(draft.amountInput) != null && draft.merchant.isNotBlank(),
            )
        }.stateInViewModel(viewModelScope, AddExpenseUiState())

    fun onAmountChanged(raw: String) = draft.update { it.copy(amountInput = MoneyInput.sanitize(raw)) }

    fun onMerchantChanged(raw: String) = draft.update { it.copy(merchant = raw.take(MERCHANT_MAX_LENGTH)) }

    fun onCategorySelected(category: Category) = draft.update { it.copy(category = category) }

    fun onSave() {
        val current = draft.value
        val amount = MoneyInput.parse(current.amountInput) ?: return
        if (current.merchant.isBlank()) return
        viewModelScope.launch {
            ledger.add(NewExpense(current.merchant.trim(), amount, current.category))
            draft.value = Draft()
            eventChannel.send(AddExpenseEvent.Saved)
        }
    }
}
