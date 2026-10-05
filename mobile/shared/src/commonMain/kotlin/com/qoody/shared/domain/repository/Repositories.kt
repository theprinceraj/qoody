package com.qoody.shared.domain.repository

import com.qoody.shared.domain.model.AppSettings
import com.qoody.shared.domain.model.AppTheme
import com.qoody.shared.domain.model.Category
import com.qoody.shared.domain.model.Currency
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

    /** Removes the entry from the ledger and every total without deleting it. */
    suspend fun exclude(id: TransactionId)

    /** Puts an excluded entry back into the ledger and its totals. */
    suspend fun restore(id: TransactionId)
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

    suspend fun setLlmApiKey(key: String?)

    suspend fun setCurrency(currency: Currency)

    suspend fun setTheme(theme: AppTheme)

    suspend fun setHapticsEnabled(enabled: Boolean)
}

/** Checks whether an LLM API key is accepted by the provider. */
interface LlmKeyVerifier {
    /** `true` when [key] is valid. */
    suspend fun verify(key: String): Boolean
}
