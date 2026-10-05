package com.qoody.shared.data

import com.qoody.shared.core.DateProvider
import com.qoody.shared.data.sample.SampleLedger
import com.qoody.shared.domain.model.Categorization
import com.qoody.shared.domain.model.Category
import com.qoody.shared.domain.model.EntryDetails
import com.qoody.shared.domain.model.EntrySource
import com.qoody.shared.domain.model.EntryStatus
import com.qoody.shared.domain.model.NewCapturedTransaction
import com.qoody.shared.domain.model.NewExpense
import com.qoody.shared.domain.model.Transaction
import com.qoody.shared.domain.model.TransactionId
import com.qoody.shared.domain.repository.LedgerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Temporary [LedgerRepository] that keeps everything in memory, seeded with [SampleLedger].
 * Replace with the persistent implementation; the interface is the contract the UI depends on.
 */
class InMemoryLedgerRepository(
    private val dates: DateProvider,
    seed: List<Transaction> = SampleLedger.build(dates.today(), dates.zone),
) : LedgerRepository {
    private val all = MutableStateFlow(seed)
    private val dedupeKeys = mutableSetOf<String>()

    override val transactions: Flow<List<Transaction>> =
        all.map { list ->
            list
                .filter { it.status == EntryStatus.Settled }
                .sortedByDescending { it.occurredAt }
        }

    override val excluded: Flow<List<Transaction>> =
        all.map { list ->
            list
                .filter { it.status == EntryStatus.Excluded }
                .sortedByDescending { it.occurredAt }
        }

    override fun observe(id: TransactionId): Flow<Transaction?> = all.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun add(expense: NewExpense): TransactionId {
        val id = nextId()
        val transaction =
            Transaction(
                id = id,
                merchant = expense.merchant,
                amount = expense.amount,
                occurredAt = dates.now(),
                category = expense.category,
                categorization =
                    if (expense.category ==
                        Category.Uncategorized
                    ) {
                        Categorization.None
                    } else {
                        Categorization.Manual
                    },
                paymentApp = MANUAL_ENTRY_SOURCE,
                source = EntrySource.Manual,
            )
        all.update { it + transaction }
        return id
    }

    override suspend fun addCaptured(capture: NewCapturedTransaction): TransactionId? {
        if (!dedupeKeys.add(capture.dedupeKey)) return null
        val id = nextId()
        all.update {
            it +
                Transaction(
                    id = id,
                    merchant = capture.merchant,
                    amount = capture.amount,
                    occurredAt = capture.occurredAt,
                    category = capture.category,
                    categorization = capture.categorization,
                    paymentApp = capture.paymentApp,
                    paymentMethod = capture.paymentMethod,
                    referenceCode = capture.referenceCode,
                    notification = capture.notification,
                    source = EntrySource.Notification,
                )
        }
        return id
    }

    private fun nextId() = TransactionId((all.value.maxOfOrNull { it.id.value } ?: SampleLedger.FIRST_ID) + 1)

    override suspend fun updateCategory(
        id: TransactionId,
        category: Category,
    ) = modify(id) { it.copy(category = category, categorization = Categorization.Manual) }

    override suspend fun updateNote(
        id: TransactionId,
        note: String,
    ) = modify(id) { it.copy(note = note) }

    override suspend fun updateDetails(
        id: TransactionId,
        details: EntryDetails,
    ) = modify(id) { transaction ->
        val corrected =
            transaction.copy(merchant = details.merchant, amount = details.amount, occurredAt = details.occurredAt)
        details.recategorised?.let { (category, categorization) ->
            corrected.copy(category = category, categorization = categorization)
        } ?: corrected
    }

    override suspend fun exclude(id: TransactionId) = modify(id) { it.copy(status = EntryStatus.Excluded) }

    override suspend fun restore(id: TransactionId) = modify(id) { it.copy(status = EntryStatus.Settled) }

    private fun modify(
        id: TransactionId,
        change: (Transaction) -> Transaction,
    ) {
        all.update { list -> list.map { if (it.id == id) change(it) else it } }
    }

    private companion object {
        /** Shown as the "via …" source for entries typed in by hand. */
        const val MANUAL_ENTRY_SOURCE = "Manual"
    }
}
