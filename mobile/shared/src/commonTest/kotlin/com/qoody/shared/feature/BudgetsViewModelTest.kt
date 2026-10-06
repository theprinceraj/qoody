package com.qoody.shared.feature

import com.qoody.shared.ViewModelTest
import com.qoody.shared.data.InMemoryBudgetRepository
import com.qoody.shared.data.InMemoryCategoryRepository
import com.qoody.shared.data.InMemoryLedgerRepository
import com.qoody.shared.data.InMemorySettingsRepository
import com.qoody.shared.domain.model.Category
import com.qoody.shared.domain.model.CustomCategory
import com.qoody.shared.domain.model.Money
import com.qoody.shared.domain.model.Permille
import com.qoody.shared.feature.budgets.BudgetsUiState
import com.qoody.shared.feature.budgets.BudgetsViewModel
import com.qoody.shared.feature.budgets.budgetableCategories
import com.qoody.shared.fixedDates
import com.qoody.shared.transaction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BudgetsViewModelTest : ViewModelTest() {
    private val dates = fixedDates()
    private val ledger =
        InMemoryLedgerRepository(
            dates,
            listOf(
                transaction(0, Money.of(30), Category.FoodAndDrink),
                transaction(1, Money.of(20), Category.FoodAndDrink),
                transaction(0, Money.of(90), Category.Transport),
                // Last month: must not count.
                transaction(40, Money.of(500), Category.FoodAndDrink),
            ),
        )
    private val budgets =
        InMemoryBudgetRepository(
            mapOf(
                Category.FoodAndDrink to Money.of(200),
                Category.Transport to Money.of(60),
            ),
        )
    private val pets = CustomCategory(id = 1, name = "Pets", emoji = "🐾")
    private val categories = InMemoryCategoryRepository(listOf(pets))
    private val viewModel by lazy { BudgetsViewModel(budgets, ledger, categories, dates) }

    private fun content() = viewModel.uiState.latest() as BudgetsUiState.Content

    private fun row(category: Category) = content().rows.single { it.category == category }.progress

    @Test
    fun showsThisMonthsSpendAgainstEachLimit() =
        runTest {
            assertEquals(budgetableCategories(listOf(pets)), content().rows.map { it.category })
            assertTrue(pets.category in content().rows.map { it.category })
            assertEquals(Money.of(50), row(Category.FoodAndDrink).spent)
            assertEquals(Permille(250), row(Category.FoodAndDrink).used)
            assertFalse(row(Category.FoodAndDrink).isOver)
            assertTrue(row(Category.Transport).isOver)
            assertNull(row(Category.Shopping).limit)
        }

    @Test
    fun settingEditingAndRemovingABudget() =
        runTest {
            viewModel.onEdit(Category.Shopping)
            assertEquals("", content().editor!!.amountInput)
            assertFalse(content().editor!!.canSave)
            viewModel.onAmountChanged("1,500.5")
            viewModel.onSave()
            assertEquals(Money.of(1500, 50), budgets.budgets.first()[Category.Shopping])
            assertNull(content().editor)

            viewModel.onEdit(Category.FoodAndDrink)
            assertEquals("200.00", content().editor!!.amountInput)
            assertTrue(content().editor!!.hasBudget)
            viewModel.onRemove()
            assertNull(budgets.budgets.first()[Category.FoodAndDrink])
        }
}
