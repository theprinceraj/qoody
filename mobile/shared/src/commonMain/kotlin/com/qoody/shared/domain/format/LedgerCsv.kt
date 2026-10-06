package com.qoody.shared.domain.format

import com.qoody.shared.domain.model.Category
import com.qoody.shared.domain.model.Transaction
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private const val FIELD_SEPARATOR = ","
private const val RECORD_SEPARATOR = "\r\n"
private const val QUOTE = "\""
private const val ESCAPED_QUOTE = "\"\""
private val charactersRequiringQuotes = setOf(',', '"', '\n', '\r')

private val columnHeaders =
    listOf("id", "date", "time", "merchant", "amount", "category", "payment_app", "tag", "note")

/** Serialises the ledger as RFC 4180 CSV (plain text, one receipt per row). */
object LedgerCsv {
    /** [customNames] maps the user's own categories to their names; built-ins use their keys. */
    fun build(
        transactions: List<Transaction>,
        zone: TimeZone,
        customNames: Map<Category, String> = emptyMap(),
    ): String =
        buildString {
            appendRecord(columnHeaders)
            transactions.forEach { transaction ->
                val local = transaction.occurredAt.toLocalDateTime(zone)
                appendRecord(
                    listOf(
                        transaction.id.value.toString(),
                        local.date.toString(),
                        local.time.toString(),
                        transaction.merchant,
                        MoneyFormatter.formatPlain(transaction.amount),
                        customNames[transaction.category] ?: transaction.category.key,
                        transaction.paymentApp,
                        transaction.tag.orEmpty(),
                        transaction.note,
                    ),
                )
            }
        }

    private fun StringBuilder.appendRecord(fields: List<String>) {
        append(fields.joinToString(FIELD_SEPARATOR, transform = ::escape))
        append(RECORD_SEPARATOR)
    }

    private fun escape(field: String): String {
        val needsQuoting = field.any { it in charactersRequiringQuotes }
        return if (needsQuoting) QUOTE + field.replace(QUOTE, ESCAPED_QUOTE) + QUOTE else field
    }
}
