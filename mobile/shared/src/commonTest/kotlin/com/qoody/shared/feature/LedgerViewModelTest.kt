package com.qoody.shared.feature

import com.qoody.shared.ViewModelTest
import com.qoody.shared.data.InMemoryBudgetRepository
import com.qoody.shared.data.InMemoryLedgerRepository
import com.qoody.shared.data.InMemorySettingsRepository
import com.qoody.shared.domain.model.Category
import com.qoody.shared.domain.model.Money
import com.qoody.shared.domain.model.Transaction
import com.qoody.shared.feature.ledger.DayLabel
import com.qoody.shared.feature.ledger.LedgerUiState
import com.qoody.shared.feature.ledger.LedgerViewModel
import com.qoody.shared.feature.ledger.TrendDirection
import com.qoody.shared.fixedDates
import com.qoody.shared.transaction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LedgerViewModelTest : ViewModelTest() {
    private val dates = fixedDates()
    private val budgets = InMemoryBudgetRepository()

    private fun viewModel(vararg transactions: Transaction) =
        LedgerViewModel(
            InMemoryLedgerRepository(dates, transactions.toList()),
            InMemorySettingsRepository(),
            dates,
            budgets,
        )

    private fun LedgerViewModel.content() = uiState.latest() as LedgerUiState.Content

    private val coffee = transaction(0, Money.of(4, 50), Category.FoodAndDrink, "Coffee", hourOfDay = 9, note = "iced")
    private val zomato = transaction(0, Money.of(18, 20), Category.FoodAndDrink, "Zomato", hourOfDay = 13)
    private val spotify = transaction(1, Money.of(10, 99), Category.Subscriptions, "Spotify", tag = "music")
    private val shoes = transaction(5, Money.of(100), Category.Shopping, "Shoes")
    private val rent = transaction(30, Money.of(200), Category.Bills, "Rent")
    private val lunch = transaction(40, Money.of(100), Category.FoodAndDrink, "Lunch")

    private val everything = arrayOf(coffee, zomato, spotify, shoes, rent, lunch)

    @Test
    fun groupsByDayNewestFirstWithRelativeLabels() =
        runTest {
            val state = viewModel(*everything).content()

            assertEquals(
                listOf(DayLabel.Today, DayLabel.Yesterday, DayLabel.Other, DayLabel.Other, DayLabel.Other),
                state.dayGroups.map { it.label },
            )
            val today = state.dayGroups.first()
            assertEquals(listOf("Zomato", "Coffee"), today.rows.map { it.merchant })
            assertEquals(Money.of(22, 70), today.total)
        }

    @Test
    fun summaryComparesThisMonthWithLastMonth() =
        runTest {
            val summary = viewModel(*everything).content().summary

            assertEquals(Money.of(133, 69), summary.spent)
            val trend = summary.trend!!
            assertEquals(TrendDirection.Lower, trend.direction)
            assertEquals(Money.of(300), trend.previousSpent)
            assertEquals(554, trend.change.value)
            assertNull(summary.budget)
        }

    @Test
    fun budgetSummaryCountsOnlyBudgetedCategoriesThisMonth() =
        runTest {
            budgets.setBudget(Category.FoodAndDrink, Money.of(20))
            budgets.setBudget(Category.Subscriptions, Money.of(20))

            val budget = viewModel(*everything).content().summary.budget!!

            // Coffee + Zomato + Spotify; shoes (no budget) and last month's lunch are left out.
            assertEquals(Money.of(33, 69), budget.spent)
            assertEquals(Money.of(40), budget.limit)
            assertEquals(842, budget.used!!.value)
            assertFalse(budget.isOver)
        }

    @Test
    fun noTrendWhenLastMonthIsEmpty() =
        runTest {
            val summary = viewModel(coffee, zomato).content().summary
            assertNull(summary.trend)
            assertNull(summary.budget)
        }

    @Test
    fun filtersByCategory() =
        runTest {
            val viewModel = viewModel(*everything)
            viewModel.onCategorySelected(Category.FoodAndDrink)

            val state = viewModel.content()

            assertTrue(state.isFiltered)
            assertEquals(
                setOf("Coffee", "Zomato", "Lunch"),
                state.dayGroups
                    .flatMap { it.rows }
                    .map { it.merchant }
                    .toSet(),
            )
            // The month summary ignores the filter.
            assertEquals(Money.of(133, 69), state.summary.spent)

            viewModel.onCategorySelected(null)
            assertFalse(viewModel.content().isFiltered)
        }

    @Test
    fun searchMatchesMerchantNoteAndTagCaseInsensitively() =
        runTest {
            val viewModel = viewModel(*everything)
            viewModel.onSearchOpened()

            viewModel.onSearchQueryChanged("ZOMA")
            assertEquals(
                listOf("Zomato"),
                viewModel
                    .content()
                    .dayGroups
                    .flatMap { it.rows }
                    .map { it.merchant },
            )

            viewModel.onSearchQueryChanged("iced")
            assertEquals(
                listOf("Coffee"),
                viewModel
                    .content()
                    .dayGroups
                    .flatMap { it.rows }
                    .map { it.merchant },
            )

            viewModel.onSearchQueryChanged("music")
            assertEquals(
                listOf("Spotify"),
                viewModel
                    .content()
                    .dayGroups
                    .flatMap { it.rows }
                    .map { it.merchant },
            )
        }

    @Test
    fun closingSearchClearsTheQuery() =
        runTest {
            val viewModel = viewModel(*everything)
            viewModel.onSearchOpened()
            viewModel.onSearchQueryChanged("coffee")

            viewModel.onSearchClosed()

            val search = viewModel.content().search
            assertFalse(search.isActive)
            assertEquals("", search.query)
        }
}
