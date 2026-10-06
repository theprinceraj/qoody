package com.qoody.app.ui.receipt

import android.app.Application
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.requestFocus
import com.qoody.app.ui.theme.QoodyTheme
import com.qoody.shared.core.DateProvider
import com.qoody.shared.data.InMemoryBudgetRepository
import com.qoody.shared.data.InMemoryLedgerRepository
import com.qoody.shared.data.InMemoryMerchantCategoryRepository
import com.qoody.shared.feature.receipt.ReceiptUiState
import com.qoody.shared.feature.receipt.ReceiptViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The note field must be leavable: Done or a tap elsewhere ends editing and saves. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class ReceiptNoteFocusTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var commits = 0
    private lateinit var state: ReceiptUiState.Content

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val dates = DateProvider()
        val ledger = InMemoryLedgerRepository(dates)
        val newest = runBlocking { ledger.transactions.first().first() }
        val viewModel =
            ReceiptViewModel(
                newest.id,
                ledger,
                dates,
                InMemoryMerchantCategoryRepository(),
                InMemoryBudgetRepository(),
            )
        state = runBlocking { viewModel.uiState.first { it is ReceiptUiState.Content } } as ReceiptUiState.Content
        composeRule.setContent {
            QoodyTheme {
                ReceiptContent(
                    state = state,
                    snackbarHost = SnackbarHostState(),
                    onBack = {},
                    onNoteChange = {},
                    onNoteCommit = { commits++ },
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
        commits = 0
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun noteField() = composeRule.onNode(hasSetTextAction())

    @Test
    fun tappingTheFieldKeepsItFocused() {
        noteField().requestFocus()

        noteField().performClick()

        noteField().assertIsFocused()
        assertEquals(0, commits)
    }

    @Test
    fun tappingElsewhereEndsEditingAndSaves() {
        noteField().requestFocus()
        noteField().performTextInput("Team lunch")

        composeRule.onNodeWithText(state.merchant).performClick()

        noteField().assertIsNotFocused()
        assertEquals(1, commits)
    }

    @Test
    fun doneEndsEditingAndSaves() {
        noteField().requestFocus()
        noteField().performImeAction()

        noteField().assertIsNotFocused()
        assertEquals(1, commits)
    }
}
