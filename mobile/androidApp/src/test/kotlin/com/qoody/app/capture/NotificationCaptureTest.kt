package com.qoody.app.capture

import android.Manifest
import android.app.Application
import android.app.Notification
import android.content.ComponentName
import android.os.Process
import android.provider.Settings
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import com.qoody.shared.capture.CaptureNotificationUseCase
import com.qoody.shared.data.InMemoryLedgerRepository
import com.qoody.shared.data.InMemoryUnparsedCaptureRepository
import com.qoody.shared.domain.model.Category
import com.qoody.shared.domain.model.EntrySource
import com.qoody.shared.domain.model.Money
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.seconds

// Every notification text here is synthetic.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class NotificationCaptureTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val ledger =
        InMemoryLedgerRepository(
            com.qoody.shared.core
                .DateProvider(),
            seed = emptyList(),
        )
    private val unparsed = InMemoryUnparsedCaptureRepository()
    private val sources = CaptureSources(defaultSmsPackage = { DEFAULT_SMS }, defaultSmsAppName = "SMS")
    private val useCase =
        CaptureNotificationUseCase(
            ledger,
            unparsed,
            unknownMerchant = { "Unknown merchant" },
            appKind = sources::kindOf,
        )
    private val handler by lazy { NotificationCaptureHandler(useCase, context.packageName, sources) }

    @After
    fun tearDown() = stopKoin()

    @Test
    fun readsTitleAndPrefersBigText() {
        val posted =
            posted(
                build {
                    setContentTitle("Payment successful")
                    setContentText("Paid ₹340")
                    setStyle(NotificationCompat.BigTextStyle().bigText("Paid ₹340.50 to Swiggy"))
                },
            )

        val read = handler.read(posted).singleOrNull()

        assertNotNull(read)
        assertEquals("Google Pay", read?.appName)
        assertEquals("Payment successful", read?.title)
        assertEquals("Paid ₹340.50 to Swiggy", read?.text)
        assertEquals(POST_TIME, read?.postedAt?.toEpochMilliseconds())
    }

    @Test
    fun readsInboxStyleLinesWhenThereIsNoText() {
        val posted =
            posted(
                build {
                    setContentTitle("Bank alert")
                    setStyle(NotificationCompat.InboxStyle().addLine("Rs 99 debited").addLine("to Spotify"))
                },
            )

        assertEquals("Rs 99 debited\nto Spotify", handler.read(posted).single().text)
    }

    @Test
    fun ignoresUnsupportedOngoingSummaryAndOwnNotifications() {
        val payment = build { setContentText("Paid ₹340.50 to Swiggy") }

        assertTrue(handler.read(posted(payment, packageName = "com.example.chat")).isEmpty())
        assertTrue(handler.read(posted(payment, packageName = context.packageName)).isEmpty())
        assertTrue(handler.read(posted(build { setContentText("Paid ₹1 to X").setOngoing(true) })).isEmpty())
        assertTrue(
            handler
                .read(posted(build { setContentText("Paid ₹1 to X").setGroup("g").setGroupSummary(true) }))
                .isEmpty(),
        )
    }

    @Test
    fun conversationStyleSmsYieldsEveryMessageWithItsSender() {
        val sender = Person.Builder().setName("JD-HDFCBK-S").build()
        val sms =
            build {
                setStyle(
                    NotificationCompat
                        .MessagingStyle(Person.Builder().setName("Me").build())
                        .addMessage("Sent Rs.500.00 From HDFC Bank A/C *1234 To SWIGGY On 05/10/26", FIRST_SMS, sender)
                        .addMessage(
                            "Sent Rs.80.00 From HDFC Bank A/C *1234 To CHAI POINT On 05/10/26",
                            SECOND_SMS,
                            sender,
                        ),
                )
            }

        val read = handler.read(posted(sms, packageName = GOOGLE_MESSAGES))

        assertEquals(listOf("JD-HDFCBK-S", "JD-HDFCBK-S"), read.map { it.title })
        assertEquals(listOf(FIRST_SMS, SECOND_SMS), read.map { it.postedAt.toEpochMilliseconds() })
        assertEquals("Google Messages", read.first().appName)
        assertEquals(2, read.map { it.key }.toSet().size)
    }

    @Test
    fun bankSmsInTheDefaultSmsAppIsSaved() {
        startKoin { modules(module { single { handler } }) }
        val service = Robolectric.buildService(QoodyNotificationListenerService::class.java).create().get()
        val sender = Person.Builder().setName("AD-SBIUPI").build()
        val sms =
            build {
                setStyle(
                    NotificationCompat
                        .MessagingStyle(Person.Builder().setName("Me").build())
                        .addMessage(
                            "Dear UPI user A/C X1234 debited by 250.0 on date 05Oct26 trf to ZOMATO Refno 512345678902.",
                            FIRST_SMS,
                            sender,
                        ),
                )
            }

        service.onNotificationPosted(posted(sms, packageName = DEFAULT_SMS))

        val saved = runBlocking { withTimeout(5.seconds) { ledger.transactions.first { it.isNotEmpty() } } }.single()
        assertEquals("Zomato", saved.merchant)
        assertEquals(Money.of(250), saved.amount)
        assertEquals("SBIUPI", saved.paymentApp)
        service.onDestroy()
    }

    @Test
    fun serviceSavesAPostedPaymentThroughKoin() {
        startKoin { modules(module { single { handler } }) }
        val service = Robolectric.buildService(QoodyNotificationListenerService::class.java).create().get()

        service.onNotificationPosted(posted(build { setContentText("Paid ₹340.50 to Swiggy") }))

        val saved = runBlocking { withTimeout(5.seconds) { ledger.transactions.first { it.isNotEmpty() } } }.single()
        assertEquals("Swiggy", saved.merchant)
        assertEquals(Money.of(340, 50), saved.amount)
        assertEquals(Category.FoodAndDrink, saved.category)
        assertEquals(EntrySource.Notification, saved.source)
        service.onDestroy()
    }

    @Test
    fun manifestDeclaresTheServiceBehindTheBindPermission() {
        val component = ComponentName(context, QoodyNotificationListenerService::class.java)
        val info = context.packageManager.getServiceInfo(component, 0)

        assertEquals(Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE, info.permission)
        assertTrue(info.exported)
    }

    @Test
    fun accessCheckerFollowsAndroidSettings() {
        val checker = AndroidNotificationAccessChecker(context)
        assertFalse(checker.isEnabled())

        val component = ComponentName(context, QoodyNotificationListenerService::class.java)
        Settings.Secure.putString(context.contentResolver, ENABLED_LISTENERS, component.flattenToString())

        assertTrue(checker.isEnabled())
    }

    private fun build(configure: NotificationCompat.Builder.() -> Unit): Notification =
        NotificationCompat
            .Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .apply(configure)
            .build()

    @Suppress("DEPRECATION")
    private fun posted(
        notification: Notification,
        packageName: String = GOOGLE_PAY,
    ) = StatusBarNotification(
        packageName,
        packageName,
        1,
        null,
        Process.myUid(),
        0,
        0,
        notification,
        Process.myUserHandle(),
        POST_TIME,
    )

    private companion object {
        const val GOOGLE_PAY = "com.google.android.apps.nbu.paisa.user"
        const val GOOGLE_MESSAGES = "com.google.android.apps.messaging"
        const val DEFAULT_SMS = "com.android.mms"
        const val FIRST_SMS = 1_759_650_000_000L
        const val SECOND_SMS = 1_759_650_060_000L
        const val CHANNEL = "payments"
        const val POST_TIME = 1_759_650_000_000L
        const val ENABLED_LISTENERS = "enabled_notification_listeners"
    }
}
