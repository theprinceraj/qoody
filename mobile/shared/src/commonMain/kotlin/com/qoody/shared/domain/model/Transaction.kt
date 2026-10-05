package com.qoody.shared.domain.model

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

/** What the user enters to add an expense by hand. */
data class NewExpense(
    val merchant: String,
    val amount: Money,
    val category: Category,
)
