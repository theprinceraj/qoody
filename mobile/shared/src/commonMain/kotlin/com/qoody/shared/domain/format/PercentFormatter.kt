package com.qoody.shared.domain.format

import com.qoody.shared.domain.model.Permille

private const val PERCENT_SIGN = '%'
private const val DECIMAL_SEPARATOR = '.'
private const val PERMILLE_PER_PERCENT = 10

object PercentFormatter {
    /** `36%` — rounded to a whole percent. */
    fun formatWhole(value: Permille): String = "${value.wholePercent}$PERCENT_SIGN"

    /** `99.4%` — one decimal place, exact because [Permille] is integral. */
    fun formatTenths(value: Permille): String {
        val whole = value.value / PERMILLE_PER_PERCENT
        val tenth = value.value % PERMILLE_PER_PERCENT
        return "$whole$DECIMAL_SEPARATOR$tenth$PERCENT_SIGN"
    }
}
