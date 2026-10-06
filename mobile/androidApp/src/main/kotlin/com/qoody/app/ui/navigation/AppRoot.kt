package com.qoody.app.ui.navigation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.qoody.app.ui.budgets.BudgetsScreen
import com.qoody.app.ui.capture.UnparsedCapturesScreen
import com.qoody.app.ui.categories.CategoriesScreen
import com.qoody.app.ui.components.QoodyBottomBar
import com.qoody.app.ui.excluded.ExcludedEntriesScreen
import com.qoody.app.ui.insights.InsightsScreen
import com.qoody.app.ui.ledger.LedgerScreen
import com.qoody.app.ui.onboarding.OnboardingScreen
import com.qoody.app.ui.receipt.ReceiptScreen
import com.qoody.app.ui.settings.SettingsScreen
import com.qoody.app.ui.theme.LocalCustomCategories
import com.qoody.shared.domain.model.TransactionId
import com.qoody.shared.feature.root.RootUiState
import com.qoody.shared.feature.root.RootViewModel
import org.koin.androidx.compose.koinViewModel

/** Chooses between onboarding and the main experience. */
@Composable
fun QoodyApp(rootViewModel: RootViewModel = koinViewModel()) {
    val root by rootViewModel.uiState.collectAsStateWithLifecycle()
    val customCategories by rootViewModel.customCategories.collectAsStateWithLifecycle()
    val openNotificationAccess = rememberNotificationAccessLauncher()

    CompositionLocalProvider(LocalCustomCategories provides customCategories) {
        when (root) {
            RootUiState.Loading -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface))
            RootUiState.Onboarding -> OnboardingScreen(onOpenNotificationAccessSettings = openNotificationAccess)
            RootUiState.Main -> MainNavigation(onOpenNotificationAccessSettings = openNotificationAccess)
        }
    }
}

@Composable
private fun MainNavigation(onOpenNotificationAccessSettings: () -> Unit) {
    val navigator = rememberQoodyNavigator()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        bottomBar = {
            if (navigator.showsBottomBar) {
                QoodyBottomBar(selected = navigator.selectedTab, onSelect = navigator::switchTo)
            }
        },
    ) { padding ->
        NavDisplay(
            backStack = navigator.backStack,
            onBack = navigator::pop,
            modifier = Modifier.padding(padding),
            entryDecorators =
                listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
            entryProvider =
                entryProvider {
                    entry<LedgerKey> { key ->
                        LedgerScreen(
                            onOpenReceipt = { id: TransactionId -> navigator.openReceipt(id) },
                            onOpenBudgets = navigator::openBudgets,
                            startWithSearch = key.openSearch,
                        )
                    }
                    entry<InsightsKey> {
                        InsightsScreen(onSearchClick = navigator::openSearch)
                    }
                    entry<SettingsKey> {
                        SettingsScreen(
                            onSearchClick = navigator::openSearch,
                            onOpenNotificationAccessSettings = onOpenNotificationAccessSettings,
                            onOpenUnparsedCaptures = navigator::openUnparsedCaptures,
                            onOpenExcludedEntries = navigator::openExcludedEntries,
                            onOpenBudgets = navigator::openBudgets,
                            onOpenCategories = navigator::openCategories,
                        )
                    }
                    entry<CategoriesKey> {
                        CategoriesScreen(onBack = navigator::pop)
                    }
                    entry<BudgetsKey> {
                        BudgetsScreen(onBack = navigator::pop)
                    }
                    entry<ExcludedEntriesKey> {
                        ExcludedEntriesScreen(
                            onBack = navigator::pop,
                            onOpenReceipt = { id: TransactionId -> navigator.openReceipt(id) },
                        )
                    }
                    entry<UnparsedCapturesKey> {
                        UnparsedCapturesScreen(onBack = navigator::pop)
                    }
                    entry<ReceiptKey> { key ->
                        ReceiptScreen(
                            transactionId = key.transactionId,
                            onBack = navigator::pop,
                            onOpenBudgets = navigator::openBudgets,
                        )
                    }
                },
        )
    }
}

/** Returns an action that opens Android's "Notification access" settings page. */
@Composable
fun rememberNotificationAccessLauncher(): () -> Unit {
    val context = LocalContext.current
    return remember(context) { { openNotificationAccessSettings(context) } }
}

private fun openNotificationAccessSettings(context: Context) {
    try {
        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    } catch (_: ActivityNotFoundException) {
        // A device without the settings page: nothing sensible to do.
    }
}
