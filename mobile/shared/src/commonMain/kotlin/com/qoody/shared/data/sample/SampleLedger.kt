package com.qoody.shared.data.sample

import com.qoody.shared.domain.model.CapturedNotification
import com.qoody.shared.domain.model.Categorization
import com.qoody.shared.domain.model.Category
import com.qoody.shared.domain.model.Money
import com.qoody.shared.domain.model.Permille
import com.qoody.shared.domain.model.Transaction
import com.qoody.shared.domain.model.TransactionId
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlin.time.Duration.Companion.minutes

/**
 * Placeholder ledger shown until real notification capture exists. Every entry is positioned relative
 * to "today" so the screens always have a realistic current month, last month and weekly history.
 * This is fixture data: it is replaced wholesale when persistence lands.
 */
object SampleLedger {
    const val FIRST_ID = 1000L
    private const val MODEL_NAME = "Qoody on-device v1"

    fun build(
        today: LocalDate,
        zone: TimeZone,
    ): List<Transaction> =
        seeds.mapIndexed { index, seed ->
            val date = today.minus(seed.daysAgo, DateTimeUnit.DAY)
            Transaction(
                id = TransactionId(FIRST_ID + seeds.size - index),
                merchant = seed.merchant,
                amount = seed.amount,
                occurredAt = date.atStartOfDayIn(zone) + (seed.hour * MINUTES_PER_HOUR + seed.minute).minutes,
                category = seed.category,
                categorization = seed.categorization,
                paymentApp = seed.paymentApp,
                note = seed.note,
                tag = seed.tag,
                paymentMethod = seed.paymentMethod,
                referenceCode = seed.referenceCode,
                notification = seed.notification,
            )
        }

    private const val MINUTES_PER_HOUR = 60

    private fun model(permille: Int) = Categorization.Model(MODEL_NAME, Permille(permille))

    private class Seed(
        val daysAgo: Int,
        val hour: Int,
        val minute: Int,
        val merchant: String,
        val amount: Money,
        val category: Category,
        val paymentApp: String,
        val categorization: Categorization = model(990),
        val note: String = "",
        val tag: String? = null,
        val paymentMethod: String? = null,
        val referenceCode: String? = null,
        val notification: CapturedNotification? = null,
    )

    private val seeds =
        listOf(
            Seed(
                daysAgo = 0,
                hour = 10,
                minute = 14,
                merchant = "Third Wave Coffee",
                amount = Money.of(4, 50),
                category = Category.FoodAndDrink,
                paymentApp = "GPay",
                categorization = model(994),
                note = "Iced Americano with hazelnut drip",
                paymentMethod = "UPI / HDFC Bank •• 4091",
                referenceCode = "429810481902",
                notification =
                    CapturedNotification(
                        appName = "Google Pay",
                        text =
                            "Paid $4.50 to Third Wave Coffee Roasters Pvt Ltd using UPI ID " +
                                "coffee@okhdfcbank. UPI Ref: 429810481902.",
                    ),
            ),
            Seed(
                daysAgo = 0,
                hour = 13,
                minute = 30,
                merchant = "Zomato",
                amount = Money.of(18, 20),
                category = Category.FoodAndDrink,
                paymentApp = "Apple Pay",
            ),
            Seed(
                daysAgo = 0,
                hour = 8,
                minute = 45,
                merchant = "Metro card top-up",
                amount = Money.of(15),
                category = Category.Transport,
                paymentApp = "Paytm",
            ),
            Seed(
                daysAgo = 1,
                hour = 4,
                minute = 0,
                merchant = "Spotify",
                amount = Money.of(10, 99),
                category = Category.Subscriptions,
                paymentApp = "HDFC auto-debit",
            ),
            Seed(
                daysAgo = 1,
                hour = 18,
                minute = 12,
                merchant = "Amazon",
                amount = Money.of(34, 40),
                category = Category.Shopping,
                paymentApp = "Amazon Pay",
            ),
            Seed(
                daysAgo = 1,
                hour = 20,
                minute = 0,
                merchant = "Transfer to Aarav",
                amount = Money.of(120),
                category = Category.Friends,
                paymentApp = "GPay",
                categorization = Categorization.Manual,
                tag = "rent share",
            ),
            Seed(
                daysAgo = 2,
                hour = 9,
                minute = 20,
                merchant = "Uber",
                amount = Money.of(12, 80),
                category = Category.Transport,
                paymentApp = "Apple Pay",
            ),
            Seed(
                daysAgo = 3,
                hour = 17,
                minute = 5,
                merchant = "Whole Foods",
                amount = Money.of(56, 30),
                category = Category.FoodAndDrink,
                paymentApp = "Debit card",
            ),
            Seed(
                daysAgo = 4,
                hour = 7,
                minute = 40,
                merchant = "City Power & Light",
                amount = Money.of(78),
                category = Category.Bills,
                paymentApp = "HDFC auto-debit",
            ),
            Seed(
                daysAgo = 5,
                hour = 8,
                minute = 15,
                merchant = "Blue Bottle",
                amount = Money.of(6, 40),
                category = Category.FoodAndDrink,
                paymentApp = "GPay",
            ),
            Seed(
                daysAgo = 6,
                hour = 15,
                minute = 50,
                merchant = "Nike",
                amount = Money.of(89),
                category = Category.Shopping,
                paymentApp = "Apple Pay",
            ),
            Seed(
                daysAgo = 8,
                hour = 12,
                minute = 10,
                merchant = "Chipotle",
                amount = Money.of(12, 75),
                category = Category.FoodAndDrink,
                paymentApp = "Apple Pay",
            ),
            Seed(
                daysAgo = 9,
                hour = 6,
                minute = 0,
                merchant = "Rent — Maple Street",
                amount = Money.of(450),
                category = Category.Bills,
                paymentApp = "HDFC auto-debit",
            ),
            Seed(
                daysAgo = 10,
                hour = 19,
                minute = 25,
                merchant = "Uber",
                amount = Money.of(21, 50),
                category = Category.Transport,
                paymentApp = "Apple Pay",
            ),
            Seed(
                daysAgo = 11,
                hour = 4,
                minute = 0,
                merchant = "Netflix",
                amount = Money.of(15, 49),
                category = Category.Subscriptions,
                paymentApp = "HDFC auto-debit",
            ),
            Seed(
                daysAgo = 13,
                hour = 18,
                minute = 30,
                merchant = "Trader Joe's",
                amount = Money.of(63, 10),
                category = Category.FoodAndDrink,
                paymentApp = "Debit card",
            ),
            Seed(
                daysAgo = 14,
                hour = 14,
                minute = 45,
                merchant = "H&M",
                amount = Money.of(42, 90),
                category = Category.Shopping,
                paymentApp = "Apple Pay",
            ),
            Seed(
                daysAgo = 16,
                hour = 8,
                minute = 50,
                merchant = "Metro card top-up",
                amount = Money.of(20),
                category = Category.Transport,
                paymentApp = "Paytm",
            ),
            Seed(
                daysAgo = 18,
                hour = 21,
                minute = 5,
                merchant = "Unknown merchant",
                amount = Money.of(9, 99),
                category = Category.Uncategorized,
                paymentApp = "Debit card",
                categorization = Categorization.None,
            ),
            Seed(
                daysAgo = 21,
                hour = 20,
                minute = 15,
                merchant = "Transfer to Aarav",
                amount = Money.of(80),
                category = Category.Friends,
                paymentApp = "GPay",
                categorization = Categorization.Manual,
            ),
            Seed(
                daysAgo = 24,
                hour = 13,
                minute = 0,
                merchant = "Zomato",
                amount = Money.of(24, 60),
                category = Category.FoodAndDrink,
                paymentApp = "Apple Pay",
            ),
            Seed(
                daysAgo = 27,
                hour = 16,
                minute = 35,
                merchant = "Amazon",
                amount = Money.of(29, 99),
                category = Category.Shopping,
                paymentApp = "Amazon Pay",
            ),
            Seed(
                daysAgo = 30,
                hour = 6,
                minute = 0,
                merchant = "Rent — Maple Street",
                amount = Money.of(450),
                category = Category.Bills,
                paymentApp = "HDFC auto-debit",
            ),
            Seed(
                daysAgo = 33,
                hour = 12,
                minute = 40,
                merchant = "Swiggy",
                amount = Money.of(17, 40),
                category = Category.FoodAndDrink,
                paymentApp = "GPay",
            ),
            Seed(
                daysAgo = 36,
                hour = 17,
                minute = 20,
                merchant = "Whole Foods",
                amount = Money.of(71, 20),
                category = Category.FoodAndDrink,
                paymentApp = "Debit card",
            ),
            Seed(
                daysAgo = 40,
                hour = 9,
                minute = 10,
                merchant = "Uber",
                amount = Money.of(18, 30),
                category = Category.Transport,
                paymentApp = "Apple Pay",
            ),
            Seed(
                daysAgo = 44,
                hour = 4,
                minute = 0,
                merchant = "Gym membership",
                amount = Money.of(29),
                category = Category.Subscriptions,
                paymentApp = "HDFC auto-debit",
            ),
            Seed(
                daysAgo = 48,
                hour = 15,
                minute = 55,
                merchant = "Walmart",
                amount = Money.of(94, 10),
                category = Category.Shopping,
                paymentApp = "Debit card",
            ),
            Seed(
                daysAgo = 52,
                hour = 7,
                minute = 40,
                merchant = "City Power & Light",
                amount = Money.of(74),
                category = Category.Bills,
                paymentApp = "HDFC auto-debit",
            ),
            Seed(
                daysAgo = 58,
                hour = 8,
                minute = 5,
                merchant = "Blue Bottle",
                amount = Money.of(5, 20),
                category = Category.FoodAndDrink,
                paymentApp = "GPay",
            ),
            Seed(
                daysAgo = 63,
                hour = 6,
                minute = 0,
                merchant = "Rent — Maple Street",
                amount = Money.of(450),
                category = Category.Bills,
                paymentApp = "HDFC auto-debit",
            ),
        )
}
