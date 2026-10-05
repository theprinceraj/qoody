package com.qoody.app.ui.budgets

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qoody.app.R
import com.qoody.app.ui.components.CategoryMarker
import com.qoody.app.ui.components.GhostButton
import com.qoody.app.ui.components.LoadingIndicator
import com.qoody.app.ui.components.PrimaryButton
import com.qoody.app.ui.components.ProgressTrack
import com.qoody.app.ui.components.QoodyCard
import com.qoody.app.ui.components.QoodyDetailTopBar
import com.qoody.app.ui.components.QoodyIcon
import com.qoody.app.ui.components.QoodyModalSheet
import com.qoody.app.ui.components.QoodyTextField
import com.qoody.app.ui.components.ScreenContainer
import com.qoody.app.ui.components.SectionLabel
import com.qoody.app.ui.format.rememberDateFormats
import com.qoody.app.ui.theme.QoodyTheme
import com.qoody.app.ui.theme.nameRes
import com.qoody.shared.domain.format.MoneyFormatter
import com.qoody.shared.domain.format.PercentFormatter
import com.qoody.shared.domain.model.Category
import com.qoody.shared.domain.model.Currency
import com.qoody.shared.domain.model.Money
import com.qoody.shared.feature.budgets.BudgetEditor
import com.qoody.shared.feature.budgets.BudgetRow
import com.qoody.shared.feature.budgets.BudgetsUiState
import com.qoody.shared.feature.budgets.BudgetsViewModel
import org.koin.androidx.compose.koinViewModel

/** Monthly spending limits per category, with this month's progress against each. */
@Composable
fun BudgetsScreen(
    onBack: () -> Unit,
    onProfileClick: () -> Unit,
    viewModel: BudgetsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BudgetsContent(
        state = state,
        onBack = onBack,
        onProfileClick = onProfileClick,
        onEdit = viewModel::onEdit,
    )
    (state as? BudgetsUiState.Content)?.editor?.let { editor ->
        BudgetSheet(
            editor = editor,
            currency = (state as BudgetsUiState.Content).currency,
            onAmountChange = viewModel::onAmountChanged,
            onSave = viewModel::onSave,
            onRemove = viewModel::onRemove,
            onDismiss = viewModel::onDismiss,
        )
    }
}

@Composable
fun BudgetsContent(
    state: BudgetsUiState,
    onBack: () -> Unit,
    onProfileClick: () -> Unit,
    onEdit: (Category) -> Unit,
) {
    ScreenContainer {
        Column(modifier = Modifier.fillMaxSize()) {
            QoodyDetailTopBar(
                title = stringResource(R.string.budgets_title),
                onBackClick = onBack,
                onProfileClick = onProfileClick,
            )
            when (state) {
                BudgetsUiState.Loading -> {
                    LoadingIndicator()
                }

                is BudgetsUiState.Content -> {
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
                                text = stringResource(R.string.budgets_intro, formats.monthName(state.month)),
                                style = QoodyTheme.typography.bodyMd,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                        items(state.rows, key = { it.category.name }) { row ->
                            BudgetCard(row, state.currency, onClick = { onEdit(row.category) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetCard(
    row: BudgetRow,
    currency: Currency,
    onClick: () -> Unit,
) {
    val progress = row.progress
    val colors = MaterialTheme.colorScheme
    val detailColor = if (progress.isOver) colors.error else colors.secondary
    val figureColor = if (progress.isOver) colors.error else colors.onSurface
    val barColor = if (progress.isOver) colors.error else colors.primaryContainer
    QoodyCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(QoodyTheme.shapes.card)
                .clickable(role = Role.Button, onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.cozy),
        ) {
            CategoryMarker(row.category)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(row.category.nameRes),
                    style = QoodyTheme.typography.titleMd,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = budgetLine(progress.spent, progress.limit, currency),
                    style = QoodyTheme.typography.bodySm,
                    color = detailColor,
                )
            }
            progress.used?.let {
                Text(
                    text = PercentFormatter.formatWhole(it),
                    style = QoodyTheme.typography.numericMd,
                    color = figureColor,
                )
            } ?: QoodyIcon(
                R.drawable.ic_add,
                contentDescription = stringResource(R.string.budgets_set),
                tint = MaterialTheme.colorScheme.secondary,
            )
        }
        progress.used?.let { used ->
            ProgressTrack(
                fraction = used.fraction,
                color = barColor,
                modifier = Modifier.padding(top = QoodyTheme.spacing.sm),
            )
        }
    }
}

/** "₹1,240.00 of ₹2,000.00" with a budget, or "₹1,240.00 spent · no budget" without. */
@Composable
private fun budgetLine(
    spent: Money,
    limit: Money?,
    currency: Currency,
): String =
    if (limit == null) {
        stringResource(R.string.budgets_no_budget, MoneyFormatter.format(spent, currency))
    } else {
        stringResource(
            R.string.budgets_spent_of,
            MoneyFormatter.format(spent, currency),
            MoneyFormatter.format(limit, currency),
        )
    }

@Composable
private fun BudgetSheet(
    editor: BudgetEditor,
    currency: Currency,
    onAmountChange: (String) -> Unit,
    onSave: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    QoodyModalSheet(onDismiss = onDismiss) {
        Text(
            text = stringResource(R.string.budgets_sheet_title, stringResource(editor.category.nameRes)),
            style = QoodyTheme.typography.headlineSm,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Column(verticalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.xs)) {
            SectionLabel(stringResource(R.string.budgets_monthly_limit))
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
        PrimaryButton(
            text = stringResource(R.string.action_save),
            onClick = onSave,
            enabled = editor.canSave,
            modifier = Modifier.fillMaxWidth(),
        )
        if (editor.hasBudget) {
            GhostButton(
                text = stringResource(R.string.budgets_remove),
                onClick = onRemove,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
