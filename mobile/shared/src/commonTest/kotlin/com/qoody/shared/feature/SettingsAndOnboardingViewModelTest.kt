package com.qoody.shared.feature

import com.qoody.shared.ViewModelTest
import com.qoody.shared.capture.CapturePolicy
import com.qoody.shared.capture.UnparsedReason
import com.qoody.shared.data.InMemoryLedgerRepository
import com.qoody.shared.data.InMemorySettingsRepository
import com.qoody.shared.data.InMemoryUnparsedCaptureRepository
import com.qoody.shared.domain.model.Category
import com.qoody.shared.domain.model.Currency
import com.qoody.shared.domain.model.KeyVerification
import com.qoody.shared.domain.model.Money
import com.qoody.shared.domain.model.NewUnparsedCapture
import com.qoody.shared.domain.repository.LlmKeyVerifier
import com.qoody.shared.feature.ledger.AddExpenseEvent
import com.qoody.shared.feature.ledger.AddExpenseViewModel
import com.qoody.shared.feature.onboarding.OnboardingViewModel
import com.qoody.shared.feature.root.RootUiState
import com.qoody.shared.feature.root.RootViewModel
import com.qoody.shared.feature.settings.SettingsUiState
import com.qoody.shared.feature.settings.SettingsViewModel
import com.qoody.shared.fixedDates
import com.qoody.shared.transaction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

private const val ACCEPTED_KEY = "sk-valid"

class RootAndOnboardingViewModelTest : ViewModelTest() {
    private val settings = InMemorySettingsRepository()

    @Test
    fun showsOnboardingUntilItIsCompleted() =
        runTest {
            val root = RootViewModel(settings)
            assertEquals(RootUiState.Onboarding, root.uiState.latest())

            OnboardingViewModel(settings).onEnableNotificationAccess()

            assertEquals(RootUiState.Main, root.uiState.latest())
            val saved = settings.settings.first()
            assertTrue(saved.onboardingCompleted)
            assertTrue(saved.notificationListenerEnabled)
        }
}

class SettingsViewModelTest : ViewModelTest() {
    private val dates = fixedDates()
    private val settings = InMemorySettingsRepository()
    private val ledger = InMemoryLedgerRepository(dates, listOf(transaction(0, Money.of(4, 50), merchant = "Coffee")))
    private val verifier =
        object : LlmKeyVerifier {
            override suspend fun verify(key: String) = key == ACCEPTED_KEY
        }

    // Lazy: a ViewModel must be created after the test installs the Main dispatcher.
    private val unparsed = InMemoryUnparsedCaptureRepository()
    private val viewModel by lazy { SettingsViewModel(settings, ledger, verifier, dates, unparsed) }

    private fun content() = viewModel.uiState.latest() as SettingsUiState.Content

    @Test
    fun showsTheFailedToParseCountAndTheAllowlistSize() =
        runTest {
            unparsed.add(
                NewUnparsedCapture(
                    packageName = "com.example.pay",
                    appName = "Test Pay",
                    title = "",
                    text = "Account debited",
                    postedAt = Instant.fromEpochMilliseconds(0),
                    reason = UnparsedReason.NoAmount,
                    dedupeKey = "k",
                ),
            )

            val state = content()
            assertEquals(1, state.unparsedCount)
            assertEquals(CapturePolicy.supportedApps.size, state.settings.monitoredAppCount)
        }

    @Test
    fun pastingAKeyStoresItTrimmedAndResetsVerification() =
        runTest {
            viewModel.onApiKeyPasted("  $ACCEPTED_KEY \n")

            val state = content()
            assertEquals(ACCEPTED_KEY, state.settings.llm.apiKey)
            assertTrue(state.hasApiKey)
            assertEquals(KeyVerification.Idle, state.keyVerification)
        }

    @Test
    fun blankPasteIsIgnored() =
        runTest {
            viewModel.onApiKeyPasted("   ")

            assertFalse(content().hasApiKey)
        }

    @Test
    fun testingAcceptedAndRejectedKeys() =
        runTest {
            viewModel.onApiKeyPasted(ACCEPTED_KEY)
            viewModel.onTestKeyRequested()
            assertEquals(KeyVerification.Verified, content().keyVerification)

            viewModel.onApiKeyPasted("sk-wrong")
            assertEquals(KeyVerification.Idle, content().keyVerification)
            viewModel.onTestKeyRequested()
            assertEquals(KeyVerification.Failed, content().keyVerification)
        }

    @Test
    fun testingWithoutAKeyDoesNothing() =
        runTest {
            viewModel.onTestKeyRequested()

            assertEquals(KeyVerification.Idle, content().keyVerification)
        }

    @Test
    fun togglesAndPreferencesArePersisted() =
        runTest {
            viewModel.onCurrencySelected(Currency.Inr)
            viewModel.onHapticsToggled(false)
            viewModel.onNotificationListenerToggled(true)
            viewModel.onKeyVisibilityToggled()

            val state = content()
            assertEquals(Currency.Inr, state.settings.currency)
            assertFalse(state.settings.hapticsEnabled)
            assertTrue(state.settings.notificationListenerEnabled)
            assertTrue(state.isKeyVisible)
        }

    @Test
    fun exportBuildsCsvFromTheLedger() =
        runTest {
            val csv = viewModel.buildCsvExport()

            assertTrue(csv.startsWith("id,date,time,merchant"))
            assertTrue(csv.contains("Coffee"))
        }
}

class AddExpenseViewModelTest : ViewModelTest() {
    private val dates = fixedDates()
    private val ledger = InMemoryLedgerRepository(dates, emptyList())

    // Lazy: a ViewModel must be created after the test installs the Main dispatcher.
    private val viewModel by lazy { AddExpenseViewModel(ledger, InMemorySettingsRepository()) }

    @Test
    fun canSaveOnlyWithAPositiveAmountAndAMerchant() =
        runTest {
            assertFalse(viewModel.uiState.latest().canSave)

            viewModel.onAmountChanged("12.5x")
            assertEquals("12.5", viewModel.uiState.latest().amountInput)
            assertFalse(viewModel.uiState.latest().canSave)

            viewModel.onMerchantChanged("Cafe")
            assertTrue(viewModel.uiState.latest().canSave)

            viewModel.onAmountChanged("0")
            assertFalse(viewModel.uiState.latest().canSave)
        }

    @Test
    fun savingAddsTheExpenseResetsTheFormAndEmitsSaved() =
        runTest {
            viewModel.onAmountChanged("12.50")
            viewModel.onMerchantChanged("  Cafe  ")
            viewModel.onCategorySelected(Category.FoodAndDrink)

            viewModel.onSave()

            assertEquals(AddExpenseEvent.Saved, viewModel.events.first())
            val saved = ledger.transactions.first().single()
            assertEquals("Cafe", saved.merchant)
            assertEquals(Money.of(12, 50), saved.amount)
            assertEquals(Category.FoodAndDrink, saved.category)
            assertEquals("", viewModel.uiState.latest().amountInput)
            assertFalse(viewModel.uiState.latest().canSave)
        }

    @Test
    fun savingAnIncompleteFormDoesNothing() =
        runTest {
            viewModel.onAmountChanged("5")

            viewModel.onSave()

            assertEquals(emptyList(), ledger.transactions.first())
        }
}
