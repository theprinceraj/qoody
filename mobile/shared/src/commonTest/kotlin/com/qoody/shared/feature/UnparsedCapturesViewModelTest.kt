package com.qoody.shared.feature

import com.qoody.shared.ViewModelTest
import com.qoody.shared.capture.UnparsedReason
import com.qoody.shared.data.InMemoryUnparsedCaptureRepository
import com.qoody.shared.domain.model.NewUnparsedCapture
import com.qoody.shared.feature.capture.UnparsedCapturesUiState
import com.qoody.shared.feature.capture.UnparsedCapturesViewModel
import com.qoody.shared.fixedDates
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class UnparsedCapturesViewModelTest : ViewModelTest() {
    private val repository = InMemoryUnparsedCaptureRepository()
    private val viewModel by lazy { UnparsedCapturesViewModel(repository, fixedDates()) }

    private fun items() = (viewModel.uiState.latest() as UnparsedCapturesUiState.Content).items

    @Test
    fun showsNewestFirstWithTitleAndTextJoined() =
        runTest {
            repository.add(capture("a", "Bank alert", "Account debited", POSTED_AT))
            repository.add(capture("b", "", "Card used", POSTED_AT + 1))

            val items = items()
            assertEquals(listOf("Card used", "Bank alert\nAccount debited"), items.map { it.text })
            assertEquals(LocalDate(2026, 10, 5), items.last().date)
            assertEquals(LocalTime(8, 0), items.last().time)
        }

    @Test
    fun dismissingOrAddingManuallyRemovesTheEntry() =
        runTest {
            repository.add(capture("a", "", "Account debited", POSTED_AT))
            repository.add(capture("b", "", "Card used", POSTED_AT + 1))

            viewModel.onDismiss(items().first().id)
            assertEquals(1, items().size)
            viewModel.onAddedManually(items().first().id)
            assertEquals(emptyList(), items())
        }

    @Test
    fun clearAllEmptiesTheList() =
        runTest {
            repository.add(capture("a", "", "Account debited", POSTED_AT))

            viewModel.onClearAll()

            assertEquals(emptyList(), items())
        }

    private fun capture(
        key: String,
        title: String,
        text: String,
        postedAtMillis: Long,
    ) = NewUnparsedCapture(
        packageName = "com.example.pay",
        appName = "Test Pay",
        title = title,
        text = text,
        postedAt = Instant.fromEpochMilliseconds(postedAtMillis),
        reason = UnparsedReason.NoAmount,
        dedupeKey = key,
    )

    private companion object {
        /** 2026-10-05 08:00 UTC. */
        const val POSTED_AT = 1_791_187_200_000L
    }
}
