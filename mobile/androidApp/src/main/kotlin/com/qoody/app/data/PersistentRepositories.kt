package com.qoody.app.data

import androidx.room3.withWriteTransaction
import com.qoody.shared.core.DateProvider
import com.qoody.shared.data.InMemorySettingsRepository
import com.qoody.shared.domain.model.AppSettings
import com.qoody.shared.domain.model.AppTheme
import com.qoody.shared.domain.model.Categorization
import com.qoody.shared.domain.model.Category
import com.qoody.shared.domain.model.Currency
import com.qoody.shared.domain.model.EntryDetails
import com.qoody.shared.domain.model.EntrySource
import com.qoody.shared.domain.model.EntryStatus
import com.qoody.shared.domain.model.MerchantKey
import com.qoody.shared.domain.model.Money
import com.qoody.shared.domain.model.NewCapturedTransaction
import com.qoody.shared.domain.model.NewExpense
import com.qoody.shared.domain.model.NewUnparsedCapture
import com.qoody.shared.domain.model.Transaction
import com.qoody.shared.domain.model.TransactionId
import com.qoody.shared.domain.model.UnparsedCapture
import com.qoody.shared.domain.repository.BudgetRepository
import com.qoody.shared.domain.repository.LedgerRepository
import com.qoody.shared.domain.repository.MerchantCategoryRepository
import com.qoody.shared.domain.repository.SettingsRepository
import com.qoody.shared.domain.repository.UnparsedCaptureRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class RoomLedgerRepository(
    private val database: QoodyDatabase,
    private val dates: DateProvider,
) : LedgerRepository {
    private val dao = database.transactionDao()

    override val transactions: Flow<List<Transaction>> =
        dao.observeAll().map { entities ->
            entities
                .map { decodeTransaction(it.payload) }
                .filter { it.status == EntryStatus.Settled }
                .sortedByDescending { it.occurredAt }
        }

    override val excluded: Flow<List<Transaction>> =
        dao.observeAll().map { entities ->
            entities
                .map { decodeTransaction(it.payload) }
                .filter { it.status == EntryStatus.Excluded }
                .sortedByDescending { it.occurredAt }
        }

    override fun observe(id: TransactionId): Flow<Transaction?> =
        dao.observe(id.value).map {
            it?.let { decodeTransaction(it.payload) }
        }

    override suspend fun add(expense: NewExpense): TransactionId =
        database.withWriteTransaction {
            val id = nextId()
            val transaction =
                Transaction(
                    id = id,
                    merchant = expense.merchant,
                    amount = expense.amount,
                    occurredAt = dates.now(),
                    category = expense.category,
                    categorization =
                        if (expense.category == Category.Uncategorized) {
                            Categorization.None
                        } else {
                            Categorization.Manual
                        },
                    paymentApp = MANUAL_ENTRY_SOURCE,
                    source = EntrySource.Manual,
                )
            dao.upsert(TransactionEntity(id.value, encodeTransaction(transaction)))
            id
        }

    override suspend fun addCaptured(capture: NewCapturedTransaction): TransactionId? =
        database.withWriteTransaction {
            if (dao.hasDedupeKey(capture.dedupeKey)) return@withWriteTransaction null
            val id = nextId()
            val transaction =
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
            dao.upsert(TransactionEntity(id.value, encodeTransaction(transaction), capture.dedupeKey))
            id
        }

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

    /** Only call inside a write transaction, so two writers cannot take the same id. */
    private suspend fun nextId() = TransactionId((dao.maxId() ?: 0L) + 1L)

    private suspend fun modify(
        id: TransactionId,
        change: (Transaction) -> Transaction,
    ) {
        database.withWriteTransaction {
            val current = dao.get(id.value) ?: return@withWriteTransaction
            dao.upsert(current.copy(payload = encodeTransaction(change(decodeTransaction(current.payload)))))
        }
    }

    private companion object {
        const val MANUAL_ENTRY_SOURCE = "Manual"
    }
}

class RoomUnparsedCaptureRepository(
    private val database: QoodyDatabase,
) : UnparsedCaptureRepository {
    private val dao = database.unparsedCaptureDao()

    override val captures: Flow<List<UnparsedCapture>> = dao.observeAll().map { list -> list.map { it.toModel() } }

    override suspend fun add(capture: NewUnparsedCapture) {
        database.withWriteTransaction {
            if (dao.hasDedupeKey(capture.dedupeKey)) return@withWriteTransaction
            dao.insert(capture.toEntity())
            dao.trimTo(UnparsedCaptureRepository.MAX_ENTRIES)
        }
    }

    override suspend fun dismiss(id: Long) = dao.delete(id)

    override suspend fun clear() = dao.deleteAll()
}

class RoomMerchantCategoryRepository(
    database: QoodyDatabase,
) : MerchantCategoryRepository {
    private val dao = database.merchantCategoryDao()

    override suspend fun categoryFor(merchant: String): Category? =
        dao.get(MerchantKey.of(merchant))?.let { entity -> Category.entries.firstOrNull { it.name == entity.category } }

    override suspend fun remember(
        merchant: String,
        category: Category,
    ) {
        val key = MerchantKey.of(merchant)
        if (key.isNotEmpty()) dao.upsert(MerchantCategoryEntity(key, category.name))
    }
}

class RoomBudgetRepository(
    database: QoodyDatabase,
) : BudgetRepository {
    private val dao = database.categoryBudgetDao()

    override val budgets: Flow<Map<Category, Money>> =
        dao.observeAll().map { entities ->
            entities
                .mapNotNull { entity ->
                    Category.entries.firstOrNull { it.name == entity.category }?.let { it to Money(entity.limitMinor) }
                }.toMap()
        }

    override suspend fun setBudget(
        category: Category,
        limit: Money?,
    ) {
        if (limit == null) {
            dao.delete(category.name)
        } else {
            dao.upsert(CategoryBudgetEntity(category.name, limit.minorUnits))
        }
    }
}

class RoomSettingsRepository(
    database: QoodyDatabase,
) : SettingsRepository {
    private val dao = database.settingsDao()

    /** Mirrors Android's notification-access state; set on every resume, never stored or backed up. */
    private val listenerEnabled = MutableStateFlow(false)

    override val settings: Flow<AppSettings> =
        combine(dao.observe(), listenerEnabled) { entity, listening ->
            val stored =
                entity?.let { decodeSettings(it.payload).toModel() } ?: InMemorySettingsRepository.defaultSettings
            stored.copy(notificationListenerEnabled = listening)
        }.distinctUntilChanged()

    override suspend fun completeOnboarding() = modify { it.copy(onboardingCompleted = true) }

    override suspend fun setNotificationListenerEnabled(enabled: Boolean) {
        listenerEnabled.value = enabled
    }

    override suspend fun setCurrency(currency: Currency) = modify { it.copy(currency = currency) }

    override suspend fun setTheme(theme: AppTheme) = modify { it.copy(theme = theme) }

    override suspend fun setHapticsEnabled(enabled: Boolean) = modify { it.copy(hapticsEnabled = enabled) }

    private suspend fun modify(change: (AppSettings) -> AppSettings) {
        val current = settings.first()
        dao.upsert(SettingsEntity(payload = encodeSettings(change(current))))
    }
}
