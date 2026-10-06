package com.qoody.app.ui.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qoody.app.R
import com.qoody.app.ui.components.EmptyState
import com.qoody.app.ui.components.GhostButton
import com.qoody.app.ui.components.LoadingIndicator
import com.qoody.app.ui.components.QoodyCard
import com.qoody.app.ui.components.QoodyDetailTopBar
import com.qoody.app.ui.components.QoodyIcon
import com.qoody.app.ui.components.QoodyInset
import com.qoody.app.ui.components.ScreenContainer
import com.qoody.app.ui.components.TonalButton
import com.qoody.app.ui.format.DateFormats
import com.qoody.app.ui.format.rememberDateFormats
import com.qoody.app.ui.ledger.AddExpenseSheet
import com.qoody.app.ui.theme.QoodyTheme
import com.qoody.shared.capture.UnparsedReason
import com.qoody.shared.feature.capture.UnparsedCaptureItem
import com.qoody.shared.feature.capture.UnparsedCapturesUiState
import com.qoody.shared.feature.capture.UnparsedCapturesViewModel
import org.koin.androidx.compose.koinViewModel

/** Payment notifications Qoody could not read, so the user can type them in or dismiss them. */
@Composable
fun UnparsedCapturesScreen(
    onBack: () -> Unit,
    viewModel: UnparsedCapturesViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var addingFor by rememberSaveable { mutableStateOf<Long?>(null) }

    UnparsedCapturesContent(
        state = state,
        onBack = onBack,
        onAddManually = { addingFor = it },
        onDismiss = viewModel::onDismiss,
        onClearAll = viewModel::onClearAll,
    )

    addingFor?.let { id ->
        AddExpenseSheet(
            onDismiss = { addingFor = null },
            onSaved = {
                viewModel.onAddedManually(id)
                addingFor = null
            },
        )
    }
}

@Composable
fun UnparsedCapturesContent(
    state: UnparsedCapturesUiState,
    onBack: () -> Unit,
    onAddManually: (Long) -> Unit,
    onDismiss: (Long) -> Unit,
    onClearAll: () -> Unit,
) {
    ScreenContainer {
        Column(modifier = Modifier.fillMaxSize()) {
            QoodyDetailTopBar(
                title = stringResource(R.string.unparsed_title),
                onBackClick = onBack,
            )
            when (state) {
                UnparsedCapturesUiState.Loading -> {
                    LoadingIndicator()
                }

                is UnparsedCapturesUiState.Content -> {
                    if (state.items.isEmpty()) {
                        EmptyState(
                            title = stringResource(R.string.unparsed_empty_title),
                            body = stringResource(R.string.unparsed_empty_body),
                        )
                    } else {
                        UnparsedList(state.items, onAddManually, onDismiss, onClearAll)
                    }
                }
            }
        }
    }
}

@Composable
private fun UnparsedList(
    items: List<UnparsedCaptureItem>,
    onAddManually: (Long) -> Unit,
    onDismiss: (Long) -> Unit,
    onClearAll: () -> Unit,
) {
    val formats = rememberDateFormats()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                start = QoodyTheme.spacing.md,
                end = QoodyTheme.spacing.md,
                bottom = QoodyTheme.spacing.lg,
            ),
        verticalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.md),
    ) {
        item {
            Text(
                text = stringResource(R.string.unparsed_intro),
                style = QoodyTheme.typography.bodyMd,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        items(items, key = { it.id }) { item ->
            UnparsedCard(item, formats, onAddManually = { onAddManually(item.id) }, onDismiss = { onDismiss(item.id) })
        }
        item {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                GhostButton(text = stringResource(R.string.unparsed_clear_all), onClick = onClearAll)
            }
        }
    }
}

@Composable
private fun UnparsedCard(
    item: UnparsedCaptureItem,
    formats: DateFormats,
    onAddManually: () -> Unit,
    onDismiss: () -> Unit,
) {
    QoodyCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.sm),
            ) {
                Surface(
                    modifier = Modifier.size(QoodyTheme.sizes.settingIconBox),
                    shape = QoodyTheme.shapes.field,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        QoodyIcon(
                            R.drawable.ic_error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
                Column {
                    Text(
                        text = item.appName,
                        style = QoodyTheme.typography.bodySmMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(item.reason.labelRes),
                        style = QoodyTheme.typography.bodySm,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
            Text(
                text = stringResource(R.string.unparsed_when, formats.monthDay(item.date), formats.time(item.time)),
                style = QoodyTheme.typography.numericSm,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
        QoodyInset(modifier = Modifier.fillMaxWidth().padding(vertical = QoodyTheme.spacing.sm)) {
            Text(
                text = item.text,
                style = QoodyTheme.typography.bodySm,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.sm)) {
            TonalButton(
                text = stringResource(R.string.unparsed_add_manually),
                onClick = onAddManually,
                leadingIcon = R.drawable.ic_add,
                modifier = Modifier.weight(1f),
            )
            GhostButton(
                text = stringResource(R.string.unparsed_dismiss),
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private val UnparsedReason.labelRes: Int
    get() =
        when (this) {
            UnparsedReason.NoAmount -> R.string.unparsed_reason_no_amount
            UnparsedReason.InvalidAmount -> R.string.unparsed_reason_invalid_amount
        }
