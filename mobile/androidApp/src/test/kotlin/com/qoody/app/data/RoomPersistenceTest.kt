package com.qoody.app.data

import android.app.Application
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import com.qoody.shared.capture.UnparsedReason
import com.qoody.shared.core.DateProvider
import com.qoody.shared.data.InMemorySettingsRepository
import com.qoody.shared.domain.model.CapturedNotification
import com.qoody.shared.domain.model.Categorization
import com.qoody.shared.domain.model.Category
import com.qoody.shared.domain.model.EntrySource
import com.qoody.shared.domain.model.Money
import com.qoody.shared.domain.model.NewCapturedTransaction
import com.qoody.shared.domain.model.NewExpense
import com.qoody.shared.domain.model.NewUnparsedCapture
import com.qoody.shared.domain.model.Transaction
import com.qoody.shared.domain.model.TransactionId
import com.qoody.shared.domain.repository.UnparsedCaptureRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import kotlin.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class RoomPersistenceTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val dates = DateProvider()
    private lateinit var database: QoodyDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder<QoodyDatabase>(context).setDriver(AndroidSQLiteDriver()).build()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun capturedPaymentIsStoredOncePerDedupeKey() =
        runTest {
            val ledger = RoomLedgerRepository(database, dates)

            val first = ledger.addCaptured(capture("ref-1"))
            val repeat = ledger.addCaptured(capture("ref-1").copy(merchant = "Other"))

            assertNotNull(first)
            assertNull(repeat)
            val stored = ledger.transactions.first().single()
            assertEquals("Swiggy", stored.merchant)
            assertEquals(EntrySource.Notification, stored.source)
            assertEquals(Categorization.Rule("swiggy"), stored.categorization)
            assertEquals("Paid Rs 250 to Swiggy", stored.notification?.text)
        }

    @Test
    fun concurrentWritersNeverShareAnId() =
        runTest {
            val ledger = RoomLedgerRepository(database, dates)

            val ids =
                (1..CONCURRENT_WRITES)
                    .map { index ->
                        async {
                            if (index % 2 == 0) {
                                ledger.add(NewExpense("Manual $index", Money(100), Category.Shopping))
                            } else {
                                ledger.addCaptured(capture("ref-$index"))
                            }
                        }
                    }.awaitAll()

            assertEquals(CONCURRENT_WRITES, ids.filterNotNull().toSet().size)
            assertEquals(CONCURRENT_WRITES, ledger.transactions.first().size)
        }

    @Test
    fun merchantCategoriesAreRememberedAndBackedUp() =
        runTest {
            val memory = RoomMerchantCategoryRepository(database)
            memory.remember("Blue Tokai", Category.FoodAndDrink)
            memory.remember("BLUE TOKAI.", Category.Shopping)
            assertEquals(Category.Shopping, memory.categoryFor("blue tokai"))

            val backup = BackupService(database, InMemorySettingsRepository())
            val exported = backup.export(PASSWORD.toCharArray())
            database.merchantCategoryDao().deleteAll()
            assertNull(memory.categoryFor("Blue Tokai"))

            backup.import(exported, PASSWORD.toCharArray())
            assertEquals(Category.Shopping, memory.categoryFor("Blue Tokai"))
        }

    @Test
    fun excludedEntriesCanBeRestored() =
        runTest {
            val ledger = RoomLedgerRepository(database, dates)
            val id = ledger.add(NewExpense("Cafe", Money(450), Category.FoodAndDrink))

            ledger.exclude(id)
            assertEquals(listOf(id), ledger.excluded.first().map { it.id })
            assertEquals(emptyList<Transaction>(), ledger.transactions.first())

            ledger.restore(id)
            assertEquals(listOf(id), ledger.transactions.first().map { it.id })
            assertEquals(emptyList<Transaction>(), ledger.excluded.first())
        }

    @Test
    fun unparsedCapturesDedupeAndKeepOnlyTheMostRecent() =
        runTest {
            val repository = RoomUnparsedCaptureRepository(database)
            val total = UnparsedCaptureRepository.MAX_ENTRIES + 5

            repeat(total) { index -> repository.add(unparsed("key-$index", postedAtMillis = index.toLong())) }
            repository.add(unparsed("key-${total - 1}", postedAtMillis = total.toLong()))

            val stored = repository.captures.first()
            assertEquals(UnparsedCaptureRepository.MAX_ENTRIES, stored.size)
            assertEquals(Instant.fromEpochMilliseconds((total - 1).toLong()), stored.first().postedAt)

            repository.dismiss(stored.first().id)
            assertEquals(UnparsedCaptureRepository.MAX_ENTRIES - 1, repository.captures.first().size)
        }

    @Test
    fun backupRoundTripKeepsDedupeKeysAndUnparsedCaptures() =
        runTest {
            val ledger = RoomLedgerRepository(database, dates)
            val unparsed = RoomUnparsedCaptureRepository(database)
            ledger.addCaptured(capture("ref-1"))
            unparsed.add(unparsed("key-1", postedAtMillis = 1))
            val backup = BackupService(database, InMemorySettingsRepository())
            val exported = backup.export(PASSWORD.toCharArray())

            database.transactionDao().deleteAll()
            unparsed.clear()
            backup.import(exported, PASSWORD.toCharArray())

            assertEquals(1, ledger.transactions.first().size)
            assertNull(ledger.addCaptured(capture("ref-1")))
            assertEquals(1, unparsed.captures.first().size)
        }

    @Test
    fun versionOneBackupStillImports() =
        runTest {
            val backup = BackupService(database, InMemorySettingsRepository())
            val v1Payload =
                """
                {"formatVersion":1,"exportedAtEpochMillis":0,
                 "transactions":[{"id":7,"merchant":"Cafe","amount":450,"occurredAt":0,"category":"FoodAndDrink",
                   "categorizationKind":"manual","paymentApp":"Manual","note":"","status":"Settled","source":"Manual"}],
                 "settings":{"onboardingCompleted":true,"notificationListenerEnabled":false,"monitoredAppCount":0,
                   "modelLabel":"Local","localFallbackReady":true,"currency":"Inr","theme":"WarmPaper","hapticsEnabled":true}}
                """.trimIndent()

            backup.import(backup.encrypt(v1Payload.encodeToByteArray(), PASSWORD.toCharArray()), PASSWORD.toCharArray())

            val restored = RoomLedgerRepository(database, dates).observe(TransactionId(7)).first()
            assertEquals("Cafe", restored?.merchant)
            assertEquals(Categorization.Manual, restored?.categorization)
        }

    @Test
    fun versionOneDatabaseMigratesWithoutLosingTransactions() =
        runTest {
            val file = File(context.cacheDir, "migration-test.db").apply { delete() }
            val stored = sampleTransaction()
            createVersionOneDatabase(file, stored)

            val migrated =
                Room
                    .databaseBuilder<QoodyDatabase>(context, file.absolutePath)
                    .setDriver(AndroidSQLiteDriver())
                    .build()
            try {
                val ledger = RoomLedgerRepository(migrated, dates)
                assertEquals(stored, ledger.observe(stored.id).first())
                assertNotNull(ledger.addCaptured(capture("ref-after-migration")))
                assertEquals(
                    TransactionId(stored.id.value + 1),
                    ledger.transactions
                        .first()
                        .first()
                        .id,
                )
                RoomUnparsedCaptureRepository(migrated).add(unparsed("key-1", postedAtMillis = 1))
            } finally {
                migrated.close()
                file.delete()
            }
        }

    /** Builds a file exactly as version 1 of the app left it, from the exported v1 schema. */
    private fun createVersionOneDatabase(
        file: File,
        transaction: Transaction,
    ) {
        val schema =
            Json
                .parseToJsonElement(File(SCHEMA_V1).readText())
                .jsonObject
                .getValue("database")
                .jsonObject
        AndroidSQLiteDriver().open(file.absolutePath).use { connection ->
            schema.getValue("entities").jsonArray.forEach { entity ->
                val table =
                    entity.jsonObject
                        .getValue("tableName")
                        .jsonPrimitive.content
                val sql =
                    entity.jsonObject
                        .getValue("createSql")
                        .jsonPrimitive.content
                connection.execSQL(sql.replace("\${TABLE_NAME}", table))
            }
            schema.getValue("setupQueries").jsonArray.forEach { connection.execSQL(it.jsonPrimitive.content) }
            connection.prepare("INSERT INTO transactions (id, payload) VALUES (?, ?)").use { statement ->
                statement.bindLong(1, transaction.id.value)
                statement.bindText(2, encodeTransaction(transaction))
                statement.step()
            }
            connection.execSQL("PRAGMA user_version = 1")
        }
    }

    private fun sampleTransaction() =
        Transaction(
            id = TransactionId(41),
            merchant = "Metro",
            amount = Money(3000),
            occurredAt = Instant.fromEpochMilliseconds(1_000),
            category = Category.Transport,
            categorization = Categorization.Manual,
            paymentApp = "Manual",
            source = EntrySource.Manual,
        )

    private fun capture(reference: String) =
        NewCapturedTransaction(
            merchant = "Swiggy",
            amount = Money(25_000),
            occurredAt = Instant.fromEpochMilliseconds(2_000),
            category = Category.FoodAndDrink,
            categorization = Categorization.Rule("swiggy"),
            paymentApp = "Test Pay",
            paymentMethod = "UPI",
            referenceCode = reference,
            notification = CapturedNotification("Test Pay", "Paid Rs 250 to Swiggy"),
            dedupeKey = reference,
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

    private companion object {
        const val PASSWORD = "correct horse"
        const val CONCURRENT_WRITES = 20
        const val SCHEMA_V1 = "schemas/com.qoody.app.data.QoodyDatabase/1.json"
    }
}
