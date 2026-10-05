package com.qoody.app.data

import com.qoody.shared.domain.model.Categorization
import com.qoody.shared.domain.model.Category
import com.qoody.shared.domain.model.EntrySource
import com.qoody.shared.domain.model.EntryStatus
import com.qoody.shared.domain.model.Money
import com.qoody.shared.domain.model.Permille
import com.qoody.shared.domain.model.Transaction
import com.qoody.shared.domain.model.TransactionId
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Instant

class PersistenceCodecTest {
    @Test
    fun transactionRoundTripsAllRestorableFields() {
        val transaction =
            Transaction(
                id = TransactionId(42),
                merchant = "Coffee",
                amount = Money(450),
                occurredAt = Instant.fromEpochMilliseconds(1_735_689_600_000),
                category = Category.FoodAndDrink,
                categorization = Categorization.Model("local", Permille(875)),
                paymentApp = "UPI",
                note = "Morning",
                tag = "work",
                paymentMethod = "Card",
                referenceCode = "ref-1",
                status = EntryStatus.Excluded,
                source = EntrySource.Notification,
            )

        assertEquals(transaction, decodeTransaction(encodeTransaction(transaction)))
    }

    @Test
    fun ruleCategorizationRoundTrips() {
        val transaction =
            Transaction(
                id = TransactionId(1),
                merchant = "Swiggy",
                amount = Money(25_000),
                occurredAt = Instant.fromEpochMilliseconds(0),
                category = Category.FoodAndDrink,
                categorization = Categorization.Rule("swiggy"),
                paymentApp = "Test Pay",
            )

        assertEquals(transaction, decodeTransaction(encodeTransaction(transaction)))
    }

    @Test
    fun rememberedCategorizationRoundTrips() {
        val transaction =
            Transaction(
                id = TransactionId(2),
                merchant = "Cafe",
                amount = Money(450),
                occurredAt = Instant.fromEpochMilliseconds(0),
                category = Category.FoodAndDrink,
                categorization = Categorization.Remembered,
                paymentApp = "Test Pay",
            )

        assertEquals(transaction, decodeTransaction(encodeTransaction(transaction)))
    }

    @Test
    fun backupSettingsNeverContainApiKey() {
        val settings = com.qoody.shared.data.InMemorySettingsRepository.defaultSettings
        val encoded = encodeSettings(settings.copy(llm = settings.llm.copy(apiKey = "secret")))

        assertEquals(false, encoded.contains("secret"))
    }
}
