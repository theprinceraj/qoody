package com.qoody.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.qoody.app.capture.NotificationAccessChecker
import com.qoody.app.ui.navigation.QoodyApp
import com.qoody.app.ui.theme.QoodyTheme
import com.qoody.shared.domain.repository.SettingsRepository
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val settings: SettingsRepository by inject()
    private val notificationAccess: NotificationAccessChecker by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QoodyTheme {
                QoodyApp()
            }
        }
    }

    /** The user may have granted or revoked notification access in Android settings while we were away. */
    override fun onResume() {
        super.onResume()
        val enabled = notificationAccess.isEnabled()
        lifecycleScope.launch { settings.setNotificationListenerEnabled(enabled) }
    }
}
