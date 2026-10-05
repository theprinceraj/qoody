package com.qoody.shared.feature.receipt

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

data class CategoryShare(
    val category: Category,
    val share: Permille,
)

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
        val categoryShare: CategoryShare,
        val isCategoryPickerOpen: Boolean,
    ) : ReceiptUiState
}
