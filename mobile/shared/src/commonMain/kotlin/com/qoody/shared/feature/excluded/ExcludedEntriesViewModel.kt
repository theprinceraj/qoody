package com.qoody.shared.feature.excluded

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qoody.shared.core.DateProvider
import com.qoody.shared.core.stateInViewModel
import com.qoody.shared.domain.model.Category
import com.qoody.shared.domain.model.Money
import com.qoody.shared.domain.model.TransactionId
import com.qoody.shared.domain.repository.LedgerRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toLocalDateTime

/** One excluded entry, as listed in Settings → Excluded entries. */
data class ExcludedEntry(
    val id: TransactionId,
    val merchant: String,
    val amount: Money,
    val category: Category,
    val paymentApp: String,
    val date: LocalDate,
    val time: LocalTime,
)

sealed interface ExcludedEntriesUiState {
    data object Loading : ExcludedEntriesUiState

    data class Content(
        val entries: List<ExcludedEntry>,
    ) : ExcludedEntriesUiState
}

class ExcludedEntriesViewModel(
    private val ledger: LedgerRepository,
    private val dates: DateProvider,
) : ViewModel() {
    val uiState: StateFlow<ExcludedEntriesUiState> =
        ledger.excluded
            .map { excluded ->
                ExcludedEntriesUiState.Content(
                    entries =
                        excluded.map { transaction ->
                            val local = transaction.occurredAt.toLocalDateTime(dates.zone)
                            ExcludedEntry(
                                id = transaction.id,
                                merchant = transaction.merchant,
                                amount = transaction.amount,
                                category = transaction.category,
                                paymentApp = transaction.paymentApp,
                                date = local.date,
                                time = local.time,
                            )
                        },
                )
            }.stateInViewModel(viewModelScope, ExcludedEntriesUiState.Loading)

    fun onRestore(id: TransactionId) {
        viewModelScope.launch { ledger.restore(id) }
    }
}
