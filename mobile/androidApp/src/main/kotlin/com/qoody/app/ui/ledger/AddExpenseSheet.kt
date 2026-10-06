package com.qoody.app.ui.ledger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qoody.app.R
import com.qoody.app.ui.components.CategoryPill
import com.qoody.app.ui.components.EntryDateField
import com.qoody.app.ui.components.PrimaryButton
import com.qoody.app.ui.components.QoodyModalSheet
import com.qoody.app.ui.components.QoodyTextField
import com.qoody.app.ui.components.SectionLabel
import com.qoody.app.ui.theme.QoodyTheme
import com.qoody.shared.domain.format.MoneyFormatter
import com.qoody.shared.domain.model.Category
import com.qoody.shared.domain.model.Money
import com.qoody.shared.feature.ledger.AddExpenseEvent
import com.qoody.shared.feature.ledger.AddExpenseUiState
import com.qoody.shared.feature.ledger.AddExpenseViewModel
import kotlinx.datetime.LocalDate
import org.koin.androidx.compose.koinViewModel

/** Bottom sheet for typing in a payment by hand. [onSaved] runs instead of [onDismiss] after a save. */
@Composable
fun AddExpenseSheet(
    onDismiss: () -> Unit,
    onSaved: () -> Unit = onDismiss,
    viewModel: AddExpenseViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                AddExpenseEvent.Saved -> onSaved()
            }
        }
    }

    QoodyModalSheet(onDismiss = onDismiss) {
        AddExpenseForm(
            state = state,
            onAmountChange = viewModel::onAmountChanged,
            onMerchantChange = viewModel::onMerchantChanged,
            onCategorySelected = viewModel::onCategorySelected,
            onDateChange = viewModel::onDateChanged,
            onSave = viewModel::onSave,
        )
    }
}

@Composable
private fun AddExpenseForm(
    state: AddExpenseUiState,
    onAmountChange: (String) -> Unit,
    onMerchantChange: (String) -> Unit,
    onCategorySelected: (Category) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onSave: () -> Unit,
) {
    Text(
        text = stringResource(R.string.add_expense_title),
        style = QoodyTheme.typography.headlineSm,
        color = MaterialTheme.colorScheme.onSurface,
    )

    Column(verticalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.xs)) {
        SectionLabel(stringResource(R.string.add_expense_amount_label))
        QoodyTextField(
            value = state.amountInput,
            onValueChange = onAmountChange,
            textStyle = QoodyTheme.typography.numericHero,
            minHeight = QoodyTheme.sizes.amountFieldHeight,
            placeholder = MoneyFormatter.formatPlain(Money.Zero),
            leading = {
                Text(
                    text = MoneyFormatter.SYMBOL,
                    style = QoodyTheme.typography.numericHero,
                    color = MaterialTheme.colorScheme.primaryContainer,
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.xs)) {
        SectionLabel(stringResource(R.string.add_expense_merchant_label))
        QoodyTextField(
            value = state.merchant,
            onValueChange = onMerchantChange,
            placeholder = stringResource(R.string.add_expense_merchant_hint),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.sm)) {
        SectionLabel(stringResource(R.string.add_expense_category_label))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.sm),
        ) {
            Category.entries.forEach { category ->
                CategoryPill(
                    category = category,
                    selected = category == state.category,
                    onClick = { onCategorySelected(category) },
                )
            }
        }
    }

    val date = state.date
    val maxDate = state.maxDate
    if (date != null && maxDate != null) EntryDateField(date = date, maxDate = maxDate, onDateChange = onDateChange)

    Row(verticalAlignment = Alignment.CenterVertically) {
        PrimaryButton(
            text = stringResource(R.string.action_save),
            onClick = onSave,
            enabled = state.canSave,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
