package com.qoody.app.ui.excluded

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qoody.app.R
import com.qoody.app.ui.components.CategoryMarker
import com.qoody.app.ui.components.EmptyState
import com.qoody.app.ui.components.LoadingIndicator
import com.qoody.app.ui.components.QoodyCard
import com.qoody.app.ui.components.QoodyDetailTopBar
import com.qoody.app.ui.components.ScreenContainer
import com.qoody.app.ui.components.TonalButton
import com.qoody.app.ui.format.DateFormats
import com.qoody.app.ui.format.rememberDateFormats
import com.qoody.app.ui.theme.QoodyTheme
import com.qoody.shared.domain.format.MoneyFormatter
import com.qoody.shared.domain.format.SignStyle
import com.qoody.shared.domain.model.Currency
import com.qoody.shared.domain.model.TransactionId
import com.qoody.shared.feature.excluded.ExcludedEntriesUiState
import com.qoody.shared.feature.excluded.ExcludedEntriesViewModel
import com.qoody.shared.feature.excluded.ExcludedEntry
import org.koin.androidx.compose.koinViewModel

/** Entries the user excluded from the ledger, each with a way back in. */
@Composable
fun ExcludedEntriesScreen(
    onBack: () -> Unit,
    onProfileClick: () -> Unit,
    onOpenReceipt: (TransactionId) -> Unit,
    viewModel: ExcludedEntriesViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ExcludedEntriesContent(
        state = state,
        onBack = onBack,
        onProfileClick = onProfileClick,
        onOpenReceipt = onOpenReceipt,
        onRestore = viewModel::onRestore,
    )
}

@Composable
fun ExcludedEntriesContent(
    state: ExcludedEntriesUiState,
    onBack: () -> Unit,
    onProfileClick: () -> Unit,
    onOpenReceipt: (TransactionId) -> Unit,
    onRestore: (TransactionId) -> Unit,
) {
    ScreenContainer {
        Column(modifier = Modifier.fillMaxSize()) {
            QoodyDetailTopBar(
                title = stringResource(R.string.excluded_title),
                onBackClick = onBack,
                onProfileClick = onProfileClick,
            )
            when (state) {
                ExcludedEntriesUiState.Loading -> {
                    LoadingIndicator()
                }

                is ExcludedEntriesUiState.Content -> {
                    if (state.entries.isEmpty()) {
                        EmptyState(
                            title = stringResource(R.string.excluded_empty_title),
                            body = stringResource(R.string.excluded_empty_body),
                        )
                    } else {
                        ExcludedList(state, onOpenReceipt, onRestore)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExcludedList(
    state: ExcludedEntriesUiState.Content,
    onOpenReceipt: (TransactionId) -> Unit,
    onRestore: (TransactionId) -> Unit,
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
                text = stringResource(R.string.excluded_intro),
                style = QoodyTheme.typography.bodyMd,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        items(state.entries, key = { it.id.value }) { entry ->
            ExcludedCard(
                entry = entry,
                currency = state.currency,
                formats = formats,
                onOpen = { onOpenReceipt(entry.id) },
                onRestore = { onRestore(entry.id) },
            )
        }
    }
}

@Composable
private fun ExcludedCard(
    entry: ExcludedEntry,
    currency: Currency,
    formats: DateFormats,
    onOpen: () -> Unit,
    onRestore: () -> Unit,
) {
    QoodyCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onOpen),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.cozy),
        ) {
            CategoryMarker(entry.category)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.merchant,
                    style = QoodyTheme.typography.titleMd,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text =
                        stringResource(
                            R.string.excluded_row_subtitle,
                            entry.paymentApp,
                            formats.monthDay(entry.date),
                            formats.time(entry.time),
                        ),
                    style = QoodyTheme.typography.bodySm,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = MoneyFormatter.format(entry.amount, currency, SignStyle.Outflow),
                style = QoodyTheme.typography.numericMd,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        TonalButton(
            text = stringResource(R.string.excluded_restore),
            onClick = onRestore,
            leadingIcon = R.drawable.ic_refresh,
            modifier = Modifier.fillMaxWidth().padding(top = QoodyTheme.spacing.sm),
        )
    }
}
