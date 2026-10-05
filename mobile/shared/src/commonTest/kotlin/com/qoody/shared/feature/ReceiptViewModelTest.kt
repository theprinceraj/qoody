package com.qoody.shared.feature

import com.qoody.shared.ViewModelTest
import com.qoody.shared.data.InMemoryLedgerRepository
import com.qoody.shared.data.InMemoryMerchantCategoryRepository
import com.qoody.shared.data.InMemorySettingsRepository
import com.qoody.shared.domain.format.toReceiptCode
import com.qoody.shared.domain.model.Categorization
import com.qoody.shared.domain.model.Category
import com.qoody.shared.domain.model.EntryStatus
import com.qoody.shared.domain.model.Money
import com.qoody.shared.domain.model.TransactionId
import com.qoody.shared.feature.excluded.ExcludedEntriesUiState
import com.qoody.shared.feature.excluded.ExcludedEntriesViewModel
import com.qoody.shared.feature.receipt.ReceiptEvent
import com.qoody.shared.feature.receipt.ReceiptUiState
import com.qoody.shared.feature.receipt.ReceiptViewModel
import com.qoody.shared.fixedDates
import com.qoody.shared.transaction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ReceiptViewModelTest : ViewModelTest() {
    private val dates = fixedDates()
    private val coffee = transaction(0, Money.of(4, 50), Category.FoodAndDrink, "Coffee")
    private val groceries = transaction(2, Money.of(95, 50), Category.FoodAndDrink, "Groceries")
    private val ledger = InMemoryLedgerRepository(dates, listOf(coffee, groceries))
    private val merchantCategories = InMemoryMerchantCategoryRepository()

    private fun viewModel(id: TransactionId = coffee.id) =
        ReceiptViewModel(id, ledger, InMemorySettingsRepository(), dates, merchantCategories)

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
            // 4.50 of the 100.00 spent on food this month.
            assertEquals(45, state.categoryShare.share.value)
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
            val excluded = ExcludedEntriesViewModel(ledger, InMemorySettingsRepository(), dates)

            val entries = (excluded.uiState.latest() as ExcludedEntriesUiState.Content).entries
            assertEquals(listOf("Groceries"), entries.map { it.merchant })

            excluded.onRestore(groceries.id)

            assertTrue((excluded.uiState.latest() as ExcludedEntriesUiState.Content).entries.isEmpty())
            assertEquals(EntryStatus.Settled, ledger.observe(groceries.id).first()!!.status)
        }
}
