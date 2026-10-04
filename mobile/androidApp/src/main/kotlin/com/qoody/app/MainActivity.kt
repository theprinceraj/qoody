package com.qoody.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.qoody.app.ui.QoodyApp
import com.qoody.app.ui.theme.QoodyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QoodyTheme {
                QoodyApp()
            }
        }
    }
}
