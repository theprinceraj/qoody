package com.qoody.shared.capture

import com.qoody.shared.data.InMemoryLedgerRepository
import com.qoody.shared.data.InMemoryUnparsedCaptureRepository
import com.qoody.shared.domain.model.CapturedNotification
import com.qoody.shared.domain.model.Categorization
import com.qoody.shared.domain.model.Category
import com.qoody.shared.domain.model.EntrySource
import com.qoody.shared.domain.model.Money
import com.qoody.shared.fixedDates
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

// All notification texts are synthetic.
class CaptureNotificationUseCaseTest {
    private val ledger = InMemoryLedgerRepository(fixedDates(), seed = emptyList())
    private val unparsed = InMemoryUnparsedCaptureRepository()
    private val capture = CaptureNotificationUseCase(ledger, unparsed, unknownMerchant = { UNKNOWN })
    private val postedAt = Instant.fromEpochMilliseconds(1_759_650_000_000)

    @Test
    fun debitFromSupportedAppIsSavedWithRuleCategory() =
        runTest {
            val result = capture(notification("Paid ₹340.50 to Swiggy", title = "Payment successful"))

            assertIs<CaptureResult.Saved>(result)
            val saved = ledger.transactions.first().single()
            assertEquals("Swiggy", saved.merchant)
            assertEquals(Money.of(340, 50), saved.amount)
            assertEquals(Category.FoodAndDrink, saved.category)
            assertEquals(Categorization.Rule("swiggy"), saved.categorization)
            assertEquals(EntrySource.Notification, saved.source)
            assertEquals(postedAt, saved.occurredAt)
            assertEquals("Google Pay", saved.paymentApp)
            assertEquals(
                CapturedNotification("Google Pay", "Payment successful\nPaid ₹340.50 to Swiggy"),
                saved.notification,
            )
        }

    @Test
    fun unknownMerchantKeepsUncategorized() =
        runTest {
            capture(notification("Sent ₹1,200 to Asha Rao using UPI"))

            val saved = ledger.transactions.first().single()
            assertEquals(Category.Uncategorized, saved.category)
            assertEquals(Categorization.None, saved.categorization)
        }

    @Test
    fun paymentWithoutPayeeUsesTheUnknownMerchantName() =
        runTest {
            capture(notification("Rs.120.00 debited from a/c **1234 via UPI"))

            assertEquals(
                UNKNOWN,
                ledger.transactions
                    .first()
                    .single()
                    .merchant,
            )
        }

    @Test
    fun unsupportedAppIsIgnoredEntirely() =
        runTest {
            val result = capture(notification("Paid ₹340.50 to Swiggy", packageName = "com.example.chat"))

            assertEquals(CaptureResult.UnsupportedApp, result)
            assertTrue(ledger.transactions.first().isEmpty())
            assertTrue(unparsed.captures.first().isEmpty())
        }

    @Test
    fun creditsAndOtpsCreateNothing() =
        runTest {
            val texts =
                listOf(
                    "₹500 received from Asha Rao",
                    "Your OTP for payment of Rs 499 is 123456",
                    "Refund of Rs 250 credited to your account",
                )
            texts.forEach { assertEquals(CaptureResult.NotAnExpense, capture(notification(it))) }

            assertTrue(ledger.transactions.first().isEmpty())
            assertTrue(unparsed.captures.first().isEmpty())
        }

    @Test
    fun unreadableDebitGoesToTheFailedToParseList() =
        runTest {
            val result = capture(notification("Your account has been debited"))

            assertEquals(CaptureResult.Unparsed, result)
            val stored = unparsed.captures.first().single()
            assertEquals(UnparsedReason.NoAmount, stored.reason)
            assertEquals("Your account has been debited", stored.text)
            assertTrue(ledger.transactions.first().isEmpty())

            capture(notification("Your account has been debited"))
            assertEquals(1, unparsed.captures.first().size)
        }

    @Test
    fun bankAndUpiAppAnnouncingTheSameReferenceYieldOneEntry() =
        runTest {
            val upi = notification("Paid ₹499 to Swiggy. UPI Ref No 428910481902")
            val bank =
                notification(
                    "Rs.499.00 debited from a/c **1234 to VPA swiggy@icici (UPI Ref No 428910481902).",
                    packageName = "com.csam.icici.bank.imobile",
                    appName = "iMobile",
                    key = "bank-1",
                )

            assertIs<CaptureResult.Saved>(capture(upi))
            assertEquals(CaptureResult.Duplicate, capture(bank))
            assertEquals(1, ledger.transactions.first().size)
        }

    @Test
    fun repeatsWithoutReferenceMergeWithinTheBucketOnly() =
        runTest {
            val base = Instant.fromEpochSeconds(CaptureDedupe.BUCKET_SECONDS * BUCKET_INDEX)
            val text = "Paid ₹80 to Chai Point"

            assertIs<CaptureResult.Saved>(capture(notification(text, at = base)))
            assertEquals(CaptureResult.Duplicate, capture(notification(text, at = base + 30.seconds)))
            assertIs<CaptureResult.Saved>(capture(notification(text, at = base + 10.minutes)))
            assertEquals(2, ledger.transactions.first().size)
        }

    private fun notification(
        text: String,
        title: String = "",
        packageName: String = GOOGLE_PAY,
        appName: String = "Google Pay",
        key: String = "key-1",
        at: Instant = postedAt,
    ) = PaymentNotification(key, packageName, appName, title, text, at)

    private companion object {
        const val UNKNOWN = "Unknown merchant"
        const val GOOGLE_PAY = "com.google.android.apps.nbu.paisa.user"
        const val BUCKET_INDEX = 5_000_000L
    }
}
