package com.qoody.shared.domain.format

import com.qoody.shared.domain.model.Money

private const val DECIMAL_SEPARATOR = '.'
private const val ZERO_DIGIT = '0'
private const val MAX_INTEGER_DIGITS = 9
private const val MINOR_UNITS_PER_MAJOR = 100L

/** Cleans and parses the amount a user types into the "add expense" field. */
object MoneyInput {
    /**
     * Keeps only digits and at most one decimal separator, limits the integer part to
     * [MAX_INTEGER_DIGITS] and the fraction to [Money.FRACTION_DIGITS] digits.
     */
    fun sanitize(raw: String): String {
        val separatorIndex = raw.indexOf(DECIMAL_SEPARATOR)
        val integerPart = (if (separatorIndex < 0) raw else raw.substring(0, separatorIndex)).filter(Char::isDigit)
        val fractionPart =
            if (separatorIndex < 0) null else raw.substring(separatorIndex + 1).filter(Char::isDigit)

        return buildString {
            append(integerPart.take(MAX_INTEGER_DIGITS))
            if (fractionPart != null) {
                append(DECIMAL_SEPARATOR)
                append(fractionPart.take(Money.FRACTION_DIGITS))
            }
        }
    }

    /** Parses sanitised input; `null` when empty or not a positive amount. */
    fun parse(sanitized: String): Money? {
        val parts = sanitized.takeIf { it.isNotEmpty() }?.split(DECIMAL_SEPARATOR)
        val major = parts?.first()?.ifEmpty { ZERO_DIGIT.toString() }?.toLongOrNull()
        val minor =
            parts
                ?.getOrNull(1)
                .orEmpty()
                .padEnd(Money.FRACTION_DIGITS, ZERO_DIGIT)
                .toLongOrNull()
        return if (major == null || minor == null) {
            null
        } else {
            Money(major * MINOR_UNITS_PER_MAJOR + minor).takeUnless { it.isZero }
        }
    }
}
