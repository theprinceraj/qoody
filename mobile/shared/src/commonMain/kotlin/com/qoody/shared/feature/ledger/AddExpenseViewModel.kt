package com.qoody.shared.feature.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qoody.shared.core.DateProvider
import com.qoody.shared.core.stateInViewModel
import com.qoody.shared.domain.format.MoneyInput
import com.qoody.shared.domain.model.Category
import com.qoody.shared.domain.model.NewExpense
import com.qoody.shared.domain.repository.LedgerRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/** Longest merchant name accepted by the manual entry form. */
const val MERCHANT_MAX_LENGTH = 60

data class AddExpenseUiState(
    val amountInput: String = "",
    val merchant: String = "",
    val category: Category = Category.Uncategorized,
    /** The day the payment was made; today unless the user picks another. */
    val date: LocalDate? = null,
    /** The latest selectable day: entries never lie in the future. */
    val maxDate: LocalDate? = null,
    val canSave: Boolean = false,
)

sealed interface AddExpenseEvent {
    data object Saved : AddExpenseEvent
}

class AddExpenseViewModel(
    private val ledger: LedgerRepository,
    private val dates: DateProvider,
) : ViewModel() {
    private data class Draft(
        val amountInput: String = "",
        val merchant: String = "",
        val category: Category = Category.Uncategorized,
        /** `null` until the user picks a day, so the form follows today across midnight. */
        val date: LocalDate? = null,
    )

    private val draft = MutableStateFlow(Draft())
    private val eventChannel = Channel<AddExpenseEvent>(Channel.BUFFERED)

    val events: Flow<AddExpenseEvent> = eventChannel.receiveAsFlow()

    val uiState: StateFlow<AddExpenseUiState> =
        draft
            .map { draft ->
                AddExpenseUiState(
                    amountInput = draft.amountInput,
                    merchant = draft.merchant,
                    category = draft.category,
                    date = draft.date ?: dates.today(),
                    maxDate = dates.today(),
                    canSave = MoneyInput.parse(draft.amountInput) != null && draft.merchant.isNotBlank(),
                )
            }.stateInViewModel(viewModelScope, AddExpenseUiState())

    fun onAmountChanged(raw: String) = draft.update { it.copy(amountInput = MoneyInput.sanitize(raw)) }

    fun onMerchantChanged(raw: String) = draft.update { it.copy(merchant = raw.take(MERCHANT_MAX_LENGTH)) }

    fun onCategorySelected(category: Category) = draft.update { it.copy(category = category) }

    /** Future days are ignored. */
    fun onDateChanged(date: LocalDate) {
        if (date <= dates.today()) draft.update { it.copy(date = date) }
    }

    fun onSave() {
        val current = draft.value
        val amount = MoneyInput.parse(current.amountInput) ?: return
        if (current.merchant.isBlank()) return
        viewModelScope.launch {
            ledger.add(NewExpense(current.merchant.trim(), amount, current.category, occurredAt(current.date)))
            draft.value = Draft()
            eventChannel.send(AddExpenseEvent.Saved)
        }
    }

    /** Now for today; for an earlier day, that day at the current time of day. */
    private fun occurredAt(date: LocalDate?) =
        date?.takeIf { it != dates.today() }?.let {
            it.atTime(dates.now().toLocalDateTime(dates.zone).time).toInstant(dates.zone)
        }
}
