package com.qoody.shared.capture

import com.qoody.shared.domain.model.Money
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

private fun payment(text: String): ParsedPayment {
    val outcome = PaymentNotificationParser.parse(text)
    assertIs<ParseOutcome.Payment>(outcome, "expected a payment for: $text")
    return outcome.payment
}

private fun assertNotExpense(text: String) =
    assertEquals(ParseOutcome.NotAnExpense, PaymentNotificationParser.parse(text), text)

// All fixtures are synthetic; none is a real bank message.
class PaymentNotificationParserTest {
    @Test
    fun upiAppPaidToMerchant() {
        val parsed = payment("Paid ₹340.50 to Blue Tokai Coffee")
        assertEquals(Money.of(340, 50), parsed.amount)
        assertEquals("Blue Tokai Coffee", parsed.merchant)
    }

    @Test
    fun sentToPerson() {
        val parsed = payment("Sent ₹1,200 to Asha Rao using UPI")
        assertEquals(Money.of(1200), parsed.amount)
        assertEquals("Asha Rao", parsed.merchant)
        assertEquals("UPI", parsed.paymentMethod)
    }

    @Test
    fun bankDebitAlertWithVpaAndReference() {
        val parsed =
            payment(
                "Rs.499.00 debited from a/c **1234 on 05-10-26 to VPA swiggy@icici (UPI Ref No 428910481902).",
            )
        assertEquals(Money.of(499), parsed.amount)
        assertEquals("Swiggy", parsed.merchant)
        assertEquals("428910481902", parsed.referenceCode)
        assertEquals("UPI", parsed.paymentMethod)
    }

    @Test
    fun debitedAndCreditedToPayeeIsStillAnExpense() {
        val parsed = payment("INR 250.00 debited from your account and credited to Rapido via UPI Ref 998877665544")
        assertEquals(Money.of(250), parsed.amount)
        assertEquals("Rapido", parsed.merchant)
    }

    @Test
    fun cardSpendAtMerchant() {
        val parsed = payment("Rs 2,499.00 spent on HDFC Bank Card x4091 at AMAZON PAY on 05-Oct. Avl limit Rs 50,000")
        assertEquals(Money.of(2499), parsed.amount)
        assertEquals("Amazon Pay", parsed.merchant)
        assertEquals("Card", parsed.paymentMethod)
    }

    @Test
    fun indianGroupingAndLargeAmounts() {
        assertEquals(Money.of(123_456, 78), payment("Paid Rs. 1,23,456.78 to Landlord").amount)
        assertEquals(Money.of(5), payment("Paid ₹5 to Chai Point").amount)
        assertEquals(Money.of(5, 10), payment("Paid ₹5.1 to Chai Point").amount)
    }

    @Test
    fun balanceFigureIsNotTheSpend() {
        val parsed = payment("Avl Bal Rs 9,000.00. Debited Rs 150.00 at Metro Station")
        assertEquals(Money.of(150), parsed.amount)
    }

    @Test
    fun youPaidNameBeforeAmount() {
        val parsed = payment("You paid Zomato ₹612")
        assertEquals(Money.of(612), parsed.amount)
        assertEquals("Zomato", parsed.merchant)
    }

    @Test
    fun missingPayeeStillYieldsPayment() {
        val parsed = payment("₹75 debited via UPI")
        assertEquals(Money.of(75), parsed.amount)
        assertNull(parsed.merchant)
    }

    @Test
    fun phoneNumberVpaKeepsDigits() {
        assertEquals("9876543210", payment("Paid ₹50 to 9876543210@ybl").merchant)
    }

    @Test
    fun creditsRefundsAndRemindersAreNotExpenses() {
        listOf(
            "You have received ₹500 from Asha",
            "Asha sent you ₹500",
            "Rs.500 credited to your a/c **1234",
            "Refund of ₹300 processed. Paid ₹300 earlier to Myntra",
            "Your payment of ₹999 to Netflix failed",
            "Payment of ₹999 is due on 10 Oct",
            "Rs 999 will be debited tomorrow for your mandate",
            "Payment pending: ₹120 to Cafe",
            "₹100 declined",
            "Use OTP 123456 to pay ₹500",
            "Asha requested ₹200",
            "Pay ₹499 and win big!",
            "Your balance is Rs 5,000",
        ).forEach(::assertNotExpense)
    }

    @Test
    fun nonRupeeAmountsAreIgnored() {
        assertNotExpense("Paid $12.99 to Spotify")
        assertNotExpense("Spent USD 40 at Airline")
    }

    @Test
    fun debitWithoutAnyAmountIsUnparsed() {
        assertEquals(
            ParseOutcome.Unparsed(UnparsedReason.NoAmount),
            PaymentNotificationParser.parse("Your account was debited. Check the app for details"),
        )
    }

    @Test
    fun zeroAmountIsUnparsed() {
        assertEquals(
            ParseOutcome.Unparsed(UnparsedReason.InvalidAmount),
            PaymentNotificationParser.parse("Paid ₹0.00 to Cafe"),
        )
    }

    @Test
    fun amountWithTooManyDecimalsIsNotGuessed() {
        assertEquals(
            ParseOutcome.Unparsed(UnparsedReason.NoAmount),
            PaymentNotificationParser.parse("Paid Rs 5.123 to Cafe"),
        )
    }

    @Test
    fun parseAmountIsExactIntegerMath() {
        assertEquals(Money.of(1, 5), PaymentNotificationParser.parseAmount("1.05"))
        assertEquals(Money.of(1, 50), PaymentNotificationParser.parseAmount("1.5"))
        assertEquals(Money.of(12_34_567), PaymentNotificationParser.parseAmount("12,34,567"))
        assertNull(PaymentNotificationParser.parseAmount("1234567890123"))
    }
}
