package com.qoody.shared.domain.model

import kotlin.jvm.JvmInline
import kotlin.math.abs

private const val MINOR_UNITS_PER_MAJOR = 100L
private const val HALF_DIVISOR = 2

/**
 * An amount of money stored as an exact integer number of minor units (cents, paise).
 * Qoody is for India, so every amount is in rupees (paise as minor units).
 */
@JvmInline
value class Money(
    val minorUnits: Long,
) : Comparable<Money> {
    val isZero: Boolean get() = minorUnits == 0L

    val absolute: Money get() = Money(abs(minorUnits))

    operator fun plus(other: Money): Money = Money(minorUnits + other.minorUnits)

    operator fun minus(other: Money): Money = Money(minorUnits - other.minorUnits)

    /** Divides and rounds half up; [divisor] must be positive. */
    fun dividedBy(divisor: Int): Money = Money((minorUnits + divisor / HALF_DIVISOR) / divisor)

    override fun compareTo(other: Money): Int = minorUnits.compareTo(other.minorUnits)

    companion object {
        val Zero = Money(0L)

        const val FRACTION_DIGITS = 2

        /** Builds an amount from whole units and a minor part, e.g. `Money.of(4, 50)` is 4.50. */
        fun of(
            major: Long,
            minor: Long = 0L,
        ): Money = Money(major * MINOR_UNITS_PER_MAJOR + minor)
    }
}

/** Sums the [Money] produced by [selector] for every element. */
inline fun <T> Iterable<T>.sumMoneyOf(selector: (T) -> Money): Money {
    var total = Money.Zero
    for (element in this) total += selector(element)
    return total
}
