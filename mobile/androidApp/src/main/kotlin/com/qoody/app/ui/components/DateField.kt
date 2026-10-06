package com.qoody.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.qoody.app.R
import com.qoody.app.ui.format.rememberDateFormats
import com.qoody.app.ui.theme.QoodyTheme
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** A labelled button showing [date]; it opens a date picker that offers no day after [maxDate]. */
@Composable
fun EntryDateField(
    date: LocalDate,
    maxDate: LocalDate,
    onDateChange: (LocalDate) -> Unit,
) {
    var isPicking by rememberSaveable { mutableStateOf(false) }
    val formats = rememberDateFormats()
    Column(verticalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.xs)) {
        SectionLabel(stringResource(R.string.edit_entry_date_label))
        PaperButton(
            text = formats.weekdayMonthDay(date),
            onClick = { isPicking = true },
            leadingIcon = R.drawable.ic_calendar_today,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    if (isPicking) {
        EntryDatePicker(
            selected = date,
            maxDate = maxDate,
            onPicked = {
                onDateChange(it)
                isPicking = false
            },
            onDismiss = { isPicking = false },
        )
    }
}

/** Material's date picker works in UTC midnights; entries never lie in the future. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryDatePicker(
    selected: LocalDate,
    maxDate: LocalDate,
    onPicked: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val latest = maxDate.toUtcMillis()
    val state =
        rememberDatePickerState(
            initialSelectedDateMillis = selected.toUtcMillis(),
            selectableDates =
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= latest

                    override fun isSelectableYear(year: Int) = year <= maxDate.year
                },
        )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { state.selectedDateMillis?.let { onPicked(it.toUtcDate()) } ?: onDismiss() },
            ) { Text(stringResource(R.string.edit_entry_date_confirm)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    ) {
        DatePicker(state = state)
    }
}

private fun LocalDate.toUtcMillis(): Long = atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

private fun Long.toUtcDate(): LocalDate = Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.UTC).date
