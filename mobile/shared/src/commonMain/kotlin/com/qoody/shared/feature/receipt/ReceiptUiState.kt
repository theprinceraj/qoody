package com.qoody.shared.feature.receipt

import com.qoody.shared.domain.format.MoneyInput
import com.qoody.shared.domain.model.BudgetProgress
import com.qoody.shared.domain.model.CapturedNotification
import com.qoody.shared.domain.model.Categorization
import com.qoody.shared.domain.model.Category
import com.qoody.shared.domain.model.Currency
import com.qoody.shared.domain.model.EntrySource
import com.qoody.shared.domain.model.EntryStatus
import com.qoody.shared.domain.model.Money
import com.qoody.shared.domain.model.Permille
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/** The "Edit entry" sheet's fields while it is open. */
data class EntryEditor(
    val amountInput: String,
    val merchant: String,
    val date: LocalDate,
    /** The latest date that may be picked: today, since an entry cannot happen in the future. */
    val maxDate: LocalDate,
) {
    val canSave: Boolean
        get() = MoneyInput.parse(amountInput) != null && merchant.isNotBlank() && date <= maxDate
}

/** What this payment means for its category's monthly budget. */
data class BudgetImpact(
    val category: Category,
    /** The category's spending this month against its budget (limit `null` when it has none). */
    val month: BudgetProgress,
) {
    /** This payment as a share of the budget; `null` without a budget. */
    fun paymentShare(amount: Money): Permille? = month.limit?.let { Permille.of(amount, it) }
}

sealed interface ReceiptUiState {
    data object Loading : ReceiptUiState

    data object NotFound : ReceiptUiState

    data class Content(
        val code: String,
        val merchant: String,
        val amount: Money,
        val currency: Currency,
        val date: LocalDate,
        val time: LocalTime,
        val paymentApp: String,
        val status: EntryStatus,
        val source: EntrySource,
        val category: Category,
        val categorization: Categorization,
        val note: String,
        val notification: CapturedNotification?,
        val paymentMethod: String?,
        val referenceCode: String?,
        /** `null` for uncategorized entries, which cannot have a budget. */
        val budgetImpact: BudgetImpact?,
        val isCategoryPickerOpen: Boolean,
        val editor: EntryEditor? = null,
    ) : ReceiptUiState {
        /** Whether there is a payment method, reference or budget impact to show; manual entries often have none. */
        val hasDetails: Boolean get() = paymentMethod != null || referenceCode != null || budgetImpact != null
    }
}
