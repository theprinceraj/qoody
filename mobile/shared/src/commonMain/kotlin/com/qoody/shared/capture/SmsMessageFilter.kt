package com.qoody.shared.capture

/**
 * Decides which SMS-app notifications may be bank alerts. Deliberately relaxed on the sender, because
 * a missed debit is what users notice most: only messages from a personal phone number are dropped.
 * Personal messages from saved contacts are kept out by requiring banking vocabulary in the text.
 */
object SmsMessageFilter {
    private val separators = Regex("[\\s()\\-]")
    private val phoneNumber = Regex("^\\+?\\d{10,13}$")
    private val bankVocabulary =
        Regex(
            "\\b(?:a/?c|acct|account|card|upi|vpa|ref(?:no)?|utr|imps|neft|rtgs|bank|txn|debit(?:ed)?)\\b",
            RegexOption.IGNORE_CASE,
        )

    /** Registered Indian SMS headers: optional 2-letter operator prefix, the sender, optional type suffix. */
    private val header = Regex("^(?:[A-Z]{2}-)?([A-Z0-9]{3,9})(?:-[A-Z])?$", RegexOption.IGNORE_CASE)

    fun mayBeBankAlert(
        sender: String,
        text: String,
    ): Boolean = !isPersonalNumber(sender) && bankVocabulary.containsMatchIn(text)

    /** `JD-HDFCBK-S` becomes `HDFCBK`; any other sender is returned trimmed. */
    fun senderLabel(sender: String): String {
        val trimmed = sender.trim()
        return header
            .matchEntire(trimmed)
            ?.groupValues
            ?.get(1)
            ?.uppercase() ?: trimmed
    }

    private fun isPersonalNumber(sender: String): Boolean = phoneNumber.matches(sender.replace(separators, ""))
}
