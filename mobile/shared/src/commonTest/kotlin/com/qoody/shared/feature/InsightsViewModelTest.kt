package com.qoody.shared.feature

import com.qoody.shared.ViewModelTest
import com.qoody.shared.data.InMemoryLedgerRepository
import com.qoody.shared.data.InMemorySettingsRepository
import com.qoody.shared.domain.model.Category
import com.qoody.shared.domain.model.Money
import com.qoody.shared.feature.insights.BucketKind
import com.qoody.shared.feature.insights.InsightsPeriod
import com.qoody.shared.feature.insights.InsightsUiState
import com.qoody.shared.feature.insights.InsightsViewModel
import com.qoody.shared.feature.ledger.TrendDirection
import com.qoody.shared.fixedDates
import com.qoody.shared.transaction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InsightsViewModelTest : ViewModelTest() {
    private val dates = fixedDates()

    // Today is Oct 24. Oct weeks: 1-7, 8-14, 15-21, 22-28, 29-31.
    private val ledger =
        listOf(
            transaction(0, Money.of(100), Category.FoodAndDrink), // Oct 24, week 4
            transaction(3, Money.of(50), Category.Shopping), // Oct 21, week 3
            transaction(10, Money.of(30), Category.FoodAndDrink), // Oct 14, week 2
            transaction(20, Money.of(20), Category.Transport), // Oct 4, week 1
            transaction(30, Money.of(300), Category.Bills), // Sep 24
        )

    private fun viewModel() =
        InsightsViewModel(InMemoryLedgerRepository(dates, ledger), InMemorySettingsRepository(), dates)

    private fun InsightsViewModel.content() = uiState.latest() as InsightsUiState.Content

    @Test
    fun thisMonthBucketsSpendIntoWeeksAndMarksTheCurrentOne() =
        runTest {
            val state = viewModel().content()

            assertEquals(Money.of(200), state.total)
            assertEquals(kotlinx.datetime.Month.OCTOBER, state.currentMonth)
            assertEquals(BucketKind.Week, state.bucketKind)
            assertEquals(listOf(20L, 30L, 50L, 100L, 0L).map { Money.of(it) }, state.buckets.map { it.amount })
            assertEquals(5, state.buckets.size)
            assertEquals(4, state.currentBucketNumber)
            assertEquals(listOf(200, 300, 500, 1_000, 0), state.buckets.map { it.height.value })
        }

    @Test
    fun categoriesAreRankedWithTheirShare() =
        runTest {
            val categories = viewModel().content().categories

            assertEquals(
                listOf(Category.FoodAndDrink, Category.Shopping, Category.Transport),
                categories.map { it.category },
            )
            assertEquals(listOf(650, 250, 100), categories.map { it.share.value })
        }

    @Test
    fun reflectionComparesWithTheSameStretchOfLastMonth() =
        runTest {
            val reflection = viewModel().content().reflection!!

            // 200.00 over the 24 days elapsed.
            assertEquals(Money.of(8, 33), reflection.averageDaily)
            val comparison = reflection.comparison!!
            assertEquals(TrendDirection.Lower, comparison.direction)
            assertEquals(Money.of(100), comparison.difference)
        }

    @Test
    fun lastMonthShowsItsOwnWeeksAndNoCurrentBucket() =
        runTest {
            val viewModel = viewModel()
            viewModel.onPeriodSelected(InsightsPeriod.LastMonth)

            val state = viewModel.content()

            assertEquals(Money.of(300), state.total)
            assertEquals(5, state.buckets.size) // September: 1-7 ... 29-30
            assertNull(state.currentBucketNumber)
            assertNull(state.reflection!!.comparison)
        }

    @Test
    fun allTimeUsesMonthlyBuckets() =
        runTest {
            val viewModel = viewModel()
            viewModel.onPeriodSelected(InsightsPeriod.AllTime)

            val state = viewModel.content()

            assertEquals(BucketKind.Month, state.bucketKind)
            assertEquals(listOf(Money.of(300), Money.of(200)), state.buckets.map { it.amount })
            assertEquals(2, state.currentBucketNumber)
        }

    @Test
    fun emptyLedgerHasNoReflection() =
        runTest {
            val empty =
                InsightsViewModel(InMemoryLedgerRepository(dates, emptyList()), InMemorySettingsRepository(), dates)

            val state = empty.content()

            assertEquals(Money.Zero, state.total)
            assertNull(state.reflection)
            assertEquals(emptyList(), state.categories)
        }
}
