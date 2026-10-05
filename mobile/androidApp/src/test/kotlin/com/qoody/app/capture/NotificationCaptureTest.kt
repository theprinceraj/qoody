package com.qoody.app.capture

import android.Manifest
import android.app.Application
import android.app.Notification
import android.content.ComponentName
import android.os.Process
import android.provider.Settings
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
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
import org.junit.Assert.assertNull
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
    private val useCase = CaptureNotificationUseCase(ledger, unparsed, unknownMerchant = { "Unknown merchant" })
    private val handler by lazy { NotificationCaptureHandler(useCase, context.packageName) }

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

        val read = handler.read(posted)

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

        assertEquals("Rs 99 debited\nto Spotify", handler.read(posted)?.text)
    }

    @Test
    fun ignoresUnsupportedOngoingSummaryAndOwnNotifications() {
        val payment = build { setContentText("Paid ₹340.50 to Swiggy") }

        assertNull(handler.read(posted(payment, packageName = "com.example.chat")))
        assertNull(handler.read(posted(payment, packageName = context.packageName)))
        assertNull(handler.read(posted(build { setContentText("Paid ₹1 to X").setOngoing(true) })))
        assertNull(
            handler.read(
                posted(build { setContentText("Paid ₹1 to X").setGroup("g").setGroupSummary(true) }),
            ),
        )
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
        const val CHANNEL = "payments"
        const val POST_TIME = 1_759_650_000_000L
        const val ENABLED_LISTENERS = "enabled_notification_listeners"
    }
}
