package com.qoody.shared.domain.format

import com.qoody.shared.domain.model.Currency
import com.qoody.shared.domain.model.Money

private const val GROUPING_SIZE = 3
private const val GROUPING_SEPARATOR = ','
private const val DECIMAL_SEPARATOR = '.'
private const val NEGATIVE_SIGN = '-'
private const val MINOR_UNITS_PER_MAJOR = 100L

enum class SignStyle {
    /** Magnitude only: `4.50`. */
    None,

    /** Outflow marker: `-4.50`. */
    Outflow,
}

/**
 * Formats [Money] for a monospaced, decimal-aligned ledger. Separators are fixed (not locale-driven)
 * on purpose so that columns of figures always line up the same way.
 */
object MoneyFormatter {
    /** Digits only, grouped and with two decimals: `1,482.50`. */
    fun formatPlain(amount: Money): String {
        val magnitude = amount.absolute.minorUnits
        val major = (magnitude / MINOR_UNITS_PER_MAJOR).toString()
        val minor = (magnitude % MINOR_UNITS_PER_MAJOR).toString().padStart(Money.FRACTION_DIGITS, '0')
        return group(major) + DECIMAL_SEPARATOR + minor
    }

    /** Symbol plus the whole-unit amount (rounded half up), for tight spaces such as chart labels: `$1,483`. */
    fun formatWhole(
        amount: Money,
        currency: Currency,
    ): String {
        val magnitude = amount.absolute.minorUnits
        val major = (magnitude + MINOR_UNITS_PER_MAJOR / 2) / MINOR_UNITS_PER_MAJOR
        return currency.symbol + group(major.toString())
    }

    /** Symbol plus digits, optionally with a leading outflow sign: `-$4.50`. */
    fun format(
        amount: Money,
        currency: Currency,
        sign: SignStyle = SignStyle.None,
    ): String =
        buildString {
            if (sign == SignStyle.Outflow) append(NEGATIVE_SIGN)
            append(currency.symbol)
            append(formatPlain(amount))
        }

    private fun group(digits: String): String =
        digits
            .reversed()
            .chunked(GROUPING_SIZE)
            .joinToString(GROUPING_SEPARATOR.toString())
            .reversed()
}
