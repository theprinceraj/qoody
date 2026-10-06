package com.qoody.shared.feature.insights

import com.qoody.shared.domain.model.Category
import com.qoody.shared.domain.model.Money
import com.qoody.shared.domain.model.Permille
import com.qoody.shared.feature.ledger.TrendDirection
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month

enum class InsightsPeriod {
    ThisMonth,
    LastMonth,
    AllTime,
}

/** What a bar in the pace chart represents. */
enum class BucketKind {
    Week,
    Month,
}

data class SpendBucket(
    val start: LocalDate,
    val end: LocalDate,
    val amount: Money,
    /** Bar height relative to the tallest bucket. */
    val height: Permille,
    val isCurrent: Boolean,
)

data class CategorySpend(
    val category: Category,
    val amount: Money,
    val share: Permille,
)

/** Spend so far compared with the same stretch of last month. */
data class ReflectionComparison(
    val direction: TrendDirection,
    val difference: Money,
)

data class Reflection(
    val averageDaily: Money,
    val comparison: ReflectionComparison?,
)

sealed interface InsightsUiState {
    data object Loading : InsightsUiState

    data class Content(
        val period: InsightsPeriod,
        val currentMonth: Month,
        val total: Money,
        val bucketKind: BucketKind,
        val buckets: List<SpendBucket>,
        val categories: List<CategorySpend>,
        val reflection: Reflection?,
    ) : InsightsUiState {
        /** 1-based position of the highlighted bucket, or `null` when none is current. */
        val currentBucketNumber: Int?
            get() = buckets.indexOfFirst { it.isCurrent }.takeIf { it >= 0 }?.plus(1)
    }
}
