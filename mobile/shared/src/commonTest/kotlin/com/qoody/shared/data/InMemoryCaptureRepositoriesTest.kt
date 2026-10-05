package com.qoody.shared.data

import com.qoody.shared.capture.UnparsedReason
import com.qoody.shared.domain.model.CapturedNotification
import com.qoody.shared.domain.model.Categorization
import com.qoody.shared.domain.model.Category
import com.qoody.shared.domain.model.EntrySource
import com.qoody.shared.domain.model.Money
import com.qoody.shared.domain.model.NewCapturedTransaction
import com.qoody.shared.domain.model.NewUnparsedCapture
import com.qoody.shared.domain.repository.UnparsedCaptureRepository
import com.qoody.shared.fixedDates
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant

class InMemoryCaptureRepositoriesTest {
    @Test
    fun capturedTransactionIsAddedOncePerDedupeKey() =
        runTest {
            val ledger = InMemoryLedgerRepository(fixedDates(), seed = emptyList())

            val id = ledger.addCaptured(capture("ref-1"))

            assertNotNull(id)
            assertNull(ledger.addCaptured(capture("ref-1")))
            val stored = ledger.transactions.first().single()
            assertEquals(EntrySource.Notification, stored.source)
            assertEquals(Categorization.Rule("swiggy"), stored.categorization)
            assertEquals(Instant.fromEpochMilliseconds(5_000), stored.occurredAt)
        }

    @Test
    fun unparsedCapturesAreDedupedCappedAndDismissible() =
        runTest {
            val repository = InMemoryUnparsedCaptureRepository()
            val total = UnparsedCaptureRepository.MAX_ENTRIES + 3

            repeat(total) { repository.add(unparsed("key-$it", it.toLong())) }
            repository.add(unparsed("key-0", 0))

            val stored = repository.captures.first()
            assertEquals(UnparsedCaptureRepository.MAX_ENTRIES, stored.size)
            assertEquals(Instant.fromEpochMilliseconds((total - 1).toLong()), stored.first().postedAt)

            repository.dismiss(stored.first().id)
            assertEquals(UnparsedCaptureRepository.MAX_ENTRIES - 1, repository.captures.first().size)
            repository.clear()
            assertEquals(emptyList(), repository.captures.first())
        }

    private fun capture(key: String) =
        NewCapturedTransaction(
            merchant = "Swiggy",
            amount = Money(25_000),
            occurredAt = Instant.fromEpochMilliseconds(5_000),
            category = Category.FoodAndDrink,
            categorization = Categorization.Rule("swiggy"),
            paymentApp = "Test Pay",
            paymentMethod = "UPI",
            referenceCode = key,
            notification = CapturedNotification("Test Pay", "Paid Rs 250 to Swiggy"),
            dedupeKey = key,
        )

    private fun unparsed(
        key: String,
        postedAtMillis: Long,
    ) = NewUnparsedCapture(
        packageName = "com.example.pay",
        appName = "Test Pay",
        title = "Payment",
        text = "Amount debited",
        postedAt = Instant.fromEpochMilliseconds(postedAtMillis),
        reason = UnparsedReason.NoAmount,
        dedupeKey = key,
    )
}
