package com.qoody.app.data

import android.util.Base64
import androidx.annotation.VisibleForTesting
import androidx.room3.withWriteTransaction
import com.qoody.shared.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerializationException
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class BackupService(
    private val database: QoodyDatabase,
    private val settings: SettingsRepository,
) {
    suspend fun export(password: CharArray): ByteArray {
        require(password.isNotEmpty())
        val transactions = database.transactionDao().getAll().map { it.toRecord() }
        val unparsed = database.unparsedCaptureDao().getAll().map { it.toRecord() }
        val merchantCategories = database.merchantCategoryDao().getAll().associate { it.merchantKey to it.category }
        val budgets = database.categoryBudgetDao().getAll().associate { it.category to it.limitMinor }
        val payload =
            BackupPayload(
                exportedAtEpochMillis =
                    kotlin.time.Clock.System
                        .now()
                        .toEpochMilliseconds(),
                transactions = transactions,
                settings = settings.settings.first().toRecord(),
                unparsedCaptures = unparsed,
                merchantCategories = merchantCategories,
                budgets = budgets,
            )
        return encrypt(encodeBackup(payload), password)
    }

    suspend fun import(
        bytes: ByteArray,
        password: CharArray,
    ) {
        require(password.isNotEmpty())
        val payload = decodeBackup(decrypt(bytes, password))
        require(payload.formatVersion in SUPPORTED_BACKUP_FORMAT_VERSIONS) { "Unsupported backup version" }
        val restored = payload.transactions.map { it.toEntity() }
        val unparsed = payload.unparsedCaptures.map { it.toEntity() }
        val merchantCategories =
            payload.merchantCategories.map { (key, category) ->
                MerchantCategoryEntity(key, category)
            }
        val budgets = payload.budgets.map { (category, limit) -> CategoryBudgetEntity(category, limit) }
        val settingsEntity = SettingsEntity(payload = encodeSettings(payload.settings.toModel()))
        database.withWriteTransaction {
            database.transactionDao().deleteAll()
            database.transactionDao().upsertAll(restored)
            database.unparsedCaptureDao().deleteAll()
            database.unparsedCaptureDao().insertAll(unparsed)
            database.merchantCategoryDao().deleteAll()
            database.merchantCategoryDao().upsertAll(merchantCategories)
            database.categoryBudgetDao().deleteAll()
            database.categoryBudgetDao().upsertAll(budgets)
            database.settingsDao().upsert(settingsEntity)
        }
    }

    /** Encrypts an already-encoded [BackupPayload]; visible so tests can build backups of older formats. */
    @VisibleForTesting
    internal fun encrypt(
        plain: ByteArray,
        password: CharArray,
    ): ByteArray {
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(TAG_BITS, iv))
        val envelope =
            BackupEnvelope(
                salt = Base64.encodeToString(salt, Base64.NO_WRAP),
                iv = Base64.encodeToString(iv, Base64.NO_WRAP),
                ciphertext = Base64.encodeToString(cipher.doFinal(plain), Base64.NO_WRAP),
            )
        return encodeEnvelope(envelope)
    }

    @Suppress("ThrowsCount")
    private fun decrypt(
        bytes: ByteArray,
        password: CharArray,
    ): ByteArray {
        try {
            val envelope = decodeEnvelope(bytes)
            require(envelope.formatVersion == ENVELOPE_FORMAT_VERSION) { "Unsupported backup version" }
            val salt = Base64.decode(envelope.salt, Base64.NO_WRAP)
            val iv = Base64.decode(envelope.iv, Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(TAG_BITS, iv))
            return cipher.doFinal(Base64.decode(envelope.ciphertext, Base64.NO_WRAP))
        } catch (error: GeneralSecurityException) {
            throw invalidBackup(error)
        } catch (error: IllegalArgumentException) {
            throw invalidBackup(error)
        } catch (error: SerializationException) {
            throw invalidBackup(error)
        }
    }

    private fun invalidBackup(error: Throwable): GeneralSecurityException =
        GeneralSecurityException("The password is incorrect or the backup is damaged", error)

    private fun deriveKey(
        password: CharArray,
        salt: ByteArray,
    ): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_BITS)
        return SecretKeySpec(SecretKeyFactory.getInstance(PBKDF2_ALGORITHM).generateSecret(spec).encoded, KEY_ALGORITHM)
    }

    private companion object {
        val random = SecureRandom()
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
        const val KEY_ALGORITHM = "AES"
        const val SALT_BYTES = 16
        const val IV_BYTES = 12
        const val TAG_BITS = 128
        const val KEY_BITS = 256
        const val PBKDF2_ITERATIONS = 210_000
    }
}
