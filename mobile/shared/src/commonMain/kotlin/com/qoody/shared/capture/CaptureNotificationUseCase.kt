package com.qoody.shared.capture

import com.qoody.shared.domain.model.CapturedNotification
import com.qoody.shared.domain.model.Categorization
import com.qoody.shared.domain.model.Category
import com.qoody.shared.domain.model.NewCapturedTransaction
import com.qoody.shared.domain.model.NewUnparsedCapture
import com.qoody.shared.domain.model.TransactionId
import com.qoody.shared.domain.repository.LedgerRepository
import com.qoody.shared.domain.repository.MerchantCategoryRepository
import com.qoody.shared.domain.repository.UnparsedCaptureRepository

/** What happened to one notification. */
sealed interface CaptureResult {
    /** The app is not on the allowlist; nothing was read or stored. */
    data object UnsupportedApp : CaptureResult

    /** Not an expense (credit, OTP, promo, personal SMS, ...); discarded. */
    data object NotAnExpense : CaptureResult

    data class Saved(
        val id: TransactionId,
    ) : CaptureResult

    /** The same payment was already recorded. */
    data object Duplicate : CaptureResult

    /** Looked like a payment but could not be read; stored in the "Failed to parse" list. */
    data object Unparsed : CaptureResult
}

/**
 * Allowlist → parse → categorise → dedupe → save, for one posted notification.
 *
 * @param unknownMerchant the name stored when a payment does not name its payee (a localised string).
 * @param appKind which kind of supported app a package is, or `null` when it is not supported.
 */
class CaptureNotificationUseCase(
    private val ledger: LedgerRepository,
    private val unparsed: UnparsedCaptureRepository,
    merchantCategories: MerchantCategoryRepository,
    private val unknownMerchant: () -> String,
    private val appKind: (String) -> AppKind? = CapturePolicy::kindOf,
) {
    private val categoriser = MerchantCategoriser(merchantCategories)

    suspend operator fun invoke(notification: PaymentNotification): CaptureResult {
        val kind = appKind(notification.packageName) ?: return CaptureResult.UnsupportedApp
        val outcome =
            if (kind == AppKind.Sms && !SmsMessageFilter.mayBeBankAlert(notification.title, notification.text)) {
                ParseOutcome.NotAnExpense
            } else {
                PaymentNotificationParser.parse(notification)
            }
        return when (outcome) {
            ParseOutcome.NotAnExpense -> CaptureResult.NotAnExpense
            is ParseOutcome.Unparsed -> storeUnparsed(notification, outcome.reason)
            is ParseOutcome.Payment -> save(notification, outcome.payment, kind)
        }
    }

    private suspend fun save(
        notification: PaymentNotification,
        payment: ParsedPayment,
        kind: AppKind,
    ): CaptureResult {
        val merchant = payment.merchant ?: unknownMerchant()
        val (category, categorization) = categoriser.categorise(payment.merchant)
        val id =
            ledger.addCaptured(
                NewCapturedTransaction(
                    merchant = merchant,
                    amount = payment.amount,
                    occurredAt = notification.postedAt,
                    category = category,
                    categorization = categorization,
                    paymentApp = paymentApp(notification, kind),
                    paymentMethod = payment.paymentMethod,
                    referenceCode = payment.referenceCode,
                    notification = CapturedNotification(notification.appName, notification.rawText),
                    dedupeKey = CaptureDedupe.paymentKey(notification, payment),
                ),
            )
        return if (id == null) CaptureResult.Duplicate else CaptureResult.Saved(id)
    }

    private suspend fun storeUnparsed(
        notification: PaymentNotification,
        reason: UnparsedReason,
    ): CaptureResult {
        unparsed.add(
            NewUnparsedCapture(
                packageName = notification.packageName,
                appName = notification.appName,
                title = notification.title,
                text = notification.text,
                postedAt = notification.postedAt,
                reason = reason,
                dedupeKey = CaptureDedupe.unparsedKey(notification),
            ),
        )
        return CaptureResult.Unparsed
    }

    /** For an SMS, the bank's sender id (`HDFCBK`) says more than "Google Messages". */
    private fun paymentApp(
        notification: PaymentNotification,
        kind: AppKind,
    ): String =
        if (kind == AppKind.Sms) {
            SmsMessageFilter.senderLabel(notification.title).ifBlank { notification.appName }
        } else {
            notification.appName
        }

    /** Title and body as the user saw them, one per line. */
    private val PaymentNotification.rawText: String
        get() = listOf(title, text).filter { it.isNotBlank() }.joinToString("\n")
}

/** Keys that identify the same payment, or the same unreadable notification, across repeats. */
object CaptureDedupe {
    /** Announcements of the same payment without a reference are merged within this window. */
    const val BUCKET_SECONDS = 300L

    private val nonAlphanumeric = Regex("[^a-z0-9]")

    /**
     * The bank or UPI reference when present (the bank and the UPI app quote the same one);
     * otherwise amount, normalised merchant and a 5-minute time bucket.
     */
    fun paymentKey(
        notification: PaymentNotification,
        payment: ParsedPayment,
    ): String {
        payment.referenceCode?.let { return "ref:${it.uppercase()}" }
        val merchant =
            payment.merchant
                .orEmpty()
                .lowercase()
                .replace(nonAlphanumeric, "")
        val bucket = notification.postedAt.epochSeconds / BUCKET_SECONDS
        return "amt:${payment.amount.minorUnits}|$merchant|$bucket"
    }

    /** The platform key plus the text, so an updated notification with new text is kept separately. */
    fun unparsedKey(notification: PaymentNotification): String =
        "${notification.key}|${notification.combinedText.hashCode()}"
}
