package com.qoody.shared.feature.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qoody.shared.core.DateProvider
import com.qoody.shared.core.localDate
import com.qoody.shared.core.spentBetween
import com.qoody.shared.core.startOfMonth
import com.qoody.shared.core.startOfPreviousMonth
import com.qoody.shared.core.stateInViewModel
import com.qoody.shared.domain.model.BudgetProgress
import com.qoody.shared.domain.model.Category
import com.qoody.shared.domain.model.Money
import com.qoody.shared.domain.model.Permille
import com.qoody.shared.domain.model.Transaction
import com.qoody.shared.domain.model.sumMoneyOf
import com.qoody.shared.domain.repository.BudgetRepository
import com.qoody.shared.domain.repository.LedgerRepository
import com.qoody.shared.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

class LedgerViewModel(
    ledger: LedgerRepository,
    settings: SettingsRepository,
    private val dates: DateProvider,
    budgets: BudgetRepository,
) : ViewModel() {
    private val selectedCategory = MutableStateFlow<Category?>(null)
    private val search = MutableStateFlow(SearchState())

    val uiState: StateFlow<LedgerUiState> =
        combine(
            ledger.transactions,
            settings.settings,
            selectedCategory,
            search,
            budgets.budgets,
        ) { transactions, appSettings, category, search, limits ->
            val zone = dates.zone
            val today = dates.today()
            LedgerUiState.Content(
                currency = appSettings.currency,
                hapticsEnabled = appSettings.hapticsEnabled,
                summary = summarise(transactions, limits, today, zone),
                selectedCategory = category,
                search = search,
                dayGroups = groupByDay(transactions.filter { it.matches(category, search.query) }, today, zone),
            )
        }.stateInViewModel(viewModelScope, LedgerUiState.Loading)

    fun onCategorySelected(category: Category?) = selectedCategory.update { category }

    fun onSearchOpened() = search.update { it.copy(isActive = true) }

    fun onSearchQueryChanged(query: String) = search.update { it.copy(query = query) }

    fun onSearchClosed() = search.update { SearchState() }

    private fun summarise(
        transactions: List<Transaction>,
        limits: Map<Category, Money>,
        today: LocalDate,
        zone: TimeZone,
    ): MonthSummary {
        val thisMonth = today.startOfMonth()
        val lastMonth = today.startOfPreviousMonth()
        val spent = transactions.spentBetween(thisMonth..today, zone)
        val previous = transactions.spentBetween(lastMonth..thisMonth.minus(1, DateTimeUnit.DAY), zone)
        return MonthSummary(
            month = today.month,
            spent = spent,
            trend = trendOf(spent, previous),
            budget =
                limits.takeIf { it.isNotEmpty() }?.let {
                    BudgetProgress(
                        spent = transactions.filter { it.category in limits }.spentBetween(thisMonth..today, zone),
                        limit = limits.values.sumMoneyOf { it },
                    )
                },
        )
    }

    private fun trendOf(
        spent: Money,
        previous: Money,
    ): Trend? {
        if (previous.isZero) return null
        val direction =
            when {
                spent < previous -> TrendDirection.Lower
                spent > previous -> TrendDirection.Higher
                else -> TrendDirection.Same
            }
        return Trend(direction, Permille.of((spent - previous).absolute, previous), previous)
    }

    private fun groupByDay(
        transactions: List<Transaction>,
        today: LocalDate,
        zone: TimeZone,
    ): List<DayGroup> {
        val yesterday = today.minus(1, DateTimeUnit.DAY)
        return transactions
            .groupBy { it.localDate(zone) }
            .toSortedMap(compareByDescending { it })
            .map { (date, group) ->
                DayGroup(
                    date = date,
                    label =
                        when (date) {
                            today -> DayLabel.Today
                            yesterday -> DayLabel.Yesterday
                            else -> DayLabel.Other
                        },
                    total = group.sumMoneyOf { it.amount },
                    rows = group.sortedByDescending { it.occurredAt }.map { it.toRow(zone) },
                )
            }
    }

    private fun Transaction.toRow(zone: TimeZone) =
        LedgerRow(
            id = id,
            merchant = merchant,
            paymentApp = paymentApp,
            time = occurredAt.toLocalDateTime(zone).time,
            amount = amount,
            category = category,
            tag = tag,
        )

    private fun Transaction.matches(
        category: Category?,
        query: String,
    ): Boolean {
        val needle = query.trim()
        val matchesCategory = category == null || this.category == category
        val matchesQuery =
            needle.isEmpty() ||
                listOfNotNull(merchant, note, tag, paymentApp).any { it.contains(needle, ignoreCase = true) }
        return matchesCategory && matchesQuery
    }
}
