package com.qoody.app.capture

import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Receives every posted notification once the user grants notification access, and passes payment
 * notifications from supported apps to [NotificationCaptureHandler]. The system creates this service,
 * so dependencies come from Koin (started in `QoodyApplication.onCreate`, which runs first).
 */
class QoodyNotificationListenerService :
    NotificationListenerService(),
    KoinComponent {
    private val handler: NotificationCaptureHandler by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onNotificationPosted(posted: StatusBarNotification) {
        handler.handle(posted, scope)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        // The system unbinds us under memory pressure or after an update; ask to be bound again.
        requestRebind(ComponentName(this, QoodyNotificationListenerService::class.java))
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
