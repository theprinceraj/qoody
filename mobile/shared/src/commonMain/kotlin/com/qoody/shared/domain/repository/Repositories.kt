package com.qoody.shared.domain.repository

import com.qoody.shared.domain.model.AppSettings
import com.qoody.shared.domain.model.AppTheme
import com.qoody.shared.domain.model.Category
import com.qoody.shared.domain.model.Currency
import com.qoody.shared.domain.model.NewExpense
import com.qoody.shared.domain.model.Transaction
import com.qoody.shared.domain.model.TransactionId
import kotlinx.coroutines.flow.Flow

interface LedgerRepository {
    /** Settled transactions, newest first. Excluded entries are never emitted here. */
    val transactions: Flow<List<Transaction>>

    /** A single transaction in any status, or `null` when it does not exist. */
    fun observe(id: TransactionId): Flow<Transaction?>

    suspend fun add(expense: NewExpense): TransactionId

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
