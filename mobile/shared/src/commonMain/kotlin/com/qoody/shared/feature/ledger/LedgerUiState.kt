package com.qoody.shared.feature.ledger

import com.qoody.shared.domain.model.BudgetProgress
import com.qoody.shared.domain.model.Category
import com.qoody.shared.domain.model.Currency
import com.qoody.shared.domain.model.Money
import com.qoody.shared.domain.model.Permille
import com.qoody.shared.domain.model.TransactionId
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month

sealed interface LedgerUiState {
    data object Loading : LedgerUiState

    data class Content(
        val currency: Currency,
        val hapticsEnabled: Boolean,
        val summary: MonthSummary,
        val selectedCategory: Category?,
        val search: SearchState,
        val dayGroups: List<DayGroup>,
    ) : LedgerUiState {
        /** True when the ledger is empty because of the filter/search rather than a lack of data. */
        val isFiltered: Boolean get() = selectedCategory != null || search.query.isNotBlank()
    }
}

data class SearchState(
    val isActive: Boolean = false,
    val query: String = "",
)

enum class TrendDirection {
    Lower,
    Higher,
    Same,
}

/** How this month compares with last month. */
data class Trend(
    val direction: TrendDirection,
    val change: Permille,
    val previousSpent: Money,
)

data class MonthSummary(
    val month: Month,
    val spent: Money,
    /** `null` when last month has no spending to compare against. */
    val trend: Trend?,
    /**
     * Spending this month in categories that have a budget, against the sum of those budgets.
     * `null` when no budget is set.
     */
    val budget: BudgetProgress?,
)

enum class DayLabel {
    Today,
    Yesterday,
    Other,
}

data class DayGroup(
    val date: LocalDate,
    val label: DayLabel,
    val total: Money,
    val rows: List<LedgerRow>,
)

data class LedgerRow(
    val id: TransactionId,
    val merchant: String,
    val paymentApp: String,
    val time: LocalTime,
    val amount: Money,
    val category: Category,
    val tag: String?,
)
