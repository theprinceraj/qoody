package com.qoody.app.ui.receipt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.qoody.app.R
import com.qoody.app.ui.components.EntryDateField
import com.qoody.app.ui.components.PrimaryButton
import com.qoody.app.ui.components.QoodyModalSheet
import com.qoody.app.ui.components.QoodyTextField
import com.qoody.app.ui.components.SectionLabel
import com.qoody.app.ui.format.rememberDateFormats
import com.qoody.app.ui.theme.QoodyTheme
import com.qoody.shared.domain.format.MoneyFormatter
import com.qoody.shared.domain.model.Currency
import com.qoody.shared.domain.model.Money
import com.qoody.shared.feature.receipt.EntryEditor
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** Bottom sheet for correcting an entry's amount, merchant and date. */
@Composable
fun EditEntrySheet(
    editor: EntryEditor,
    currency: Currency,
    onAmountChange: (String) -> Unit,
    onMerchantChange: (String) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    QoodyModalSheet(onDismiss = onDismiss) {
        Text(
            text = stringResource(R.string.edit_entry_title),
            style = QoodyTheme.typography.headlineSm,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Column(verticalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.xs)) {
            SectionLabel(stringResource(R.string.add_expense_amount_label))
            QoodyTextField(
                value = editor.amountInput,
                onValueChange = onAmountChange,
                textStyle = QoodyTheme.typography.numericHero,
                minHeight = QoodyTheme.sizes.amountFieldHeight,
                placeholder = MoneyFormatter.formatPlain(Money.Zero),
                leading = {
                    Text(
                        text = currency.symbol,
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
                value = editor.merchant,
                onValueChange = onMerchantChange,
                placeholder = stringResource(R.string.add_expense_merchant_hint),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )
        }
        EntryDateField(date = editor.date, maxDate = editor.maxDate, onDateChange = onDateChange)
        PrimaryButton(
            text = stringResource(R.string.action_save),
            onClick = onSave,
            enabled = editor.canSave,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
