package com.qoody.shared.domain.model

import kotlin.jvm.JvmInline

private const val PERMILLE_PER_PERCENT = 10
private const val PERMILLE_SCALE = 1_000L
private const val ROUNDING_DIVISOR = 2L

/** A ratio in thousandths (1 permille = 0.1%), stored as an Int so formatting needs no floating point. */
@JvmInline
value class Permille(
    val value: Int,
) {
    /** The whole percentage, rounded half up. */
    val wholePercent: Int get() = (value + PERMILLE_PER_PERCENT / 2) / PERMILLE_PER_PERCENT

    /** The ratio as a 0..1 fraction, for layout use only (never for money maths). */
    val fraction: Float get() = value.toFloat() / PERMILLE_SCALE

    fun coerceAtMost(maximum: Permille): Permille = Permille(minOf(value, maximum.value))

    companion object {
        val Zero = Permille(0)

        /** 100%. */
        val Full = Permille(PERMILLE_SCALE.toInt())

        /** `part / total` rounded half up; zero when [total] is zero. */
        fun of(
            part: Money,
            total: Money,
        ): Permille {
            if (total.isZero) return Zero
            val scaled = part.minorUnits * PERMILLE_SCALE * ROUNDING_DIVISOR + total.minorUnits
            return Permille((scaled / (total.minorUnits * ROUNDING_DIVISOR)).toInt())
        }
    }
}
