package com.qoody.app.data

import android.content.Context
import android.util.Base64
import androidx.room3.withWriteTransaction
import com.qoody.shared.core.DateProvider
import com.qoody.shared.data.InMemorySettingsRepository
import com.qoody.shared.domain.model.AppSettings
import com.qoody.shared.domain.model.AppTheme
import com.qoody.shared.domain.model.Categorization
import com.qoody.shared.domain.model.Category
import com.qoody.shared.domain.model.Currency
import com.qoody.shared.domain.model.EntrySource
import com.qoody.shared.domain.model.EntryStatus
import com.qoody.shared.domain.model.NewCapturedTransaction
import com.qoody.shared.domain.model.NewExpense
import com.qoody.shared.domain.model.NewUnparsedCapture
import com.qoody.shared.domain.model.Transaction
import com.qoody.shared.domain.model.TransactionId
import com.qoody.shared.domain.model.UnparsedCapture
import com.qoody.shared.domain.repository.LedgerRepository
import com.qoody.shared.domain.repository.SettingsRepository
import com.qoody.shared.domain.repository.UnparsedCaptureRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

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

    override suspend fun exclude(id: TransactionId) = modify(id) { it.copy(status = EntryStatus.Excluded) }

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

class EncryptedKeyStore(
    context: Context,
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    fun read(): String? = preferences.getString(API_KEY_VALUE, null)?.let(::decrypt)

    fun write(value: String?) {
        preferences
            .edit()
            .apply {
                if (value == null) remove(API_KEY_VALUE) else putString(API_KEY_VALUE, encrypt(value))
            }.apply()
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val combined = cipher.iv + cipher.doFinal(value.encodeToByteArray())
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String? =
        runCatching {
            val combined = Base64.decode(value, Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                key(),
                GCMParameterSpec(GCM_TAG_BITS, combined.copyOfRange(0, GCM_IV_BYTES)),
            )
            cipher.doFinal(combined.copyOfRange(GCM_IV_BYTES, combined.size)).decodeToString()
        }.getOrNull()

    private fun key(): SecretKey {
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing
        return KeyGenerator
            .getInstance(KEY_ALGORITHM, ANDROID_KEYSTORE)
            .apply {
                init(
                    android.security.keystore.KeyGenParameterSpec
                        .Builder(
                            KEY_ALIAS,
                            android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
                                android.security.keystore.KeyProperties.PURPOSE_DECRYPT,
                        ).setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                        .build(),
                )
            }.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALGORITHM = "AES"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_ALIAS = "qoody_api_key"
        const val PREFERENCES_NAME = "qoody_secure_preferences"
        const val API_KEY_VALUE = "llm_api_key"
        const val GCM_IV_BYTES = 12
        const val GCM_TAG_BITS = 128
    }
}

class RoomSettingsRepository(
    database: QoodyDatabase,
    private val keyStore: EncryptedKeyStore,
) : SettingsRepository {
    private val dao = database.settingsDao()
    private val apiKey = MutableStateFlow(keyStore.read())

    override val settings: Flow<AppSettings> =
        combine(dao.observe(), apiKey) { entity, key ->
            (entity?.let { decodeSettings(it.payload).toModel() } ?: InMemorySettingsRepository.defaultSettings).copy(
                llm =
                    (
                        entity?.let { decodeSettings(it.payload).toModel() }
                            ?: InMemorySettingsRepository.defaultSettings
                    ).llm.copy(apiKey = key),
            )
        }.distinctUntilChanged()

    override suspend fun completeOnboarding() = modify { it.copy(onboardingCompleted = true) }

    override suspend fun setNotificationListenerEnabled(enabled: Boolean) =
        modify {
            it.copy(notificationListenerEnabled = enabled)
        }

    override suspend fun setLlmApiKey(key: String?) {
        keyStore.write(key)
        apiKey.value = key
    }

    override suspend fun setCurrency(currency: Currency) = modify { it.copy(currency = currency) }

    override suspend fun setTheme(theme: AppTheme) = modify { it.copy(theme = theme) }

    override suspend fun setHapticsEnabled(enabled: Boolean) = modify { it.copy(hapticsEnabled = enabled) }

    private suspend fun modify(change: (AppSettings) -> AppSettings) {
        val current = settings.first()
        dao.upsert(SettingsEntity(payload = encodeSettings(change(current))))
    }
}
