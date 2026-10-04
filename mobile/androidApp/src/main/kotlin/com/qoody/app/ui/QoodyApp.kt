package com.qoody.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.qoody.app.ui.home.HomeScreen
import kotlinx.serialization.Serializable

@Serializable
data object Home : NavKey

@Composable
fun QoodyApp() {
    val backStack = rememberNavBackStack(Home)
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider =
            entryProvider {
                entry<Home> { HomeScreen() }
            },
    )
}
