package com.qoody.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import com.qoody.shared.domain.model.TransactionId

/**
 * Owns the back stack. The ledger is always the root; the other tabs and pushed screens stack on top,
 * so Back from any tab returns to the ledger and a second Back leaves the app.
 */
@Stable
class QoodyNavigator(
    val backStack: NavBackStack<NavKey>,
) {
    /** The tab currently highlighted, even while a detail screen is pushed on top of it. */
    val selectedTab: TopLevelDestination
        get() = TopLevelDestination.of(backStack.filterIsInstance<TabKey>().last())

    /** The bottom bar is shown on the three main tabs only. */
    val showsBottomBar: Boolean
        get() = backStack.last() is TabKey

    fun switchTo(destination: TopLevelDestination) {
        if (destination == selectedTab && showsBottomBar) return
        trimToRoot()
        if (destination != TopLevelDestination.Ledger) backStack.add(destination.toKey())
    }

    /** Opens the ledger with its search field focused (the search icon on other tabs). */
    fun openSearch() {
        backStack.clear()
        backStack.add(LedgerKey(openSearch = true))
    }

    fun openReceipt(id: TransactionId) {
        backStack.add(ReceiptKey(id.value))
    }

    fun pop() {
        backStack.removeLastOrNull()
    }

    private fun trimToRoot() {
        // removeAt(lastIndex), not removeLast(): the latter binds to a Java API missing before Android 15.
        while (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }
}

@Composable
fun rememberQoodyNavigator(): QoodyNavigator {
    val backStack = rememberNavBackStack(LedgerKey())
    return QoodyNavigator(backStack)
}
