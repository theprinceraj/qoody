package com.qoody.app.ui.receipt

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qoody.app.R
import com.qoody.app.ui.components.CategoryPill
import com.qoody.app.ui.components.EmptyState
import com.qoody.app.ui.components.FieldStyle
import com.qoody.app.ui.components.GhostButton
import com.qoody.app.ui.components.LoadingIndicator
import com.qoody.app.ui.components.PaperButton
import com.qoody.app.ui.components.PerforationRule
import com.qoody.app.ui.components.PrimaryButton
import com.qoody.app.ui.components.ProgressTrack
import com.qoody.app.ui.components.QoodyCard
import com.qoody.app.ui.components.QoodyDetailTopBar
import com.qoody.app.ui.components.QoodyIcon
import com.qoody.app.ui.components.QoodyInset
import com.qoody.app.ui.components.QoodyModalSheet
import com.qoody.app.ui.components.QoodyTextField
import com.qoody.app.ui.components.ScreenContainer
import com.qoody.app.ui.components.SectionLabel
import com.qoody.app.ui.components.SheetHandle
import com.qoody.app.ui.components.StatusPill
import com.qoody.app.ui.components.TonalButton
import com.qoody.app.ui.format.DateFormats
import com.qoody.app.ui.format.rememberDateFormats
import com.qoody.app.ui.theme.QoodyTheme
import com.qoody.app.ui.theme.nameRes
import com.qoody.shared.domain.format.MoneyFormatter
import com.qoody.shared.domain.format.PercentFormatter
import com.qoody.shared.domain.model.Categorization
import com.qoody.shared.domain.model.Category
import com.qoody.shared.domain.model.EntrySource
import com.qoody.shared.domain.model.EntryStatus
import com.qoody.shared.feature.receipt.BudgetImpact
import com.qoody.shared.feature.receipt.ReceiptEvent
import com.qoody.shared.feature.receipt.ReceiptUiState
import com.qoody.shared.feature.receipt.ReceiptViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/** Everything about a single payment, with controls to correct or discard it. */
@Composable
fun ReceiptScreen(
    transactionId: Long,
    onBack: () -> Unit,
    onProfileClick: () -> Unit,
    onOpenBudgets: () -> Unit = {},
    viewModel: ReceiptViewModel = koinViewModel(key = transactionId.toString()) { parametersOf(transactionId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val splitUnavailable = stringResource(R.string.receipt_split_unavailable)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                ReceiptEvent.Close -> onBack()
            }
        }
    }
    DisposableEffect(viewModel) { onDispose { viewModel.onNoteCommitted() } }

    ReceiptContent(
        state = state,
        snackbarHost = snackbarHost,
        onBack = onBack,
        onProfileClick = onProfileClick,
        onNoteChange = viewModel::onNoteChanged,
        onNoteCommit = viewModel::onNoteCommitted,
        onChangeCategory = viewModel::onCategoryPickerRequested,
        onCategorySelected = viewModel::onCategorySelected,
        onCategoryPickerDismiss = viewModel::onCategoryPickerDismissed,
        onSplit = { scope.launch { snackbarHost.showSnackbar(splitUnavailable) } },
        onKeep = viewModel::onKeepEntry,
        onExclude = viewModel::onExcludeFromLedger,
        onRestore = viewModel::onRestoreToLedger,
        onOpenBudgets = onOpenBudgets,
        editActions =
            EntryEditorActions(
                onOpen = viewModel::onEditRequested,
                onAmountChange = viewModel::onEditAmountChanged,
                onMerchantChange = viewModel::onEditMerchantChanged,
                onDateChange = viewModel::onEditDateChanged,
                onSave = viewModel::onEditSaved,
                onDismiss = viewModel::onEditDismissed,
            ),
    )
}

@Composable
fun ReceiptContent(
    state: ReceiptUiState,
    snackbarHost: SnackbarHostState,
    onBack: () -> Unit,
    onProfileClick: () -> Unit,
    onNoteChange: (String) -> Unit,
    onNoteCommit: () -> Unit,
    onChangeCategory: () -> Unit,
    onCategorySelected: (Category) -> Unit,
    onCategoryPickerDismiss: () -> Unit,
    onSplit: () -> Unit,
    onKeep: () -> Unit,
    onExclude: () -> Unit,
    onRestore: () -> Unit,
    editActions: EntryEditorActions = EntryEditorActions.None,
    onOpenBudgets: () -> Unit = {},
) {
    ScreenContainer {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                QoodyDetailTopBar(
                    title = stringResource(R.string.receipt_title),
                    onBackClick = onBack,
                    onProfileClick = onProfileClick,
                )
                when (state) {
                    ReceiptUiState.Loading -> {
                        LoadingIndicator()
                    }

                    ReceiptUiState.NotFound -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            EmptyState(title = stringResource(R.string.receipt_not_found))
                            GhostButton(text = stringResource(R.string.cd_back), onClick = onBack)
                        }
                    }

                    is ReceiptUiState.Content -> {
                        ReceiptBody(
                            state = state,
                            onNoteChange = onNoteChange,
                            onNoteCommit = onNoteCommit,
                            onChangeCategory = onChangeCategory,
                            onSplit = onSplit,
                            onKeep = onKeep,
                            onExclude = onExclude,
                            onRestore = onRestore,
                            onEdit = editActions.onOpen,
                            onOpenBudgets = onOpenBudgets,
                        )
                    }
                }
            }
            SnackbarHost(hostState = snackbarHost, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }

    if (state is ReceiptUiState.Content) {
        state.editor?.let { editor ->
            EditEntrySheet(
                editor = editor,
                currency = state.currency,
                onAmountChange = editActions.onAmountChange,
                onMerchantChange = editActions.onMerchantChange,
                onDateChange = editActions.onDateChange,
                onSave = editActions.onSave,
                onDismiss = editActions.onDismiss,
            )
        }
    }

    if (state is ReceiptUiState.Content && state.isCategoryPickerOpen) {
        CategoryPickerSheet(
            selected = state.category,
            onSelected = onCategorySelected,
            onDismiss = onCategoryPickerDismiss,
        )
    }
}

@Composable
private fun ReceiptBody(
    state: ReceiptUiState.Content,
    onNoteChange: (String) -> Unit,
    onNoteCommit: () -> Unit,
    onChangeCategory: () -> Unit,
    onSplit: () -> Unit,
    onKeep: () -> Unit,
    onExclude: () -> Unit,
    onRestore: () -> Unit,
    onEdit: () -> Unit,
    onOpenBudgets: () -> Unit,
) {
    val formats = rememberDateFormats()
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = QoodyTheme.spacing.md)
                .padding(bottom = QoodyTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.md),
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { SheetHandle() }
        MainCard(state, formats, onNoteChange, onNoteCommit, onChangeCategory, onEdit)
        state.notification?.let { NotificationCard(it.appName, it.text, formats.time(state.time)) }
        DetailsCard(state, onOpenBudgets)
        Column(verticalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.sm)) {
            PaperButton(
                text = stringResource(R.string.receipt_split),
                onClick = onSplit,
                leadingIcon = R.drawable.ic_call_split,
                modifier = Modifier.fillMaxWidth(),
            )
            PrimaryButton(
                text = stringResource(R.string.receipt_keep),
                onClick = onKeep,
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.status == EntryStatus.Excluded) {
                TonalButton(
                    text = stringResource(R.string.receipt_restore),
                    onClick = onRestore,
                    leadingIcon = R.drawable.ic_refresh,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                GhostButton(
                    text = stringResource(R.string.receipt_exclude),
                    onClick = onExclude,
                    leadingIcon = R.drawable.ic_visibility_off,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun MainCard(
    state: ReceiptUiState.Content,
    formats: DateFormats,
    onNoteChange: (String) -> Unit,
    onNoteCommit: () -> Unit,
    onChangeCategory: () -> Unit,
    onEdit: () -> Unit,
) {
    QoodyCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(QoodyTheme.spacing.lg)) {
        PerforationRule(modifier = Modifier.padding(bottom = QoodyTheme.spacing.md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusPill(
                label =
                    stringResource(
                        if (state.status ==
                            EntryStatus.Settled
                        ) {
                            R.string.receipt_status_settled
                        } else {
                            R.string.receipt_status_excluded
                        },
                    ),
                dotColor = MaterialTheme.colorScheme.primaryContainer,
                textStyle = QoodyTheme.typography.bodySmMedium,
            )
            Text(
                text = stringResource(R.string.receipt_code, state.code),
                style = QoodyTheme.typography.numericMd,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = QoodyTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = state.merchant,
                style = QoodyTheme.typography.headlineMd,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onEdit, modifier = Modifier.size(QoodyTheme.sizes.touchTarget)) {
                QoodyIcon(
                    R.drawable.ic_edit,
                    contentDescription = stringResource(R.string.edit_entry_title),
                    tint = MaterialTheme.colorScheme.secondary,
                )
            }
        }
        Row(
            modifier = Modifier.padding(top = QoodyTheme.spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.xs),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = MoneyFormatter.format(state.amount, state.currency),
                style = QoodyTheme.typography.numericHero,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = state.currency.isoCode,
                style = QoodyTheme.typography.bodySm,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(bottom = QoodyTheme.spacing.xs),
            )
        }
        Text(
            text =
                stringResource(
                    if (state.source ==
                        EntrySource.Manual
                    ) {
                        R.string.receipt_meta_manual
                    } else {
                        R.string.receipt_meta_captured
                    },
                    formats.weekdayMonthDay(state.date),
                    formats.time(state.time),
                    state.paymentApp,
                ),
            style = QoodyTheme.typography.bodySm,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = QoodyTheme.spacing.xs, bottom = QoodyTheme.spacing.md),
        )

        CategoryPanel(state, onChangeCategory)

        Column(
            modifier = Modifier.padding(top = QoodyTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.xs),
        ) {
            SectionLabel(stringResource(R.string.receipt_personal_note), color = MaterialTheme.colorScheme.tertiary)
            QoodyTextField(
                value = state.note,
                onValueChange = onNoteChange,
                modifier = Modifier.onFocusChanged { if (!it.hasFocus) onNoteCommit() },
                style = FieldStyle.Filled,
                minHeight = QoodyTheme.sizes.noteFieldHeight,
                placeholder = stringResource(R.string.receipt_note_hint),
                trailing = {
                    QoodyIcon(
                        R.drawable.ic_edit_note,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onNoteCommit() }),
            )
        }
    }
}

@Composable
private fun CategoryPanel(
    state: ReceiptUiState.Content,
    onChangeCategory: () -> Unit,
) {
    QoodyInset(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(QoodyTheme.spacing.md)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionLabel(stringResource(R.string.receipt_assigned_category), color = MaterialTheme.colorScheme.tertiary)
            LinkText(
                text = stringResource(R.string.receipt_change),
                onClick = onChangeCategory,
                color = MaterialTheme.colorScheme.primaryContainer,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = QoodyTheme.spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryPill(state.category)
            CategorizationBadge(state.categorization)
        }
        if (state.categorization.isAutomatic) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.receipt_inaccurate_question),
                    style = QoodyTheme.typography.bodySm,
                    color = MaterialTheme.colorScheme.secondary,
                )
                LinkText(
                    text = stringResource(R.string.receipt_correct_category),
                    onClick = onChangeCategory,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/** Who chose the category: the on-device model (with its confidence), a keyword rule, or the user. */
@Composable
private fun CategorizationBadge(categorization: Categorization) {
    val text =
        when (categorization) {
            Categorization.None -> {
                return
            }

            Categorization.Manual -> {
                stringResource(R.string.receipt_set_by_you)
            }

            is Categorization.Model -> {
                stringResource(
                    R.string.receipt_model_confidence,
                    PercentFormatter.formatTenths(categorization.confidence),
                )
            }

            is Categorization.Rule -> {
                stringResource(R.string.receipt_matched_rule, categorization.ruleId)
            }

            Categorization.Remembered -> {
                stringResource(R.string.receipt_remembered)
            }
        }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.xs),
    ) {
        QoodyIcon(
            id = R.drawable.ic_auto_awesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            size = QoodyTheme.sizes.iconSm,
        )
        Text(text = text, style = QoodyTheme.typography.bodySm, color = MaterialTheme.colorScheme.tertiary)
    }
}

@Composable
private fun LinkText(
    text: String,
    onClick: () -> Unit,
    color: Color,
) {
    Box(
        modifier =
            Modifier
                .heightIn(min = QoodyTheme.sizes.touchTarget)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = QoodyTheme.spacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = QoodyTheme.typography.bodySmMedium, color = color)
    }
}

@Composable
private fun NotificationCard(
    appName: String,
    text: String,
    time: String,
) {
    QoodyCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
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
                            R.drawable.ic_account_balance_wallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                Column {
                    Text(
                        text = appName,
                        style = QoodyTheme.typography.bodySmMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.receipt_original_notification),
                        style = QoodyTheme.typography.bodySm,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
            Text(text = time, style = QoodyTheme.typography.numericSm, color = MaterialTheme.colorScheme.tertiary)
        }
        QoodyInset(modifier = Modifier.fillMaxWidth().padding(vertical = QoodyTheme.spacing.sm)) {
            Text(
                text = text,
                style = QoodyTheme.typography.numericSm,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.sm)) {
            QoodyIcon(
                id = R.drawable.ic_verified_user,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primaryContainer,
                size = QoodyTheme.sizes.iconSm,
            )
            Text(
                text = stringResource(R.string.receipt_parsed_on_device),
                style = QoodyTheme.typography.bodySm,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun DetailsCard(
    state: ReceiptUiState.Content,
    onOpenBudgets: () -> Unit,
) {
    QoodyCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(QoodyTheme.spacing.md)) {
        state.paymentMethod?.let {
            DetailRow(
                stringResource(R.string.receipt_transaction_method),
                it,
                monospace = false,
            )
        }
        state.referenceCode?.let { DetailRow(stringResource(R.string.receipt_reference_code), it, monospace = true) }
        state.budgetImpact?.let { BudgetImpactRows(it, state, onOpenBudgets) }
    }
}

/** This payment's share of its category budget and the month's progress, or a link to set one. */
@Composable
private fun BudgetImpactRows(
    impact: BudgetImpact,
    state: ReceiptUiState.Content,
    onOpenBudgets: () -> Unit,
) {
    val categoryName = stringResource(impact.category.nameRes)
    val share = impact.paymentShare(state.amount)
    val limit = impact.month.limit
    val used = impact.month.used
    if (share == null || limit == null || used == null) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button, onClick = onOpenBudgets)
                    .padding(vertical = QoodyTheme.spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.receipt_budget_impact),
                style = QoodyTheme.typography.bodySm,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                text = stringResource(R.string.receipt_set_budget, categoryName),
                style = QoodyTheme.typography.bodySmMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = QoodyTheme.spacing.md),
            )
        }
        return
    }
    DetailRow(
        label = stringResource(R.string.receipt_budget_impact),
        value =
            stringResource(
                R.string.receipt_budget_impact_value,
                PercentFormatter.formatTenths(share),
                categoryName,
            ),
        monospace = false,
    )
    val colors = MaterialTheme.colorScheme
    val accent = if (impact.month.isOver) colors.error else colors.primaryContainer
    Text(
        text =
            stringResource(
                R.string.receipt_budget_month,
                MoneyFormatter.format(impact.month.spent, state.currency),
                MoneyFormatter.format(limit, state.currency),
            ),
        style = QoodyTheme.typography.bodySm,
        color = if (impact.month.isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
    )
    ProgressTrack(
        fraction = used.fraction,
        color = accent,
        modifier = Modifier.padding(top = QoodyTheme.spacing.xs, bottom = QoodyTheme.spacing.sm),
    )
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    monospace: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = QoodyTheme.spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = QoodyTheme.typography.bodySm, color = MaterialTheme.colorScheme.secondary)
        Text(
            text = value,
            style = if (monospace) QoodyTheme.typography.numericMd else QoodyTheme.typography.bodySmMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = QoodyTheme.spacing.md),
        )
    }
}

@Composable
private fun CategoryPickerSheet(
    selected: Category,
    onSelected: (Category) -> Unit,
    onDismiss: () -> Unit,
) {
    QoodyModalSheet(onDismiss = onDismiss) {
        Text(
            text = stringResource(R.string.category_picker_title),
            style = QoodyTheme.typography.headlineSm,
            color = MaterialTheme.colorScheme.onSurface,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.sm),
        ) {
            Category.entries.forEach { category ->
                CategoryPill(category = category, selected = category == selected, onClick = { onSelected(category) })
            }
        }
    }
}

/** Chosen by Qoody rather than the user, so the receipt offers to correct it. */
private val Categorization.isAutomatic: Boolean
    get() = this is Categorization.Model || this is Categorization.Rule || this == Categorization.Remembered

/** Callbacks of the "Edit entry" sheet, bundled to keep the receipt's signature readable. */
class EntryEditorActions(
    val onOpen: () -> Unit,
    val onAmountChange: (String) -> Unit,
    val onMerchantChange: (String) -> Unit,
    val onDateChange: (LocalDate) -> Unit,
    val onSave: () -> Unit,
    val onDismiss: () -> Unit,
) {
    companion object {
        /** For previews and screenshots that never open the sheet. */
        val None = EntryEditorActions({}, {}, {}, {}, {}, {})
    }
}
