package com.qoody.app

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.qoody.app.ui.budgets.BudgetsContent
import com.qoody.app.ui.capture.UnparsedCapturesContent
import com.qoody.app.ui.excluded.ExcludedEntriesContent
import com.qoody.app.ui.insights.InsightsContent
import com.qoody.app.ui.ledger.LedgerContent
import com.qoody.app.ui.onboarding.OnboardingContent
import com.qoody.app.ui.receipt.ReceiptContent
import com.qoody.app.ui.settings.SettingsContent
import com.qoody.app.ui.theme.QoodyTheme
import com.qoody.shared.capture.UnparsedReason
import com.qoody.shared.core.DateProvider
import com.qoody.shared.data.InMemoryBudgetRepository
import com.qoody.shared.data.InMemoryLedgerRepository
import com.qoody.shared.data.InMemoryMerchantCategoryRepository
import com.qoody.shared.data.InMemorySettingsRepository
import com.qoody.shared.data.InMemoryUnparsedCaptureRepository
import com.qoody.shared.domain.model.Category
import com.qoody.shared.domain.model.Money
import com.qoody.shared.domain.model.NewUnparsedCapture
import com.qoody.shared.feature.budgets.BudgetsUiState
import com.qoody.shared.feature.budgets.BudgetsViewModel
import com.qoody.shared.feature.capture.UnparsedCapturesUiState
import com.qoody.shared.feature.capture.UnparsedCapturesViewModel
import com.qoody.shared.feature.excluded.ExcludedEntriesUiState
import com.qoody.shared.feature.excluded.ExcludedEntriesViewModel
import com.qoody.shared.feature.insights.InsightsUiState
import com.qoody.shared.feature.insights.InsightsViewModel
import com.qoody.shared.feature.ledger.LedgerUiState
import com.qoody.shared.feature.ledger.LedgerViewModel
import com.qoody.shared.feature.receipt.ReceiptUiState
import com.qoody.shared.feature.receipt.ReceiptViewModel
import com.qoody.shared.feature.settings.SettingsUiState
import com.qoody.shared.feature.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/** Qualifiers for a typical phone, in portrait, at the density the designs were drawn for. */
private const val PHONE = "w411dp-h891dp-xxhdpi"

/** A phone-width screen tall enough to show a whole scrolling page in one image. */
private const val TALL_PAGE = "w411dp-h2300dp-xxhdpi"

private const val OUTPUT_DIRECTORY = "build/outputs/screens"

/**
 * Renders every screen from the real ViewModels and sample data to a PNG and checks it composes.
 * Open the PNGs in `androidApp/build/outputs/screens` to compare with the designs.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = PHONE, application = Application::class)
class ScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val today = LocalDate(2026, 10, 24)
    private val noon: Instant = today.atStartOfDayIn(TimeZone.UTC) + 12.hours
    private val dates =
        DateProvider(
            clock =
                object : Clock {
                    override fun now(): Instant = noon
                },
            zoneProvider = { TimeZone.UTC },
        )
    private val ledger by lazy { InMemoryLedgerRepository(dates) }
    private val settings by lazy { InMemorySettingsRepository() }

    @Before
    fun installMainDispatcher() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun removeMainDispatcher() = Dispatchers.resetMain()

    private fun <S> StateFlow<S>.await(isReady: (S) -> Boolean): S = runBlocking { first(isReady) }

    private fun capture(
        name: String,
        qualifiers: String = PHONE,
        content: @Composable () -> Unit,
    ) {
        RuntimeEnvironment.setQualifiers("+$qualifiers")
        composeRule.setContent {
            QoodyTheme {
                Surface(color = MaterialTheme.colorScheme.surface) { content() }
            }
        }
        File(OUTPUT_DIRECTORY).mkdirs()
        composeRule.onRoot().captureRoboImage("$OUTPUT_DIRECTORY/$name.png")
    }

    @Test
    fun onboarding() = capture("onboarding") { OnboardingContent(onEnableClick = {}) }

    @Test
    fun ledger() {
        val budgets =
            InMemoryBudgetRepository(
                mapOf(
                    Category.FoodAndDrink to Money.of(150),
                    Category.Transport to Money.of(400),
                ),
            )
        val state = LedgerViewModel(ledger, settings, dates, budgets).uiState.await { it is LedgerUiState.Content }
        capture("ledger", TALL_PAGE) {
            LedgerContent(
                state = state,
                onSearchClick = {},
                onSearchQueryChange = {},
                onSearchClose = {},
                onCategorySelected = {},
                onRowClick = {},
                onAddExpenseClick = {},
            )
        }
    }

    @Test
    fun insights() {
        val state = InsightsViewModel(ledger, settings, dates).uiState.await { it is InsightsUiState.Content }
        capture("insights", TALL_PAGE) {
            InsightsContent(state = state, onPeriodSelected = {}, onSearchClick = {})
        }
    }

    @Test
    fun receipt() {
        val newest = runBlocking { ledger.transactions.first().first() }
        val viewModel =
            ReceiptViewModel(
                newest.id,
                ledger,
                settings,
                dates,
                InMemoryMerchantCategoryRepository(),
                InMemoryBudgetRepository(mapOf(newest.category to Money.of(150))),
            )
        val state = viewModel.uiState.await { it is ReceiptUiState.Content }
        capture("receipt", TALL_PAGE) {
            ReceiptContent(
                state = state,
                snackbarHost = SnackbarHostState(),
                onBack = {},
                onNoteChange = {},
                onNoteCommit = {},
                onChangeCategory = {},
                onCategorySelected = {},
                onCategoryPickerDismiss = {},
                onSplit = {},
                onKeep = {},
                onExclude = {},
                onRestore = {},
            )
        }
    }

    @Test
    fun settings() {
        val unparsed = InMemoryUnparsedCaptureRepository()
        runBlocking { unparsed.add(sampleUnparsed()) }
        val viewModel = SettingsViewModel(settings, ledger, dates, unparsed)
        viewModel.onNotificationListenerToggled(true)
        val state = viewModel.uiState.await { it is SettingsUiState.Content }
        capture("settings", TALL_PAGE) {
            SettingsContent(
                state = state,
                snackbarHost = SnackbarHostState(),
                onSearchClick = {},
                onNotificationToggled = {},
                onManageApps = {},
                onOpenUnparsedCaptures = {},
                onOpenExcludedEntries = {},
                onThemeSelected = {},
                onCurrencySelected = {},
                onHapticsToggled = {},
                onOpenSource = {},
                onExport = {},
                onExportFull = {},
                onImportFull = {},
            )
        }
    }

    @Test
    fun unparsedCaptures() {
        val repository = InMemoryUnparsedCaptureRepository()
        runBlocking { repository.add(sampleUnparsed()) }
        val state =
            UnparsedCapturesViewModel(repository, dates).uiState.await { it is UnparsedCapturesUiState.Content }
        capture("unparsed-captures") {
            UnparsedCapturesContent(
                state = state,
                onBack = {},
                onAddManually = {},
                onDismiss = {},
                onClearAll = {},
            )
        }
    }

    @Test
    fun excludedEntries() {
        val newest = runBlocking { ledger.transactions.first().first() }
        runBlocking { ledger.exclude(newest.id) }
        val state =
            ExcludedEntriesViewModel(ledger, settings, dates).uiState.await {
                it is ExcludedEntriesUiState.Content && it.entries.isNotEmpty()
            }
        capture("excluded-entries") {
            ExcludedEntriesContent(state = state, onBack = {}, onOpenReceipt = {}, onRestore = {})
        }
    }

    @Test
    fun budgets() {
        val budgets =
            InMemoryBudgetRepository(
                mapOf(Category.FoodAndDrink to Money.of(150), Category.Transport to Money.of(400)),
            )
        val state =
            BudgetsViewModel(budgets, ledger, settings, dates).uiState.await { it is BudgetsUiState.Content }
        capture("budgets", TALL_PAGE) {
            BudgetsContent(state = state, onBack = {}, onEdit = {})
        }
    }

    /** A synthetic bank alert that names no amount. */
    private fun sampleUnparsed() =
        NewUnparsedCapture(
            packageName = "com.example.bank",
            appName = "Example Bank",
            title = "Transaction alert",
            text = "Your account XX1234 has been debited towards UPI payment.",
            postedAt = noon - 2.hours,
            reason = UnparsedReason.NoAmount,
            dedupeKey = "sample",
        )
}
