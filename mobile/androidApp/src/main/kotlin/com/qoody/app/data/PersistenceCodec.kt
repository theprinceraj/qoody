package com.qoody.app.data

import com.qoody.shared.capture.UnparsedReason
import com.qoody.shared.domain.model.AppSettings
import com.qoody.shared.domain.model.AppTheme
import com.qoody.shared.domain.model.CapturedNotification
import com.qoody.shared.domain.model.Categorization
import com.qoody.shared.domain.model.Category
import com.qoody.shared.domain.model.Currency
import com.qoody.shared.domain.model.EntrySource
import com.qoody.shared.domain.model.EntryStatus
import com.qoody.shared.domain.model.Money
import com.qoody.shared.domain.model.NewUnparsedCapture
import com.qoody.shared.domain.model.Permille
import com.qoody.shared.domain.model.Transaction
import com.qoody.shared.domain.model.TransactionId
import com.qoody.shared.domain.model.UnparsedCapture
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Instant

private val json = Json { ignoreUnknownKeys = true }

@Serializable
data class TransactionRecord(
    val id: Long,
    val merchant: String,
    val amount: Long,
    val occurredAt: Long,
    val category: String,
    val categorizationKind: String,
    val modelName: String? = null,
    val confidence: Int? = null,
    val ruleId: String? = null,
    val paymentApp: String,
    val note: String,
    val tag: String? = null,
    val paymentMethod: String? = null,
    val referenceCode: String? = null,
    val notificationAppName: String? = null,
    val notificationText: String? = null,
    val status: String,
    val source: String,
    /** Added in backup format 2; absent in older backups and for manual entries. */
    val dedupeKey: String? = null,
)

@Serializable
data class UnparsedCaptureRecord(
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val postedAt: Long,
    val reason: String,
    val dedupeKey: String,
)

@Serializable
data class SettingsRecord(
    val onboardingCompleted: Boolean,
    val notificationListenerEnabled: Boolean,
    val monitoredAppCount: Int,
    val currency: String,
    val theme: String,
    val hapticsEnabled: Boolean,
)

@Serializable
data class BackupPayload(
    val formatVersion: Int = BACKUP_FORMAT_VERSION,
    val exportedAtEpochMillis: Long,
    val transactions: List<TransactionRecord>,
    val settings: SettingsRecord,
    /** Added in backup format 2. */
    val unparsedCaptures: List<UnparsedCaptureRecord> = emptyList(),
    /** Added in backup format 3: merchant key to category name. */
    val merchantCategories: Map<String, String> = emptyMap(),
    /** Added in backup format 4: category name to monthly limit in minor units. */
    val budgets: Map<String, Long> = emptyMap(),
)

@Serializable
data class BackupEnvelope(
    val formatVersion: Int = ENVELOPE_FORMAT_VERSION,
    val salt: String,
    val iv: String,
    val ciphertext: String,
)

fun Transaction.toRecord(): TransactionRecord =
    TransactionRecord(
        id = id.value,
        merchant = merchant,
        amount = amount.minorUnits,
        occurredAt = occurredAt.toEpochMilliseconds(),
        category = category.name,
        categorizationKind = categorization.kind,
        modelName = (categorization as? Categorization.Model)?.modelName,
        confidence = (categorization as? Categorization.Model)?.confidence?.value,
        ruleId = (categorization as? Categorization.Rule)?.ruleId,
        paymentApp = paymentApp,
        note = note,
        tag = tag,
        paymentMethod = paymentMethod,
        referenceCode = referenceCode,
        notificationAppName = notification?.appName,
        notificationText = notification?.text,
        status = status.name,
        source = source.name,
    )

fun TransactionRecord.toModel(): Transaction =
    Transaction(
        id = TransactionId(id),
        merchant = merchant,
        amount = Money(amount),
        occurredAt = Instant.fromEpochMilliseconds(occurredAt),
        category = enumValueOfOrDefault(category, Category.Uncategorized),
        categorization =
            when {
                categorizationKind == CATEGORIZATION_MODEL && modelName != null && confidence != null -> {
                    Categorization.Model(modelName, Permille(confidence))
                }

                categorizationKind == CATEGORIZATION_RULE && ruleId != null -> {
                    Categorization.Rule(ruleId)
                }

                categorizationKind == CATEGORIZATION_MANUAL -> {
                    Categorization.Manual
                }

                categorizationKind == CATEGORIZATION_REMEMBERED -> {
                    Categorization.Remembered
                }

                else -> {
                    Categorization.None
                }
            },
        paymentApp = paymentApp,
        note = note,
        tag = tag,
        paymentMethod = paymentMethod,
        referenceCode = referenceCode,
        notification =
            if (notificationAppName != null && notificationText != null) {
                CapturedNotification(notificationAppName, notificationText)
            } else {
                null
            },
        status = enumValueOfOrDefault(status, EntryStatus.Settled),
        source = enumValueOfOrDefault(source, EntrySource.Manual),
    )

fun TransactionEntity.toRecord(): TransactionRecord = decodeTransaction(payload).toRecord().copy(dedupeKey = dedupeKey)

fun TransactionRecord.toEntity(): TransactionEntity = TransactionEntity(id, encodeTransaction(toModel()), dedupeKey)

fun UnparsedCaptureEntity.toModel(): UnparsedCapture =
    UnparsedCapture(
        id = id,
        packageName = packageName,
        appName = appName,
        title = title,
        text = text,
        postedAt = Instant.fromEpochMilliseconds(postedAt),
        reason = enumValueOfOrDefault(reason, UnparsedReason.NoAmount),
    )

fun NewUnparsedCapture.toEntity(): UnparsedCaptureEntity =
    UnparsedCaptureEntity(
        packageName = packageName,
        appName = appName,
        title = title,
        text = text,
        postedAt = postedAt.toEpochMilliseconds(),
        reason = reason.name,
        dedupeKey = dedupeKey,
    )

fun UnparsedCaptureEntity.toRecord(): UnparsedCaptureRecord =
    UnparsedCaptureRecord(packageName, appName, title, text, postedAt, reason, dedupeKey)

fun UnparsedCaptureRecord.toEntity(): UnparsedCaptureEntity =
    UnparsedCaptureEntity(
        packageName = packageName,
        appName = appName,
        title = title,
        text = text,
        postedAt = postedAt,
        reason = reason,
        dedupeKey = dedupeKey,
    )

fun AppSettings.toRecord(): SettingsRecord =
    SettingsRecord(
        onboardingCompleted = onboardingCompleted,
        notificationListenerEnabled = false,
        monitoredAppCount = monitoredAppCount,
        currency = currency.name,
        theme = theme.name,
        hapticsEnabled = hapticsEnabled,
    )

fun SettingsRecord.toModel(): AppSettings =
    AppSettings(
        onboardingCompleted = onboardingCompleted,
        notificationListenerEnabled = false,
        monitoredAppCount = monitoredAppCount,
        currency = enumValueOfOrDefault(currency, Currency.Usd),
        theme = enumValueOfOrDefault(theme, AppTheme.WarmPaper),
        hapticsEnabled = hapticsEnabled,
    )

fun encodeTransaction(transaction: Transaction): String = json.encodeToString(transaction.toRecord())

fun decodeTransaction(payload: String): Transaction = json.decodeFromString<TransactionRecord>(payload).toModel()

fun encodeSettings(settings: AppSettings): String = json.encodeToString(settings.toRecord())

fun decodeSettings(payload: String): SettingsRecord = json.decodeFromString(payload)

fun encodeBackup(payload: BackupPayload): ByteArray = json.encodeToString(payload).encodeToByteArray()

fun decodeBackup(bytes: ByteArray): BackupPayload = json.decodeFromString(bytes.decodeToString())

fun encodeEnvelope(envelope: BackupEnvelope): ByteArray = json.encodeToString(envelope).encodeToByteArray()

fun decodeEnvelope(bytes: ByteArray): BackupEnvelope = json.decodeFromString(bytes.decodeToString())

private inline fun <reified T : Enum<T>> enumValueOfOrDefault(
    value: String,
    default: T,
): T = runCatching { enumValueOf<T>(value) }.getOrDefault(default)

private val Categorization.kind: String
    get() =
        when (this) {
            Categorization.None -> CATEGORIZATION_NONE
            Categorization.Manual -> CATEGORIZATION_MANUAL
            is Categorization.Model -> CATEGORIZATION_MODEL
            is Categorization.Rule -> CATEGORIZATION_RULE
            Categorization.Remembered -> CATEGORIZATION_REMEMBERED
        }

/**
 * Version of [BackupPayload]. 2 added dedupe keys and unparsed captures, 3 remembered merchant
 * categories, 4 category budgets, 5 dropped the language-model settings; older versions are still
 * importable (their extra settings fields are ignored).
 */
const val BACKUP_FORMAT_VERSION = 5
val SUPPORTED_BACKUP_FORMAT_VERSIONS = 1..BACKUP_FORMAT_VERSION

/** Version of the encryption [BackupEnvelope], independent of the payload inside it. */
const val ENVELOPE_FORMAT_VERSION = 1
private const val CATEGORIZATION_NONE = "none"
private const val CATEGORIZATION_MANUAL = "manual"
private const val CATEGORIZATION_MODEL = "model"
private const val CATEGORIZATION_RULE = "rule"
private const val CATEGORIZATION_REMEMBERED = "remembered"
