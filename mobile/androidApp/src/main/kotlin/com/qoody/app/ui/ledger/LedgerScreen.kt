package com.qoody.app.ui.ledger

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qoody.app.R
import com.qoody.app.ui.components.CategoryMarker
import com.qoody.app.ui.components.EmptyState
import com.qoody.app.ui.components.FilterPill
import com.qoody.app.ui.components.LoadingIndicator
import com.qoody.app.ui.components.PrimaryButton
import com.qoody.app.ui.components.QoodyCard
import com.qoody.app.ui.components.QoodyIcon
import com.qoody.app.ui.components.QoodyTextField
import com.qoody.app.ui.components.QoodyTopBar
import com.qoody.app.ui.components.ScreenContainer
import com.qoody.app.ui.components.SectionLabel
import com.qoody.app.ui.components.StatusPill
import com.qoody.app.ui.components.TagChip
import com.qoody.app.ui.format.DateFormats
import com.qoody.app.ui.format.StyledArg
import com.qoody.app.ui.format.rememberDateFormats
import com.qoody.app.ui.format.styledStringResource
import com.qoody.app.ui.theme.QoodyTheme
import com.qoody.app.ui.theme.chipLabelRes
import com.qoody.shared.domain.format.MoneyFormatter
import com.qoody.shared.domain.format.PercentFormatter
import com.qoody.shared.domain.format.SignStyle
import com.qoody.shared.domain.model.Category
import com.qoody.shared.domain.model.Currency
import com.qoody.shared.domain.model.TransactionId
import com.qoody.shared.feature.ledger.DayGroup
import com.qoody.shared.feature.ledger.DayLabel
import com.qoody.shared.feature.ledger.LedgerRow
import com.qoody.shared.feature.ledger.LedgerUiState
import com.qoody.shared.feature.ledger.LedgerViewModel
import com.qoody.shared.feature.ledger.MonthSummary
import com.qoody.shared.feature.ledger.SearchState
import com.qoody.shared.feature.ledger.TrendDirection
import org.koin.androidx.compose.koinViewModel

/** The ledger tab: this month's total, category filters and the day-by-day list of payments. */
@Composable
fun LedgerScreen(
    onOpenReceipt: (TransactionId) -> Unit,
    onProfileClick: () -> Unit,
    startWithSearch: Boolean,
    viewModel: LedgerViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddExpense by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(startWithSearch) { if (startWithSearch) viewModel.onSearchOpened() }

    LedgerContent(
        state = state,
        onSearchClick = viewModel::onSearchOpened,
        onSearchQueryChange = viewModel::onSearchQueryChanged,
        onSearchClose = viewModel::onSearchClosed,
        onProfileClick = onProfileClick,
        onCategorySelected = viewModel::onCategorySelected,
        onRowClick = onOpenReceipt,
        onAddExpenseClick = { showAddExpense = true },
    )

    if (showAddExpense) AddExpenseSheet(onDismiss = { showAddExpense = false })
}

@Composable
fun LedgerContent(
    state: LedgerUiState,
    onSearchClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchClose: () -> Unit,
    onProfileClick: () -> Unit,
    onCategorySelected: (Category?) -> Unit,
    onRowClick: (TransactionId) -> Unit,
    onAddExpenseClick: () -> Unit,
) {
    ScreenContainer {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                QoodyTopBar(onSearchClick = onSearchClick, onProfileClick = onProfileClick)
                when (state) {
                    LedgerUiState.Loading -> {
                        LoadingIndicator()
                    }

                    is LedgerUiState.Content -> {
                        LedgerList(
                            state = state,
                            onSearchQueryChange = onSearchQueryChange,
                            onSearchClose = onSearchClose,
                            onCategorySelected = onCategorySelected,
                            onRowClick = onRowClick,
                        )
                    }
                }
            }
            if (state is LedgerUiState.Content) {
                PrimaryButton(
                    text = stringResource(R.string.ledger_add_expense),
                    onClick = onAddExpenseClick,
                    leadingIcon = R.drawable.ic_add,
                    compact = true,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(QoodyTheme.spacing.md),
                )
            }
        }
    }
}

@Composable
private fun LedgerList(
    state: LedgerUiState.Content,
    onSearchQueryChange: (String) -> Unit,
    onSearchClose: () -> Unit,
    onCategorySelected: (Category?) -> Unit,
    onRowClick: (TransactionId) -> Unit,
) {
    val formats = rememberDateFormats()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                start = QoodyTheme.spacing.md,
                end = QoodyTheme.spacing.md,
                bottom = QoodyTheme.sizes.heroButtonHeight + QoodyTheme.spacing.xl,
            ),
        verticalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.md),
    ) {
        if (state.search.isActive) {
            item(key = SEARCH_KEY) { SearchField(state.search, onSearchQueryChange, onSearchClose) }
        }
        item(key = SUMMARY_KEY) { SummaryCard(state.summary, state.currency, formats) }
        item(key = FILTERS_KEY) {
            CategoryFilters(
                selected = state.selectedCategory,
                onSelected = onCategorySelected,
            )
        }
        if (state.dayGroups.isEmpty()) {
            item(key = EMPTY_KEY) {
                if (state.isFiltered) {
                    EmptyState(
                        title = stringResource(R.string.ledger_no_results_title),
                        body = stringResource(R.string.ledger_no_results_body),
                    )
                } else {
                    EmptyState(
                        title = stringResource(R.string.ledger_empty_title),
                        body = stringResource(R.string.ledger_empty_body),
                    )
                }
            }
        }
        items(items = state.dayGroups, key = { it.date.toString() }) { group ->
            DayGroupCard(group, state.currency, state.hapticsEnabled, formats, onRowClick)
        }
    }
}

private const val SEARCH_KEY = "search"
private const val SUMMARY_KEY = "summary"
private const val FILTERS_KEY = "filters"
private const val EMPTY_KEY = "empty"
private const val ALL_FILTER_KEY = "all"

@Composable
private fun SearchField(
    search: SearchState,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    QoodyTextField(
        value = search.query,
        onValueChange = onQueryChange,
        modifier = Modifier.focusRequester(focusRequester),
        placeholder = stringResource(R.string.ledger_search_placeholder),
        leading = {
            QoodyIcon(R.drawable.ic_search, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        },
        trailing = {
            IconButton(onClick = onClose, modifier = Modifier.heightIn(min = QoodyTheme.sizes.touchTarget)) {
                QoodyIcon(
                    id = R.drawable.ic_close,
                    contentDescription = stringResource(R.string.cd_close_search),
                    tint = MaterialTheme.colorScheme.secondary,
                )
            }
        },
    )
}

@Composable
private fun SummaryCard(
    summary: MonthSummary,
    currency: Currency,
    formats: DateFormats,
) {
    QoodyCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionLabel(
                text = stringResource(R.string.ledger_spent_this_month, formats.monthName(summary.month)),
                modifier = Modifier.weight(1f, fill = false),
            )
            StatusPill(
                label = stringResource(R.string.ledger_active_cycle),
                dotColor = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.padding(start = QoodyTheme.spacing.sm),
            )
        }
        Row(
            modifier = Modifier.padding(top = QoodyTheme.spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.xs),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = currency.symbol,
                style = QoodyTheme.typography.numericHero.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primaryContainer,
            )
            Text(
                text = MoneyFormatter.formatPlain(summary.spent),
                style = QoodyTheme.typography.numericHero,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        summary.trend?.let { trend ->
            Row(
                modifier = Modifier.padding(top = QoodyTheme.spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val previous = MoneyFormatter.format(trend.previousSpent, currency)
                when (trend.direction) {
                    TrendDirection.Same -> {
                        TrendText(stringResource(R.string.ledger_trend_same, previous))
                    }

                    else -> {
                        QoodyIcon(
                            id =
                                if (trend.direction ==
                                    TrendDirection.Lower
                                ) {
                                    R.drawable.ic_arrow_downward
                                } else {
                                    R.drawable.ic_arrow_upward
                                },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primaryContainer,
                            size = QoodyTheme.sizes.iconSm,
                        )
                        Text(
                            text =
                                styledStringResource(
                                    R.string.ledger_trend_comparison,
                                    StyledArg(
                                        PercentFormatter.formatWhole(trend.change),
                                        SpanStyle(
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        ),
                                    ),
                                    StyledArg(previous),
                                ),
                            style = QoodyTheme.typography.bodySm,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
        }
        ProgressTrack(
            fraction = summary.progress.fraction,
            modifier = Modifier.padding(top = QoodyTheme.spacing.md),
        )
    }
}

@Composable
private fun TrendText(text: String) {
    Text(text = text, style = QoodyTheme.typography.bodySm, color = MaterialTheme.colorScheme.secondary)
}

/** A thin pill-shaped bar showing how far this month's spending has come against last month's. */
@Composable
private fun ProgressTrack(
    fraction: Float,
    modifier: Modifier = Modifier,
) {
    val animated by animateFloatAsState(fraction, tween(QoodyTheme.motion.chartMillis))
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = QoodyTheme.sizes.progressTrack)
                .clip(QoodyTheme.shapes.pill)
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth(animated)
                    .heightIn(min = QoodyTheme.sizes.progressTrack)
                    .clip(QoodyTheme.shapes.pill)
                    .background(MaterialTheme.colorScheme.primaryContainer),
        )
    }
}

@Composable
private fun CategoryFilters(
    selected: Category?,
    onSelected: (Category?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.sm)) {
        item(key = ALL_FILTER_KEY) {
            FilterPill(
                label = stringResource(R.string.ledger_filter_all),
                selected = selected == null,
                onClick = { onSelected(null) },
            )
        }
        items(items = Category.entries, key = { it.name }) { category ->
            FilterPill(
                label = stringResource(category.chipLabelRes),
                selected = selected == category,
                onClick = { onSelected(category) },
            )
        }
    }
}

@Composable
private fun DayGroupCard(
    group: DayGroup,
    currency: Currency,
    hapticsEnabled: Boolean,
    formats: DateFormats,
    onRowClick: (TransactionId) -> Unit,
) {
    QoodyCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(QoodyTheme.spacing.md)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = QoodyTheme.spacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionLabel(text = dayHeading(group, formats))
            Text(
                text = MoneyFormatter.format(group.total, currency, SignStyle.Outflow),
                style = QoodyTheme.typography.numericMd,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        group.rows.forEach { row ->
            LedgerRowItem(row, currency, hapticsEnabled, formats, onClick = { onRowClick(row.id) })
        }
    }
}

@Composable
private fun dayHeading(
    group: DayGroup,
    formats: DateFormats,
): String =
    when (group.label) {
        DayLabel.Today -> stringResource(R.string.ledger_day_today, formats.monthDay(group.date))
        DayLabel.Yesterday -> stringResource(R.string.ledger_day_yesterday, formats.monthDay(group.date))
        DayLabel.Other -> formats.shortWeekdayMonthDay(group.date)
    }

@Composable
private fun LedgerRowItem(
    row: LedgerRow,
    currency: Currency,
    hapticsEnabled: Boolean,
    formats: DateFormats,
    onClick: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(QoodyTheme.shapes.tile)
                .clickable(role = Role.Button) {
                    if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onClick()
                }.heightIn(min = QoodyTheme.sizes.rowMinHeight)
                .padding(horizontal = QoodyTheme.spacing.sm, vertical = QoodyTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.cozy),
        ) {
            CategoryMarker(row.category)
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.sm),
                ) {
                    Text(
                        text = row.merchant,
                        style = QoodyTheme.typography.titleMd,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    row.tag?.let { TagChip(it) }
                }
                Text(
                    text = stringResource(R.string.ledger_row_subtitle, row.paymentApp, formats.time(row.time)),
                    style = QoodyTheme.typography.bodySm,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            text = MoneyFormatter.format(row.amount, currency, SignStyle.Outflow),
            style = QoodyTheme.typography.numericMd,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = QoodyTheme.spacing.cozy),
        )
    }
}
