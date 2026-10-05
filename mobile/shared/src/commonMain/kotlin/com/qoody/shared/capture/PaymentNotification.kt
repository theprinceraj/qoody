package com.qoody.shared.capture

import com.qoody.shared.domain.model.Money
import kotlin.time.Instant

/** A platform-neutral snapshot of one posted notification. */
data class PaymentNotification(
    /** Stable per-notification key from the platform; the same key is re-posted when an app updates it. */
    val key: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val postedAt: Instant,
) {
    /** Title and body joined, with whitespace collapsed. This is what the parser reads. */
    val combinedText: String
        get() = "$title $text".replace(WHITESPACE, " ").trim()

    private companion object {
        val WHITESPACE = Regex("\\s+")
    }
}

/** The fields recovered from a payment notification. */
data class ParsedPayment(
    val amount: Money,
    /** `null` when the notification does not name a payee. */
    val merchant: String?,
    val referenceCode: String?,
    val paymentMethod: String?,
)

/** Why a notification that looked like a payment could not be turned into a transaction. */
enum class UnparsedReason {
    /** It reads like a debit but contains no rupee amount. */
    NoAmount,

    /** The amount is zero or not a valid rupee value. */
    InvalidAmount,
}

/** What the parser decided about a notification. */
sealed interface ParseOutcome {
    /** A confident outflow. */
    data class Payment(
        val payment: ParsedPayment,
    ) : ParseOutcome

    /** Not an expense (credit, refund, OTP, reminder, promo, non-INR, ...). Safe to discard. */
    data object NotAnExpense : ParseOutcome

    /** Looks like an expense but could not be read; the user should see it to fix it by hand. */
    data class Unparsed(
        val reason: UnparsedReason,
    ) : ParseOutcome
}
