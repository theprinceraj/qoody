package com.qoody.shared.domain.repository

import com.qoody.shared.domain.model.AppSettings
import com.qoody.shared.domain.model.AppTheme
import com.qoody.shared.domain.model.Category
import com.qoody.shared.domain.model.CustomCategory
import com.qoody.shared.domain.model.EntryDetails
import com.qoody.shared.domain.model.Money
import com.qoody.shared.domain.model.NewCapturedTransaction
import com.qoody.shared.domain.model.NewExpense
import com.qoody.shared.domain.model.NewUnparsedCapture
import com.qoody.shared.domain.model.Transaction
import com.qoody.shared.domain.model.TransactionId
import com.qoody.shared.domain.model.UnparsedCapture
import kotlinx.coroutines.flow.Flow

interface LedgerRepository {
    /** Settled transactions, newest first. Excluded entries are never emitted here. */
    val transactions: Flow<List<Transaction>>

    /** Entries the user excluded from the ledger, newest first. */
    val excluded: Flow<List<Transaction>>

    /** A single transaction in any status, or `null` when it does not exist. */
    fun observe(id: TransactionId): Flow<Transaction?>

    suspend fun add(expense: NewExpense): TransactionId

    /** Stores a captured payment, or returns `null` when one with the same dedupe key already exists. */
    suspend fun addCaptured(capture: NewCapturedTransaction): TransactionId?

    suspend fun updateCategory(
        id: TransactionId,
        category: Category,
    )

    suspend fun updateNote(
        id: TransactionId,
        note: String,
    )

    /** Corrects the merchant, amount or time of an entry, for example a bank SMS with no payee. */
    suspend fun updateDetails(
        id: TransactionId,
        details: EntryDetails,
    )

    /** Removes the entry from the ledger and every total without deleting it. */
    suspend fun exclude(id: TransactionId)

    /** Puts an excluded entry back into the ledger and its totals. */
    suspend fun restore(id: TransactionId)

    /** Moves every entry in [from], excluded ones included, to [to] with no categorisation source. */
    suspend fun recategorise(
        from: Category,
        to: Category,
    )
}

/** Monthly spending limits per category. */
interface BudgetRepository {
    /** The monthly limit of every category that has one. */
    val budgets: Flow<Map<Category, Money>>

    /** Sets [category]'s monthly limit, or removes it when [limit] is `null`. */
    suspend fun setBudget(
        category: Category,
        limit: Money?,
    )
}

/** Categories the user chose for merchants, applied to that merchant's future captures. */
interface MerchantCategoryRepository {
    /** The category last chosen for [merchant], or `null` when the user never chose one. */
    suspend fun categoryFor(merchant: String): Category?

    suspend fun remember(
        merchant: String,
        category: Category,
    )

    /** Drops every remembered merchant that points at [category]. */
    suspend fun forget(category: Category)
}

/** Categories the user made (D25). Built-in categories are fixed and not stored. */
interface CategoryRepository {
    /** Oldest first. */
    val custom: Flow<List<CustomCategory>>

    /** Stores a new category named [name] shown with [emoji]. */
    suspend fun create(
        name: String,
        emoji: String,
    ): CustomCategory

    /** Renames [category] or changes its emoji; its entries keep pointing at it. */
    suspend fun update(category: CustomCategory)

    /** Removes the definition only; the caller moves its entries, budget and memories first. */
    suspend fun delete(id: Long)
}

/** Notifications that looked like payments but could not be read. */
interface UnparsedCaptureRepository {
    /** Newest first. */
    val captures: Flow<List<UnparsedCapture>>

    /** Stores [capture] unless one with the same dedupe key exists; keeps only the most recent entries. */
    suspend fun add(capture: NewUnparsedCapture)

    suspend fun dismiss(id: Long)

    suspend fun clear()

    companion object {
        /** Older entries beyond this many are dropped so the list cannot grow without bound. */
        const val MAX_ENTRIES = 50
    }
}

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun completeOnboarding()

    suspend fun setNotificationListenerEnabled(enabled: Boolean)

    suspend fun setTheme(theme: AppTheme)

    suspend fun setHapticsEnabled(enabled: Boolean)
}
