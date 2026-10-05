package com.qoody.shared.core

import com.qoody.shared.domain.model.Money
import com.qoody.shared.domain.model.Transaction
import com.qoody.shared.domain.model.sumMoneyOf
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

internal const val DAYS_PER_WEEK = 7

internal fun LocalDate.startOfMonth(): LocalDate = LocalDate(year, month, 1)

internal fun LocalDate.startOfPreviousMonth(): LocalDate = startOfMonth().minus(1, DateTimeUnit.MONTH)

internal fun LocalDate.lengthOfMonth(): Int {
    val start = startOfMonth()
    return start.daysUntil(start.plus(1, DateTimeUnit.MONTH))
}

internal fun LocalDate.endOfMonth(): LocalDate = startOfMonth().plus(lengthOfMonth() - 1, DateTimeUnit.DAY)

internal fun Transaction.localDate(zone: TimeZone): LocalDate = occurredAt.toLocalDateTime(zone).date

/** The transactions that happened on a day inside [range]. */
internal fun List<Transaction>.transactionsIn(
    range: ClosedRange<LocalDate>,
    zone: TimeZone,
): List<Transaction> = filter { it.localDate(zone) in range }

internal fun List<Transaction>.spentBetween(
    range: ClosedRange<LocalDate>,
    zone: TimeZone,
): Money = transactionsIn(range, zone).sumMoneyOf { it.amount }
