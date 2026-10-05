package com.qoody.shared.capture

import com.qoody.shared.domain.model.Money

/**
 * Rule-based reader for Indian bank and UPI debit notifications (INR only).
 *
 * Bias: a missed expense costs the user a manual entry, a phantom one corrupts their totals, so
 * anything that is not clearly an outflow is [ParseOutcome.NotAnExpense]. Pure and stateless.
 */
object PaymentNotificationParser {
    private const val MAX_WHOLE_DIGITS = 12
    private const val FRACTION_DIGITS = 2
    private const val BALANCE_LOOKBEHIND = 24
    private const val MAX_MERCHANT_LENGTH = 60
    private const val MIN_REFERENCE_LENGTH = 8
    private const val PHONE_DIGITS_MIN = 8

    private const val METHOD_UPI = "UPI"
    private const val METHOD_CARD = "Card"
    private const val METHOD_NET_BANKING = "Net banking"

    private val ignoreCase = setOf(RegexOption.IGNORE_CASE)

    private val debitCue =
        Regex(
            "\\b(?:debited|paid|payment of|sent|spent|purchase|withdrawn|txn of|transaction of|charged|" +
                "transferred|trf|debit of|dr\\.?\\s+from)\\b",
            ignoreCase,
        )

    /** "debited", or Bank of Baroda's "Dr. from A/C"; outranks a "credited to <payee>" in the same message. */
    private val debitedCue = Regex("\\b(?:debited|dr\\.?\\s+from)\\b", ignoreCase)
    private val hardReject =
        Regex(
            "\\b(?:refund(?:ed)?|revers(?:ed|al)|failed|failure|declined|unsuccessful|otp|requested|requests?|" +
                "reminder|due|will be debited|scheduled|cashback|received|pending)\\b",
            ignoreCase,
        )
    private val incomingCue = Regex("\\b(?:sent you|paid you|credited)\\b", ignoreCase)
    private val foreignAmount =
        Regex("(?:[$€£]|\\b(?:usd|eur|gbp|aed|sgd)\\b)\\s?\\d", ignoreCase)

    private val amount =
        Regex("(?:₹|\\brs[.:]?|\\binr[.:]?)\\s*(\\d[\\d,]*(?:\\.\\d{1,2})?)(?!\\d|\\.\\d)", ignoreCase)

    /** SBI-style "debited by 500.0", with no currency marker; only used when no rupee amount is found. */
    private val bareDebitAmount =
        Regex(
            "\\b(?:debited\\s+(?:by|for|with)|debit\\s+of)\\s+(\\d[\\d,]*(?:\\.\\d{1,2})?)(?!\\d|\\.\\d)",
            ignoreCase,
        )
    private val balanceWords = Regex("bal|limit|avl|available|outstanding", ignoreCase)

    private const val TERMINATOR =
        "(?=\\s(?:on|via|using|from|ref|refno|upi|txn|utr|with|for|at|bal|avl|is|was|has|not|if|dt|thru|total|" +
            "call|sms)\\b|[,;(\\n]|\\.(?:\\s|$)|$)"
    private val payee =
        Regex(
            "\\bto\\s+(?:vpa\\s+)?(?!your\\b|you\\b|a/c|ac\\b|account\\b|be\\b|the\\b)(.+?)$TERMINATOR",
            ignoreCase,
        )
    private val atPlace = Regex("\\bat\\s+(?!\\d)(.+?)$TERMINATOR", ignoreCase)
    private val paidName =
        Regex("\\bpaid\\s+(?!to\\b)(.+?)\\s+(?:₹|rs\\.?|inr)", ignoreCase)

    /** Axis-style "UPI/P2M/428910481902/SWIGGY". */
    private val upiPath =
        Regex("\\bupi/p2[amp]/\\d+/([^/\\n]+?)(?=/|\\s(?:not|sms|call)\\b|[.,;]|\\s-|$)", ignoreCase)

    /** ICICI-style "...; SWIGGY credited." */
    private val creditedName = Regex(";\\s*([^;.\\n]+?)\\s+credited\\b", ignoreCase)

    /** Canara-style "towards UPI/SWIGGY." */
    private val towards = Regex("\\btowards\\s+(?:upi/)?(.+?)$TERMINATOR", ignoreCase)

    /** Payee patterns, most reliable first. */
    private val payeePatterns by lazy { listOf(upiPath, payee, creditedName, atPlace, towards, paidName) }

    private val reference =
        Regex(
            "\\b(?:upi\\s*ref(?:erence)?(?:\\s*(?:no|number|id))?|ref(?:erence)?\\s*(?:no|number|id)?|" +
                "txn\\s*id|transaction\\s*id|utr)\\.?\\s*[:\\-]?\\s*([A-Za-z0-9]{$MIN_REFERENCE_LENGTH,})",
            ignoreCase,
        )

    private val upiReference = Regex("\\bupi\\s*[:/]\\s*(?:p2[amp]/)?(\\d{$MIN_REFERENCE_LENGTH,})", ignoreCase)

    /** The word UPI/VPA, or a UPI handle such as `name@oksbi`. */
    private val upiMarker = Regex("\\b(?:upi|vpa)\\b|[\\w.\\-]+@[a-z]{2,}\\b", ignoreCase)
    private val cardMarker = Regex("\\bcard\\b", ignoreCase)
    private val netBankingMarker = Regex("\\b(?:net ?banking|neft|imps|rtgs)\\b", ignoreCase)

    fun parse(notification: PaymentNotification): ParseOutcome = parse(notification.combinedText)

    fun parse(text: String): ParseOutcome = if (isOutflow(text)) readPayment(text) else ParseOutcome.NotAnExpense

    private fun isOutflow(text: String): Boolean =
        debitCue.containsMatchIn(text) &&
            !hardReject.containsMatchIn(text) &&
            (debitedCue.containsMatchIn(text) || !incomingCue.containsMatchIn(text))

    private fun readPayment(text: String): ParseOutcome {
        val match = firstSpendAmount(text) ?: bareDebitAmount.find(text) ?: return withoutAmount(text)
        val money = parseAmount(match.groupValues[1])
        return if (money == null || money.isZero) {
            ParseOutcome.Unparsed(UnparsedReason.InvalidAmount)
        } else {
            ParseOutcome.Payment(
                ParsedPayment(
                    amount = money,
                    merchant = merchantIn(text),
                    referenceCode = (reference.find(text) ?: upiReference.find(text))?.groupValues?.get(1),
                    paymentMethod = methodIn(text),
                ),
            )
        }
    }

    private fun withoutAmount(text: String): ParseOutcome =
        if (foreignAmount.containsMatchIn(text)) {
            ParseOutcome.NotAnExpense
        } else {
            ParseOutcome.Unparsed(UnparsedReason.NoAmount)
        }

    /** First rupee amount that is not a balance or limit figure. */
    private fun firstSpendAmount(text: String): MatchResult? =
        amount.findAll(text).firstOrNull { match ->
            val start = match.range.first
            val before = text.substring(maxOf(0, start - BALANCE_LOOKBEHIND), start)
            !balanceWords.containsMatchIn(before)
        }

    /** Integer-only parse of `1,23,456.78`; never goes through floating point. */
    internal fun parseAmount(raw: String): Money? {
        val parts = raw.replace(",", "").split('.')
        val whole = parts[0].takeIf { it.length in 1..MAX_WHOLE_DIGITS }?.toLongOrNull()
        val fraction =
            parts
                .getOrNull(1)
                .orEmpty()
                .padEnd(FRACTION_DIGITS, '0')
                .toLongOrNull()
        return if (whole == null || fraction == null) null else Money.of(whole, fraction)
    }

    /** The first payee candidate that reads as a name; bank SMS often add "SMS BLOCK to 92...", so numbers are skipped. */
    private fun merchantIn(text: String): String? =
        payeePatterns
            .asSequence()
            .flatMap { pattern -> pattern.findAll(text) }
            .map { it.groupValues[1] }
            .mapNotNull { raw -> cleanMerchant(raw)?.takeIf { '@' in raw || !it.startsWithPhoneNumber() } }
            .firstOrNull()

    /** "9200000001" or "9200000001 to block"; a phone-number VPA (`98...@ybl`) is a real payee and is kept. */
    private fun String.startsWithPhoneNumber(): Boolean {
        val first = substringBefore(' ')
        return first.length >= PHONE_DIGITS_MIN && first.all { it.isDigit() }
    }

    private fun cleanMerchant(raw: String): String? {
        val trimmed = raw.trim().trimEnd('.', ',', ':', '-', ' ')
        val name = if ('@' in trimmed) nameFromVpa(trimmed) else trimmed
        val readable = name.replace(Regex("\\s+"), " ").take(MAX_MERCHANT_LENGTH).trim()
        if (readable.isEmpty()) return null
        return titleCaseIfShouting(readable)
    }

    /** `swiggy.food@icici` becomes `swiggy food`, `shop0011-1@fbl` becomes `shop`; a phone-number VPA keeps its digits. */
    private fun nameFromVpa(vpa: String): String {
        val local = vpa.substringBefore('@')
        if (local.count { it.isDigit() } >= PHONE_DIGITS_MIN) return local
        val withoutNumericTail = local.replace(vpaNumericTail, "").ifEmpty { local }
        return withoutNumericTail.replace('.', ' ').replace('_', ' ').replace('-', ' ')
    }

    /** Digits and separators that UPI apps append to make a handle unique ("name0011-1"). */
    private val vpaNumericTail = Regex("[\\d._-]+$")

    private fun titleCaseIfShouting(value: String): String {
        val letters = value.filter { it.isLetter() }
        val uniform = letters.isNotEmpty() && (letters.all { it.isUpperCase() } || letters.all { it.isLowerCase() })
        if (!uniform) return value
        return value.split(' ').joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.uppercase() }
        }
    }

    private fun methodIn(text: String): String? =
        when {
            upiMarker.containsMatchIn(text) -> METHOD_UPI
            cardMarker.containsMatchIn(text) -> METHOD_CARD
            netBankingMarker.containsMatchIn(text) -> METHOD_NET_BANKING
            else -> null
        }
}
