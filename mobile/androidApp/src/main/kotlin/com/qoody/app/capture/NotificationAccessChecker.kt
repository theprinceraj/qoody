package com.qoody.app.capture

import android.content.Context
import androidx.core.app.NotificationManagerCompat

/** Whether the user has granted Qoody notification access in Android settings. */
fun interface NotificationAccessChecker {
    fun isEnabled(): Boolean
}

class AndroidNotificationAccessChecker(
    private val context: Context,
) : NotificationAccessChecker {
    override fun isEnabled(): Boolean =
        context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)
}
