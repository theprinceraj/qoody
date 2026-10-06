package com.qoody.shared.feature

import com.qoody.shared.ViewModelTest
import com.qoody.shared.data.InMemoryBudgetRepository
import com.qoody.shared.data.InMemoryCategoryRepository
import com.qoody.shared.data.InMemoryLedgerRepository
import com.qoody.shared.data.InMemoryMerchantCategoryRepository
import com.qoody.shared.domain.model.Categorization
import com.qoody.shared.domain.model.Category
import com.qoody.shared.domain.model.CustomCategory
import com.qoody.shared.domain.model.Money
import com.qoody.shared.domain.model.NewExpense
import com.qoody.shared.feature.categories.CategoriesUiState
import com.qoody.shared.feature.categories.CategoriesViewModel
import com.qoody.shared.feature.categories.CategoryEmojis
import com.qoody.shared.fixedDates
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CategoryTest {
    @Test
    fun keysRoundTripForBuiltInsAndCustomCategories() {
        assertEquals(Category.FoodAndDrink, Category.fromKey("FoodAndDrink"))
        assertEquals(Category.custom(7), Category.fromKey("custom:7"))
        assertEquals(7L, Category.custom(7).customId)
        assertTrue(Category.custom(7).isCustom)
        assertFalse(Category.Bills.isCustom)
        assertNull(Category.fromKey("Groceries"))
        assertNull(Category.fromKey("custom:x"))
    }

    @Test
    fun customCategoriesSitBetweenBuiltInsAndUncategorized() {
        val pets = CustomCategory(3, "Pets", "🐾")

        val all = Category.all(listOf(pets))

        assertEquals(Category.custom(3), all[all.size - 2])
        assertEquals(Category.Uncategorized, all.last())
        assertEquals(Category.builtIns.size + 1, all.size)
    }
}

class CategoriesViewModelTest : ViewModelTest() {
    private val dates = fixedDates()
    private val categories = InMemoryCategoryRepository()
    private val ledger = InMemoryLedgerRepository(dates, emptyList())
    private val budgets = InMemoryBudgetRepository()
    private val merchants = InMemoryMerchantCategoryRepository()

    // Lazy: a ViewModel must be created after the test installs the Main dispatcher.
    private val viewModel by lazy { CategoriesViewModel(categories, ledger, budgets, merchants) }

    private fun content() = viewModel.uiState.latest() as CategoriesUiState.Content

    private fun add(
        name: String,
        emoji: String = CategoryEmojis.first(),
    ) {
        viewModel.onAddRequested()
        viewModel.onNameChanged(name)
        viewModel.onEmojiSelected(emoji)
        viewModel.onSave()
    }

    @Test
    fun addingStoresATrimmedNameAndEmojiAndClosesTheSheet() =
        runTest {
            add("  Pets ", emoji = "🐾")

            assertEquals(listOf(CustomCategory(1, "Pets", "🐾")), content().custom)
            assertNull(content().editor)
        }

    @Test
    fun aBlankNameCannotBeSaved() =
        runTest {
            viewModel.onAddRequested()
            viewModel.onNameChanged("   ")

            assertFalse(content().editor!!.canSave)
            viewModel.onSave()
            assertEquals(emptyList(), content().custom)
        }

    @Test
    fun aDuplicateNameIsRejectedIgnoringCase() =
        runTest {
            add("Pets")
            add("pets")

            assertEquals(1, content().custom.size)
            assertTrue(content().editor!!.isDuplicate)
            viewModel.onNameChanged("Pet food")
            assertFalse(content().editor!!.isDuplicate)
        }

    @Test
    fun renamingKeepsTheIdSoEntriesFollow() =
        runTest {
            add("Pets")
            val pets = content().custom.single()

            viewModel.onEditRequested(pets)
            viewModel.onNameChanged("Pet care")
            viewModel.onEmojiSelected("💊")
            viewModel.onSave()

            assertEquals(listOf(CustomCategory(pets.id, "Pet care", "💊")), content().custom)
        }

    @Test
    fun deletingMovesEntriesAndDropsBudgetAndMemories() =
        runTest {
            add("Pets")
            val pets = content().custom.single()
            val entry = ledger.add(NewExpense("Vet", Money.of(900), pets.category))
            budgets.setBudget(pets.category, Money.of(2000))
            merchants.remember("Vet", pets.category)

            viewModel.onDeleteRequested(pets)
            assertEquals(pets, content().pendingDelete)
            viewModel.onDeleteConfirmed()

            assertEquals(emptyList(), content().custom)
            val moved = ledger.observe(entry).first()!!
            assertEquals(Category.Uncategorized, moved.category)
            assertEquals(Categorization.None, moved.categorization)
            assertEquals(emptyMap(), budgets.budgets.first())
            assertNull(merchants.categoryFor("Vet"))
        }

    @Test
    fun dismissingTheDeleteKeepsTheCategory() =
        runTest {
            add("Pets")
            viewModel.onDeleteRequested(content().custom.single())
            viewModel.onDeleteDismissed()

            assertNull(content().pendingDelete)
            assertEquals(1, content().custom.size)
        }
}
