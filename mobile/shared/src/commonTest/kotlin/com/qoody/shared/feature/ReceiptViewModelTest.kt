package com.qoody.shared.feature

import com.qoody.shared.ViewModelTest
import com.qoody.shared.data.InMemoryBudgetRepository
import com.qoody.shared.data.InMemoryLedgerRepository
import com.qoody.shared.data.InMemoryMerchantCategoryRepository
import com.qoody.shared.data.InMemorySettingsRepository
import com.qoody.shared.domain.format.toReceiptCode
import com.qoody.shared.domain.model.Categorization
import com.qoody.shared.domain.model.Category
import com.qoody.shared.domain.model.EntryStatus
import com.qoody.shared.domain.model.Money
import com.qoody.shared.domain.model.NewExpense
import com.qoody.shared.domain.model.Permille
import com.qoody.shared.domain.model.TransactionId
import com.qoody.shared.feature.excluded.ExcludedEntriesUiState
import com.qoody.shared.feature.excluded.ExcludedEntriesViewModel
import com.qoody.shared.feature.receipt.ReceiptEvent
import com.qoody.shared.feature.receipt.ReceiptUiState
import com.qoody.shared.feature.receipt.ReceiptViewModel
import com.qoody.shared.fixedDates
import com.qoody.shared.testToday
import com.qoody.shared.transaction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

class ReceiptViewModelTest : ViewModelTest() {
    private val dates = fixedDates()
    private val coffee = transaction(0, Money.of(4, 50), Category.FoodAndDrink, "Coffee")
    private val groceries = transaction(2, Money.of(95, 50), Category.FoodAndDrink, "Groceries")
    private val ledger = InMemoryLedgerRepository(dates, listOf(coffee, groceries))
    private val merchantCategories = InMemoryMerchantCategoryRepository()
    private val budgets = InMemoryBudgetRepository()

    private fun viewModel(id: TransactionId = coffee.id) =
        ReceiptViewModel(id, ledger, dates, merchantCategories, budgets)

    private fun ReceiptViewModel.content() = uiState.latest() as ReceiptUiState.Content

    @Test
    fun exposesTheTransactionDetails() =
        runTest {
            val state = viewModel().content()

            assertEquals(coffee.id.toReceiptCode(), state.code)
            assertEquals("Coffee", state.merchant)
            assertEquals(Money.of(4, 50), state.amount)
            assertEquals(Category.FoodAndDrink, state.category)
            assertEquals(EntryStatus.Settled, state.status)
            // No budget yet: 100.00 spent on food this month, no share to show.
            assertEquals(Money.of(100), state.budgetImpact!!.month.spent)
            assertNull(state.budgetImpact.paymentShare(state.amount))
        }

    @Test
    fun budgetImpactUsesTheCategoryBudget() =
        runTest {
            budgets.setBudget(Category.FoodAndDrink, Money.of(90))

            val impact = viewModel().content().budgetImpact!!

            assertEquals(Permille(50), impact.paymentShare(Money.of(4, 50)))
            assertTrue(impact.month.isOver)
        }

    @Test
    fun aManualUncategorizedEntryHasNoDetailsToShow() =
        runTest {
            val manual = ledger.add(NewExpense("Street food", Money.of(60), Category.Uncategorized))

            val state = viewModel(manual).content()

            assertFalse(state.hasDetails)
            assertNull(state.notification)
            assertTrue(viewModel().content().hasDetails)
        }

    @Test
    fun unknownIdIsNotFound() =
        runTest {
            val state = viewModel(TransactionId(Long.MAX_VALUE)).uiState.latest()

            assertIs<ReceiptUiState.NotFound>(state)
        }

    @Test
    fun changingTheCategoryMarksItAsManual() =
        runTest {
            val viewModel = viewModel()
            viewModel.onCategoryPickerRequested()
            assertTrue(viewModel.content().isCategoryPickerOpen)

            viewModel.onCategorySelected(Category.Transport)

            val state = viewModel.content()
            assertEquals(Category.Transport, state.category)
            assertEquals(Categorization.Manual, state.categorization)
            assertFalse(state.isCategoryPickerOpen)
            assertEquals(Category.Transport, merchantCategories.categoryFor("COFFEE"))
        }

    @Test
    fun keepingTheEntrySavesTheNoteAndCloses() =
        runTest {
            val viewModel = viewModel()
            viewModel.onNoteChanged("Morning pour-over")

            viewModel.onKeepEntry()

            assertEquals(ReceiptEvent.Close, viewModel.events.first())
            assertEquals("Morning pour-over", ledger.observe(coffee.id).first()!!.note)
        }

    @Test
    fun editingCorrectsAmountMerchantAndDateButKeepsTheTimeOfDay() =
        runTest {
            val viewModel = viewModel()
            viewModel.onEditRequested()
            val editor = viewModel.content().editor!!
            assertEquals("4.50", editor.amountInput)
            assertEquals("Coffee", editor.merchant)
            assertEquals(testToday, editor.date)

            viewModel.onEditAmountChanged("12.75")
            viewModel.onEditMerchantChanged("Blue Tokai")
            viewModel.onEditDateChanged(testToday.minus(1, DateTimeUnit.DAY))
            viewModel.onEditSaved()

            val saved = ledger.observe(coffee.id).first()!!
            assertEquals(Money.of(12, 75), saved.amount)
            assertEquals("Blue Tokai", saved.merchant)
            assertEquals(coffee.occurredAt - 1.days, saved.occurredAt)
            assertNull(viewModel.content().editor)
        }

    @Test
    fun correctingTheMerchantOfAnUncategorizedEntryAppliesItsCategory() =
        runTest {
            val unknown = ledger.add(NewExpense("Unknown merchant", Money.of(5), Category.Uncategorized))
            merchantCategories.remember("Chai Point", Category.FoodAndDrink)
            val viewModel = viewModel(unknown)

            viewModel.onEditRequested()
            viewModel.content()
            viewModel.onEditMerchantChanged("Chai Point")
            viewModel.onEditSaved()

            val saved = ledger.observe(unknown).first()!!
            assertEquals(Category.FoodAndDrink, saved.category)
            assertEquals(Categorization.Remembered, saved.categorization)
        }

    @Test
    fun aDeliberateUncategorizedIsNotOverwritten() =
        runTest {
            val id = ledger.add(NewExpense("Unknown merchant", Money.of(5), Category.Uncategorized))
            ledger.updateCategory(id, Category.Uncategorized)
            merchantCategories.remember("Chai Point", Category.FoodAndDrink)
            val viewModel = viewModel(id)

            viewModel.onEditRequested()
            viewModel.content()
            viewModel.onEditMerchantChanged("Chai Point")
            viewModel.onEditSaved()

            assertEquals(Category.Uncategorized, ledger.observe(id).first()!!.category)
        }

    @Test
    fun correctingTheMerchantKeepsAnExistingCategory() =
        runTest {
            val viewModel = viewModel()
            viewModel.onEditRequested()
            viewModel.content()
            viewModel.onEditMerchantChanged("Uber")
            viewModel.onEditSaved()

            assertEquals(Category.FoodAndDrink, ledger.observe(coffee.id).first()!!.category)
        }

    @Test
    fun editorRejectsFutureDatesAndEmptyFields() =
        runTest {
            val viewModel = viewModel()
            viewModel.onEditRequested()

            viewModel.onEditDateChanged(testToday.plus(1, DateTimeUnit.DAY))
            assertFalse(viewModel.content().editor!!.canSave)
            viewModel.onEditDateChanged(testToday)
            viewModel.onEditMerchantChanged(" ")
            assertFalse(viewModel.content().editor!!.canSave)
            viewModel.onEditSaved()
            assertEquals("Coffee", ledger.observe(coffee.id).first()!!.merchant)

            viewModel.onEditDismissed()
            assertNull(viewModel.content().editor)
        }

    @Test
    fun excludingRemovesTheEntryFromTheLedgerAndCloses() =
        runTest {
            val viewModel = viewModel()

            viewModel.onExcludeFromLedger()

            assertEquals(ReceiptEvent.Close, viewModel.events.first())
            assertEquals(listOf(groceries.id), ledger.transactions.first().map { it.id })
            assertEquals(EntryStatus.Excluded, ledger.observe(coffee.id).first()!!.status)
        }

    @Test
    fun restoringAnExcludedEntryPutsItBackAndCloses() =
        runTest {
            ledger.exclude(coffee.id)
            assertEquals(listOf(coffee.id), ledger.excluded.first().map { it.id })
            val viewModel = viewModel()

            viewModel.onRestoreToLedger()

            assertEquals(ReceiptEvent.Close, viewModel.events.first())
            assertEquals(
                setOf(coffee.id, groceries.id),
                ledger.transactions
                    .first()
                    .map { it.id }
                    .toSet(),
            )
            assertTrue(ledger.excluded.first().isEmpty())
        }

    @Test
    fun excludedEntriesListShowsAndRestoresHiddenEntries() =
        runTest {
            ledger.exclude(groceries.id)
            val excluded = ExcludedEntriesViewModel(ledger, dates)

            val entries = (excluded.uiState.latest() as ExcludedEntriesUiState.Content).entries
            assertEquals(listOf("Groceries"), entries.map { it.merchant })

            excluded.onRestore(groceries.id)

            assertTrue((excluded.uiState.latest() as ExcludedEntriesUiState.Content).entries.isEmpty())
            assertEquals(EntryStatus.Settled, ledger.observe(groceries.id).first()!!.status)
        }
}
