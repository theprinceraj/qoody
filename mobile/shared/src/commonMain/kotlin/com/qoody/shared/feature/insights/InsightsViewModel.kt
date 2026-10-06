package com.qoody.shared.feature.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qoody.shared.core.DAYS_PER_WEEK
import com.qoody.shared.core.DateProvider
import com.qoody.shared.core.endOfMonth
import com.qoody.shared.core.lengthOfMonth
import com.qoody.shared.core.localDate
import com.qoody.shared.core.spentBetween
import com.qoody.shared.core.startOfMonth
import com.qoody.shared.core.startOfPreviousMonth
import com.qoody.shared.core.stateInViewModel
import com.qoody.shared.core.transactionsIn
import com.qoody.shared.domain.model.Money
import com.qoody.shared.domain.model.Permille
import com.qoody.shared.domain.model.Transaction
import com.qoody.shared.domain.model.sumMoneyOf
import com.qoody.shared.domain.repository.LedgerRepository
import com.qoody.shared.feature.ledger.TrendDirection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.monthsUntil
import kotlinx.datetime.plus

/** The "all time" chart shows at most this many trailing months. */
private const val MAX_MONTH_BUCKETS = 6

class InsightsViewModel(
    ledger: LedgerRepository,
    private val dates: DateProvider,
) : ViewModel() {
    private val period = MutableStateFlow(InsightsPeriod.ThisMonth)

    val uiState: StateFlow<InsightsUiState> =
        combine(ledger.transactions, period) { transactions, period ->
            build(transactions, period, dates.today(), dates.zone)
        }.stateInViewModel(viewModelScope, InsightsUiState.Loading)

    fun onPeriodSelected(selected: InsightsPeriod) = period.update { selected }

    private fun build(
        transactions: List<Transaction>,
        period: InsightsPeriod,
        today: LocalDate,
        zone: TimeZone,
    ): InsightsUiState.Content {
        val thisMonth = today.startOfMonth()
        val lastMonth = today.startOfPreviousMonth()
        val inPeriod =
            when (period) {
                InsightsPeriod.ThisMonth -> transactions.transactionsIn(thisMonth..today, zone)
                InsightsPeriod.LastMonth -> transactions.transactionsIn(lastMonth..lastMonth.endOfMonth(), zone)
                InsightsPeriod.AllTime -> transactions
            }
        val total = inPeriod.sumMoneyOf { it.amount }

        val (kind, buckets) =
            when (period) {
                InsightsPeriod.AllTime -> BucketKind.Month to monthBuckets(inPeriod, today, zone)
                InsightsPeriod.ThisMonth -> BucketKind.Week to weekBuckets(inPeriod, thisMonth, today, zone)
                InsightsPeriod.LastMonth -> BucketKind.Week to weekBuckets(inPeriod, lastMonth, null, zone)
            }

        return InsightsUiState.Content(
            period = period,
            currentMonth = today.month,
            total = total,
            bucketKind = kind,
            buckets = buckets,
            categories = categoriesOf(inPeriod, total),
            reflection = reflectionOf(transactions, inPeriod, total, period, today, zone),
        )
    }

    private fun weekBuckets(
        inPeriod: List<Transaction>,
        monthStart: LocalDate,
        today: LocalDate?,
        zone: TimeZone,
    ): List<SpendBucket> {
        val monthEnd = monthStart.endOfMonth()
        val weekCount = (monthStart.lengthOfMonth() + DAYS_PER_WEEK - 1) / DAYS_PER_WEEK
        val ranges =
            (0 until weekCount).map { week ->
                val start = monthStart.plus(week * DAYS_PER_WEEK, DateTimeUnit.DAY)
                start to minOf(start.plus(DAYS_PER_WEEK - 1, DateTimeUnit.DAY), monthEnd)
            }
        return scaled(ranges, inPeriod, zone) { start, end -> today != null && today in start..end }
    }

    private fun monthBuckets(
        inPeriod: List<Transaction>,
        today: LocalDate,
        zone: TimeZone,
    ): List<SpendBucket> {
        val firstMonth = inPeriod.minOfOrNull { it.localDate(zone) }?.startOfMonth() ?: today.startOfMonth()
        val monthsSpan = firstMonth.monthsUntil(today.startOfMonth()) + 1
        val visible = minOf(monthsSpan, MAX_MONTH_BUCKETS)
        val ranges =
            (visible - 1 downTo 0).map { monthsAgo ->
                val start = today.startOfMonth().minus(monthsAgo, DateTimeUnit.MONTH)
                start to start.endOfMonth()
            }
        return scaled(ranges, inPeriod, zone) { start, end -> today in start..end }
    }

    private fun scaled(
        ranges: List<Pair<LocalDate, LocalDate>>,
        inPeriod: List<Transaction>,
        zone: TimeZone,
        isCurrent: (LocalDate, LocalDate) -> Boolean,
    ): List<SpendBucket> {
        val amounts = ranges.map { (start, end) -> inPeriod.spentBetween(start..end, zone) }
        val tallest = amounts.maxOrNull() ?: Money.Zero
        return ranges.mapIndexed { index, (start, end) ->
            SpendBucket(
                start = start,
                end = end,
                amount = amounts[index],
                height = Permille.of(amounts[index], tallest),
                isCurrent = isCurrent(start, end),
            )
        }
    }

    private fun categoriesOf(
        inPeriod: List<Transaction>,
        total: Money,
    ): List<CategorySpend> =
        inPeriod
            .groupBy { it.category }
            .map { (category, group) ->
                val amount = group.sumMoneyOf { it.amount }
                CategorySpend(category, amount, Permille.of(amount, total))
            }.sortedByDescending { it.amount }

    private fun reflectionOf(
        all: List<Transaction>,
        inPeriod: List<Transaction>,
        total: Money,
        period: InsightsPeriod,
        today: LocalDate,
        zone: TimeZone,
    ): Reflection? {
        if (inPeriod.isEmpty()) return null
        val firstDay = inPeriod.minOf { it.localDate(zone) }
        return when (period) {
            InsightsPeriod.ThisMonth -> {
                val elapsed = today.day
                val lastMonth = today.startOfPreviousMonth()
                val sameStretch = minOf(lastMonth.plus(elapsed - 1, DateTimeUnit.DAY), lastMonth.endOfMonth())
                val lastToDate = all.spentBetween(lastMonth..sameStretch, zone)
                Reflection(total.dividedBy(elapsed), comparisonOf(total, lastToDate))
            }

            InsightsPeriod.LastMonth -> {
                Reflection(total.dividedBy(today.startOfPreviousMonth().lengthOfMonth()), comparison = null)
            }

            InsightsPeriod.AllTime -> {
                Reflection(total.dividedBy(firstDay.daysUntil(today) + 1), comparison = null)
            }
        }
    }

    private fun comparisonOf(
        spent: Money,
        previous: Money,
    ): ReflectionComparison? {
        if (previous.isZero) return null
        val direction =
            when {
                spent < previous -> TrendDirection.Lower
                spent > previous -> TrendDirection.Higher
                else -> TrendDirection.Same
            }
        return ReflectionComparison(direction, (spent - previous).absolute)
    }
}
