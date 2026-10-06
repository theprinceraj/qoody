package com.qoody.shared.domain.model

import com.qoody.shared.capture.UnparsedReason
import kotlin.jvm.JvmInline
import kotlin.time.Instant

@JvmInline
value class TransactionId(
    val value: Long,
)

/** Who decided a transaction's [Category]. */
sealed interface Categorization {
    /** No category was assigned. */
    data object None : Categorization

    /** The user picked the category. */
    data object Manual : Categorization

    /** The on-device model picked the category with the given confidence. */
    data class Model(
        val modelName: String,
        val confidence: Permille,
    ) : Categorization

    /** A keyword rule matched the merchant name; [ruleId] is the keyword that matched. */
    data class Rule(
        val ruleId: String,
    ) : Categorization

    /** The user chose this category for the same merchant before, and Qoody remembered it. */
    data object Remembered : Categorization
}

/** Where a transaction came from. */
enum class EntrySource {
    /** Parsed from a payment notification. */
    Notification,

    /** Typed in by the user. */
    Manual,
}

enum class EntryStatus {
    Settled,
    Excluded,
}

/** The push notification a transaction was parsed from. */
data class CapturedNotification(
    val appName: String,
    val text: String,
)

/** A single outflow in the ledger. */
data class Transaction(
    val id: TransactionId,
    val merchant: String,
    val amount: Money,
    val occurredAt: Instant,
    val category: Category,
    val categorization: Categorization,
    val paymentApp: String,
    val note: String = "",
    val tag: String? = null,
    val paymentMethod: String? = null,
    val referenceCode: String? = null,
    val notification: CapturedNotification? = null,
    val status: EntryStatus = EntryStatus.Settled,
    val source: EntrySource = EntrySource.Notification,
)

/** The facts of an entry the user can correct on its receipt. */
data class EntryDetails(
    val merchant: String,
    val amount: Money,
    val occurredAt: Instant,
    /** A new category to apply along with the correction; `null` keeps the current one. */
    val recategorised: Pair<Category, Categorization>? = null,
)

/** What the user enters to add an expense by hand. */
data class NewExpense(
    val merchant: String,
    val amount: Money,
    val category: Category,
    /** When it was paid; `null` means now. */
    val occurredAt: Instant? = null,
)

/** An expense read from a payment notification, ready to be stored. */
data class NewCapturedTransaction(
    val merchant: String,
    val amount: Money,
    val occurredAt: Instant,
    val category: Category,
    val categorization: Categorization,
    val paymentApp: String,
    val paymentMethod: String?,
    val referenceCode: String?,
    val notification: CapturedNotification,
    /** Identifies the payment across repeated announcements; a second capture with the same key is dropped. */
    val dedupeKey: String,
)

/** A notification that looked like a payment but could not be read, kept so the user can enter it by hand. */
data class UnparsedCapture(
    val id: Long,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val postedAt: Instant,
    val reason: UnparsedReason,
)

/** What is stored for a new [UnparsedCapture]. */
data class NewUnparsedCapture(
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val postedAt: Instant,
    val reason: UnparsedReason,
    /** Re-posts of the same notification share this key and are stored once. */
    val dedupeKey: String,
)
