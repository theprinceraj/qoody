package com.qoody.app.capture

import android.app.Notification
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.qoody.shared.capture.CaptureNotificationUseCase
import com.qoody.shared.capture.CapturePolicy
import com.qoody.shared.capture.PaymentNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.time.Instant

/**
 * Turns a posted [StatusBarNotification] into a [PaymentNotification] and hands it to the capture use case.
 * Kept apart from the service so it can be tested with real notifications and no service binding.
 *
 * Privacy: packages not on the allowlist are dropped before any notification text is read, and no
 * notification content is ever logged.
 */
class NotificationCaptureHandler(
    private val capture: CaptureNotificationUseCase,
    private val ownPackage: String,
) {
    fun handle(
        posted: StatusBarNotification,
        scope: CoroutineScope,
    ) {
        val notification = read(posted) ?: return
        scope.launch { capture(notification) }
    }

    /** The notification's content, or `null` when it must be ignored. */
    fun read(posted: StatusBarNotification): PaymentNotification? {
        val appName = CapturePolicy.appName(posted.packageName)
        val notification = posted.notification
        if (appName == null || notification == null || isIgnored(posted, notification)) return null

        val extras = notification.extras
        val title = extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString().orEmpty()
        val text =
            extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT)?.toString()
                ?: extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString()
                ?: extras
                    .getCharSequenceArray(NotificationCompat.EXTRA_TEXT_LINES)
                    ?.joinToString("\n")
                    .orEmpty()
        return if (title.isBlank() && text.isBlank()) {
            null
        } else {
            PaymentNotification(
                key = posted.key,
                packageName = posted.packageName,
                appName = appName,
                title = title,
                text = text,
                postedAt = Instant.fromEpochMilliseconds(posted.postTime),
            )
        }
    }

    /** Our own, ongoing (progress, media) and group-summary notifications are never payments. */
    private fun isIgnored(
        posted: StatusBarNotification,
        notification: Notification,
    ): Boolean =
        posted.packageName == ownPackage ||
            posted.isOngoing ||
            notification.flags and Notification.FLAG_GROUP_SUMMARY != 0
}
