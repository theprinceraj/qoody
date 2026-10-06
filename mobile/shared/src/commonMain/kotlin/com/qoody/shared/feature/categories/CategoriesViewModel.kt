package com.qoody.shared.feature.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qoody.shared.core.stateInViewModel
import com.qoody.shared.domain.model.Category
import com.qoody.shared.domain.model.CustomCategory
import com.qoody.shared.domain.repository.BudgetRepository
import com.qoody.shared.domain.repository.CategoryRepository
import com.qoody.shared.domain.repository.LedgerRepository
import com.qoody.shared.domain.repository.MerchantCategoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Emojis offered for a new category; a fixed set keeps the picker simple and every chip legible. */
val CategoryEmojis: List<String> =
    listOf(
        "🏷️",
        "🏠",
        "🧒",
        "🐾",
        "💊",
        "🏥",
        "🎓",
        "📚",
        "🎮",
        "🎬",
        "🎵",
        "🏋️",
        "✈️",
        "⛽",
        "🛠️",
        "🎁",
        "💇",
        "👕",
        "🍼",
        "🙏",
        "💼",
        "📱",
        "☕",
        "🍺",
    )

/** The add/edit sheet while it is open; [id] is `null` for a new category. */
data class CategoryEditor(
    val id: Long?,
    val name: String,
    val emoji: String,
    /** Set after a save attempt with a name another category already uses. */
    val isDuplicate: Boolean = false,
) {
    val canSave: Boolean get() = name.isNotBlank()
}

sealed interface CategoriesUiState {
    data object Loading : CategoriesUiState

    data class Content(
        val custom: List<CustomCategory>,
        val editor: CategoryEditor?,
        /** The category waiting for the user to confirm its deletion. */
        val pendingDelete: CustomCategory?,
    ) : CategoriesUiState
}

/** Settings → Categories: the user's own categories (D25). */
class CategoriesViewModel(
    private val categories: CategoryRepository,
    private val ledger: LedgerRepository,
    private val budgets: BudgetRepository,
    private val merchantCategories: MerchantCategoryRepository,
) : ViewModel() {
    private val editor = MutableStateFlow<CategoryEditor?>(null)
    private val pendingDelete = MutableStateFlow<CustomCategory?>(null)

    val uiState: StateFlow<CategoriesUiState> =
        combine(categories.custom, editor, pendingDelete) { custom, open, deleting ->
            CategoriesUiState.Content(custom, open, deleting)
        }.stateInViewModel(viewModelScope, CategoriesUiState.Loading)

    fun onAddRequested() {
        editor.value = CategoryEditor(id = null, name = "", emoji = CategoryEmojis.first())
    }

    fun onEditRequested(category: CustomCategory) {
        editor.value = CategoryEditor(id = category.id, name = category.name, emoji = category.emoji)
    }

    fun onNameChanged(name: String) =
        editor.update { it?.copy(name = name.take(CustomCategory.NAME_MAX_LENGTH), isDuplicate = false) }

    fun onEmojiSelected(emoji: String) = editor.update { it?.copy(emoji = emoji) }

    fun onEditorDismissed() {
        editor.value = null
    }

    fun onSave() {
        val draft = editor.value?.takeIf { it.canSave } ?: return
        viewModelScope.launch {
            val name = draft.name.trim()
            val existing = categories.custom.first()
            if (existing.any { it.id != draft.id && it.name.equals(name, ignoreCase = true) }) {
                editor.update { it?.copy(isDuplicate = true) }
                return@launch
            }
            if (draft.id == null) {
                categories.create(name, draft.emoji)
            } else {
                categories.update(CustomCategory(draft.id, name, draft.emoji))
            }
            editor.value = null
        }
    }

    fun onDeleteRequested(category: CustomCategory) {
        pendingDelete.value = category
    }

    fun onDeleteDismissed() {
        pendingDelete.value = null
    }

    /**
     * Moves the category's entries to Uncategorized and drops its budget and remembered merchants
     * before the definition, so an interrupted delete never leaves entries pointing at nothing.
     */
    fun onDeleteConfirmed() {
        val category = pendingDelete.value ?: return
        pendingDelete.value = null
        viewModelScope.launch {
            ledger.recategorise(from = category.category, to = Category.Uncategorized)
            budgets.setBudget(category.category, null)
            merchantCategories.forget(category.category)
            categories.delete(category.id)
        }
    }
}
