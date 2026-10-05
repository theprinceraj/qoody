package com.qoody.shared.capture

import com.qoody.shared.domain.model.Money
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Bank debit SMS formats as they reach the SMS app's notification. Every fixture is synthetic: the
 * shape follows each bank's public alert format; amounts, accounts, references and numbers are made up.
 */
class BankSmsFormatsTest {
    private fun payment(text: String): ParsedPayment {
        val outcome = PaymentNotificationParser.parse(text)
        assertIs<ParseOutcome.Payment>(outcome, "expected a payment for: $text")
        return outcome.payment
    }

    @Test
    fun hdfcSentToWithBlockInstructions() {
        val parsed =
            payment(
                "Sent Rs.500.00 From HDFC Bank A/C *1234 To SWIGGY On 05/10/26 Ref 512345678901 " +
                    "Not You? Call 18001234567/SMS BLOCK UPI to 7300000000",
            )
        assertEquals(Money.of(500), parsed.amount)
        assertEquals("Swiggy", parsed.merchant)
        assertEquals("512345678901", parsed.referenceCode)
    }

    @Test
    fun sbiDebitedByWithoutCurrency() {
        val parsed =
            payment(
                "Dear UPI user A/C X1234 debited by 250.0 on date 05Oct26 trf to ZOMATO Refno 512345678902. " +
                    "If not u? call 1800000000. -SBI",
            )
        assertEquals(Money.of(250), parsed.amount)
        assertEquals("Zomato", parsed.merchant)
        assertEquals("512345678902", parsed.referenceCode)
    }

    @Test
    fun iciciPayeeBeforeCreditedAndNumberAfterTo() {
        val parsed =
            payment(
                "ICICI Bank Acct XX123 debited for Rs 1,299.00 on 05-Oct-26; AMAZON PAY credited. " +
                    "UPI:512345678903. Call 18000000000 for dispute. SMS BLOCK 123 to 9200000000.",
            )
        assertEquals(Money.of(1299), parsed.amount)
        assertEquals("Amazon Pay", parsed.merchant)
        assertEquals("512345678903", parsed.referenceCode)
    }

    @Test
    fun axisUpiPath() {
        val parsed =
            payment(
                "INR 80.00 debited A/c no. XX1234 05-10-26 10:00:00 UPI/P2M/512345678904/CHAI POINT " +
                    "Not you? SMS BLOCKALL 919900000000 - Axis Bank",
            )
        assertEquals(Money.of(80), parsed.amount)
        assertEquals("Chai Point", parsed.merchant)
        assertEquals("512345678904", parsed.referenceCode)
    }

    @Test
    fun kotakSentToVpa() {
        val parsed =
            payment(
                "Sent Rs.120.00 from Kotak Bank AC X1234 to rapido@ybl on 05-10-26.UPI Ref 512345678905. " +
                    "Not you, https://example.com/fraud",
            )
        assertEquals(Money.of(120), parsed.amount)
        assertEquals("Rapido", parsed.merchant)
    }

    @Test
    fun pnbWithOnlyBlockNumberHasNoMerchant() {
        val parsed =
            payment(
                "A/c XX1234 debited INR 450.00 Dt 05-10-26 thru UPI:512345678906. Bal INR 1,000.00 CR " +
                    "Not u? Fwd this SMS to 9200000001 to block UPI. -PNB",
            )
        assertEquals(Money.of(450), parsed.amount)
        assertEquals(null, parsed.merchant)
        assertEquals("512345678906", parsed.referenceCode)
    }

    @Test
    fun unionRsColonAmount() {
        val parsed =
            payment(
                "A/c *1234 Debited for Rs:999.00 on 05-10-2026 10:00:00 by Mob Bk ref no 512345678907 " +
                    "Avl Bal Rs:1000.00.If not you, Call 1800000002 -Union Bank of India",
            )
        assertEquals(Money.of(999), parsed.amount)
        assertEquals("512345678907", parsed.referenceCode)
    }

    @Test
    fun barodaTransferred() {
        val parsed =
            payment(
                "Rs.300.00 transferred from A/c ...1234 to:UPI/512345678908. Total Bal:Rs.1000.00CR. " +
                    "Avlbl Amt:Rs.1000.00(05-10-2026 10:00:00) - Bank of Baroda",
            )
        assertEquals(Money.of(300), parsed.amount)
        assertEquals("512345678908", parsed.referenceCode)
    }

    @Test
    fun canaraTowardsUpi() {
        val parsed =
            payment(
                "An amount of INR 60.00 has been DEBITED to your account XXX123 on 05/10/2026 towards UPI/BLINKIT. " +
                    "Total Avail.Bal INR 1,000.00",
            )
        assertEquals(Money.of(60), parsed.amount)
        assertEquals("Blinkit", parsed.merchant)
    }

    @Test
    fun creditSmsIsNotAnExpense() {
        assertEquals(
            ParseOutcome.NotAnExpense,
            PaymentNotificationParser.parse(
                "Your A/c XX1234 is credited with Rs.500.00 on 05-10-26 by UPI Ref 512345678909",
            ),
        )
    }
}
