package com.qoody.app.capture

import android.app.Notification
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.qoody.shared.capture.CaptureNotificationUseCase
import com.qoody.shared.capture.PaymentNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.time.Instant

/**
 * Turns a posted [StatusBarNotification] into [PaymentNotification]s and hands them to the capture use case.
 * Kept apart from the service so it can be tested with real notifications and no service binding.
 *
 * Privacy: packages not on the allowlist are dropped before any notification text is read, and no
 * notification content is ever logged.
 */
class NotificationCaptureHandler(
    private val capture: CaptureNotificationUseCase,
    private val ownPackage: String,
    private val sources: CaptureSources,
) {
    fun handle(
        posted: StatusBarNotification,
        scope: CoroutineScope,
    ) {
        val notifications = read(posted)
        if (notifications.isEmpty()) return
        scope.launch { notifications.forEach { capture(it) } }
    }

    /**
     * The notification's content; empty when it must be ignored. A conversation-style notification
     * (SMS apps) yields one entry per message, since several bank alerts can arrive in one update.
     */
    fun read(posted: StatusBarNotification): List<PaymentNotification> {
        val appName = sources.appName(posted.packageName)
        val notification = posted.notification
        if (appName == null || notification == null || isIgnored(posted, notification)) return emptyList()

        val messaging = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
        val entries =
            if (messaging != null && messaging.messages.isNotEmpty()) {
                messaging.messages.map { message ->
                    val postedAtMillis = message.timestamp.takeIf { it > 0 } ?: posted.postTime
                    Entry(
                        // Stable per message, so a re-posted conversation does not repeat earlier messages.
                        key = "${posted.key}#$postedAtMillis",
                        title =
                            message.person?.name?.toString()
                                ?: messaging.conversationTitle?.toString()
                                ?: notification.extras.text(NotificationCompat.EXTRA_TITLE),
                        text = message.text?.toString().orEmpty(),
                        postedAtMillis = postedAtMillis,
                    )
                }
            } else {
                listOf(
                    Entry(
                        key = posted.key,
                        title = notification.extras.text(NotificationCompat.EXTRA_TITLE),
                        text = plainText(notification),
                        postedAtMillis = posted.postTime,
                    ),
                )
            }
        return entries
            .filter { it.title.isNotBlank() || it.text.isNotBlank() }
            .map { entry ->
                PaymentNotification(
                    key = entry.key,
                    packageName = posted.packageName,
                    appName = appName,
                    title = entry.title,
                    text = entry.text,
                    postedAt = Instant.fromEpochMilliseconds(entry.postedAtMillis),
                )
            }
    }

    private fun plainText(notification: Notification): String {
        val extras = notification.extras
        return extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString()
            ?: extras
                .getCharSequenceArray(NotificationCompat.EXTRA_TEXT_LINES)
                ?.joinToString("\n")
                .orEmpty()
    }

    private fun android.os.Bundle.text(key: String): String = getCharSequence(key)?.toString().orEmpty()

    /** Our own, ongoing (progress, media) and group-summary notifications are never payments. */
    private fun isIgnored(
        posted: StatusBarNotification,
        notification: Notification,
    ): Boolean =
        posted.packageName == ownPackage ||
            posted.isOngoing ||
            notification.flags and Notification.FLAG_GROUP_SUMMARY != 0

    private data class Entry(
        val key: String,
        val title: String,
        val text: String,
        val postedAtMillis: Long,
    )
}
