package com.qoody.shared

import com.qoody.shared.core.DateProvider
import com.qoody.shared.domain.model.Categorization
import com.qoody.shared.domain.model.Category
import com.qoody.shared.domain.model.Money
import com.qoody.shared.domain.model.Transaction
import com.qoody.shared.domain.model.TransactionId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/** "Today" for every test: a Thursday late in a 31-day month, so weekly buckets are well defined. */
val testToday = LocalDate(2026, 10, 24)

val testZone = TimeZone.UTC

private val noon = testToday.atStartOfDayIn(testZone) + 12.hours

fun fixedDates(): DateProvider =
    DateProvider(
        clock =
            object : Clock {
                override fun now(): Instant = noon
            },
        zoneProvider = { testZone },
    )

private var nextId = 1L

fun transaction(
    daysAgo: Int,
    amount: Money,
    category: Category = Category.FoodAndDrink,
    merchant: String = "Merchant",
    hourOfDay: Int = 9,
    note: String = "",
    tag: String? = null,
): Transaction {
    val date = testToday.minus(daysAgo, DateTimeUnit.DAY)
    return Transaction(
        id = TransactionId(nextId++),
        merchant = merchant,
        amount = amount,
        occurredAt = date.atStartOfDayIn(testZone) + hourOfDay.hours,
        category = category,
        categorization = Categorization.Manual,
        paymentApp = "GPay",
        note = note,
        tag = tag,
    )
}

/**
 * Base class for ViewModel tests. Routes `viewModelScope` to an eager test dispatcher and offers
 * [latest], which reads a ViewModel's state the way a screen would: with an active subscriber.
 * (ViewModel state is shared with `WhileSubscribed`, so reading `.value` without one can be stale.)
 */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class ViewModelTest {
    private lateinit var collectors: CoroutineScope

    @BeforeTest
    fun installMainDispatcher() {
        val dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
        collectors = CoroutineScope(dispatcher + SupervisorJob())
    }

    @AfterTest
    fun removeMainDispatcher() {
        collectors.cancel()
        Dispatchers.resetMain()
    }

    /** Subscribes for the rest of the test and returns the up-to-date value. */
    protected fun <T> StateFlow<T>.latest(): T {
        collectors.launch { collect { } }
        return value
    }
}
