package com.qoody.shared.feature.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qoody.shared.core.DateProvider
import com.qoody.shared.core.endOfMonth
import com.qoody.shared.core.spentBetween
import com.qoody.shared.core.startOfMonth
import com.qoody.shared.core.stateInViewModel
import com.qoody.shared.domain.format.MoneyFormatter
import com.qoody.shared.domain.format.MoneyInput
import com.qoody.shared.domain.model.Category
import com.qoody.shared.domain.model.Currency
import com.qoody.shared.domain.model.Money
import com.qoody.shared.domain.model.Permille
import com.qoody.shared.domain.repository.BudgetRepository
import com.qoody.shared.domain.repository.LedgerRepository
import com.qoody.shared.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Month

/** How one category's spending this month compares with its monthly limit. */
data class BudgetProgress(
    val spent: Money,
    /** `null` when the category has no budget. */
    val limit: Money?,
) {
    /** Share of the limit used; above [Permille.Full] when over budget. `null` without a limit. */
    val used: Permille? get() = limit?.let { Permille.of(spent, it) }

    val isOver: Boolean get() = limit != null && spent > limit
}

data class BudgetRow(
    val category: Category,
    val progress: BudgetProgress,
)

/** The amount sheet for one category while it is open. */
data class BudgetEditor(
    val category: Category,
    val amountInput: String,
    /** Whether the category already has a budget, so the sheet offers to remove it. */
    val hasBudget: Boolean,
) {
    val canSave: Boolean get() = MoneyInput.parse(amountInput) != null
}

sealed interface BudgetsUiState {
    data object Loading : BudgetsUiState

    data class Content(
        val currency: Currency,
        val month: Month,
        val rows: List<BudgetRow>,
        val editor: BudgetEditor?,
    ) : BudgetsUiState
}

/** Categories a budget can be set for; "Uncategorized" is a to-do pile, not a spending goal. */
val BudgetableCategories: List<Category> = Category.entries.filter { it != Category.Uncategorized }

class BudgetsViewModel(
    private val budgets: BudgetRepository,
    ledger: LedgerRepository,
    settings: SettingsRepository,
    private val dates: DateProvider,
) : ViewModel() {
    private val editor = MutableStateFlow<BudgetEditor?>(null)

    val uiState: StateFlow<BudgetsUiState> =
        combine(
            budgets.budgets,
            ledger.transactions,
            settings.settings,
            editor,
        ) { limits, transactions, appSettings, open ->
            val today = dates.today()
            val month = today.startOfMonth()..today.endOfMonth()
            BudgetsUiState.Content(
                currency = appSettings.currency,
                month = today.month,
                rows =
                    BudgetableCategories.map { category ->
                        val spent = transactions.filter { it.category == category }.spentBetween(month, dates.zone)
                        BudgetRow(category, BudgetProgress(spent, limits[category]))
                    },
                editor = open,
            )
        }.stateInViewModel(viewModelScope, BudgetsUiState.Loading)

    fun onEdit(category: Category) {
        val current = (uiState.value as? BudgetsUiState.Content)?.rows?.firstOrNull { it.category == category }
        val limit = current?.progress?.limit
        editor.value =
            BudgetEditor(
                category = category,
                amountInput = limit?.let { MoneyInput.sanitize(MoneyFormatter.formatPlain(it)) }.orEmpty(),
                hasBudget = limit != null,
            )
    }

    fun onAmountChanged(raw: String) = editor.update { it?.copy(amountInput = MoneyInput.sanitize(raw)) }

    fun onSave() {
        val open = editor.value ?: return
        val limit = MoneyInput.parse(open.amountInput) ?: return
        viewModelScope.launch {
            budgets.setBudget(open.category, limit)
            editor.value = null
        }
    }

    fun onRemove() {
        val open = editor.value ?: return
        viewModelScope.launch {
            budgets.setBudget(open.category, null)
            editor.value = null
        }
    }

    fun onDismiss() = editor.update { null }
}
