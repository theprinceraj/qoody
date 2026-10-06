package com.qoody.shared.domain.format

import com.qoody.shared.domain.model.Money

private const val LAST_GROUP_SIZE = 3
private const val GROUP_SIZE = 2
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
 * Formats [Money] in rupees for a monospaced, decimal-aligned ledger, with Indian digit grouping
 * (lakh and crore: `1,23,456.00`). Separators are fixed (not locale-driven) on purpose so that
 * columns of figures always line up the same way.
 */
object MoneyFormatter {
    const val SYMBOL = "₹"
    const val ISO_CODE = "INR"

    /** Digits only, grouped and with two decimals: `1,482.50`, `12,34,567.89`. */
    fun formatPlain(amount: Money): String {
        val magnitude = amount.absolute.minorUnits
        val major = (magnitude / MINOR_UNITS_PER_MAJOR).toString()
        val minor = (magnitude % MINOR_UNITS_PER_MAJOR).toString().padStart(Money.FRACTION_DIGITS, '0')
        return group(major) + DECIMAL_SEPARATOR + minor
    }

    /** Symbol plus the whole-rupee amount (rounded half up), for tight spaces such as chart labels: `₹1,483`. */
    fun formatWhole(amount: Money): String {
        val magnitude = amount.absolute.minorUnits
        val major = (magnitude + MINOR_UNITS_PER_MAJOR / 2) / MINOR_UNITS_PER_MAJOR
        return SYMBOL + group(major.toString())
    }

    /** Symbol plus digits, optionally with a leading outflow sign: `-₹4.50`. */
    fun format(
        amount: Money,
        sign: SignStyle = SignStyle.None,
    ): String =
        buildString {
            if (sign == SignStyle.Outflow) append(NEGATIVE_SIGN)
            append(SYMBOL)
            append(formatPlain(amount))
        }

    /** The last three digits form one group, every two before them another. */
    private fun group(digits: String): String {
        if (digits.length <= LAST_GROUP_SIZE) return digits
        val head = digits.dropLast(LAST_GROUP_SIZE)
        val tail = digits.takeLast(LAST_GROUP_SIZE)
        val headGroups =
            head
                .reversed()
                .chunked(GROUP_SIZE)
                .joinToString(GROUPING_SEPARATOR.toString())
                .reversed()
        return headGroups + GROUPING_SEPARATOR + tail
    }
}
