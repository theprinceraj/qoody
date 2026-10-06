package com.qoody.app.ui.categories

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qoody.app.R
import com.qoody.app.ui.components.GhostButton
import com.qoody.app.ui.components.HairlineDivider
import com.qoody.app.ui.components.LoadingIndicator
import com.qoody.app.ui.components.PrimaryButton
import com.qoody.app.ui.components.QoodyCard
import com.qoody.app.ui.components.QoodyDetailTopBar
import com.qoody.app.ui.components.QoodyIcon
import com.qoody.app.ui.components.QoodyModalSheet
import com.qoody.app.ui.components.QoodyTextField
import com.qoody.app.ui.components.ScreenContainer
import com.qoody.app.ui.components.SectionLabel
import com.qoody.app.ui.theme.QoodyTheme
import com.qoody.shared.domain.model.CustomCategory
import com.qoody.shared.feature.categories.CategoriesUiState
import com.qoody.shared.feature.categories.CategoriesViewModel
import com.qoody.shared.feature.categories.CategoryEditor
import com.qoody.shared.feature.categories.CategoryEmojis
import org.koin.androidx.compose.koinViewModel

/** Settings → Categories: the user's own categories (D25). */
@Composable
fun CategoriesScreen(
    onBack: () -> Unit,
    viewModel: CategoriesViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CategoriesContent(
        state = state,
        onBack = onBack,
        onAdd = viewModel::onAddRequested,
        onEdit = viewModel::onEditRequested,
    )
    val content = state as? CategoriesUiState.Content ?: return
    content.editor?.let { editor ->
        CategorySheet(
            editor = editor,
            onNameChange = viewModel::onNameChanged,
            onEmojiSelected = viewModel::onEmojiSelected,
            onSave = viewModel::onSave,
            onDelete = {
                content.custom.firstOrNull { it.id == editor.id }?.let(viewModel::onDeleteRequested)
                viewModel.onEditorDismissed()
            },
            onDismiss = viewModel::onEditorDismissed,
        )
    }
    content.pendingDelete?.let { category ->
        AlertDialog(
            onDismissRequest = viewModel::onDeleteDismissed,
            title = { Text(stringResource(R.string.categories_delete_title, category.name)) },
            text = { Text(stringResource(R.string.categories_delete_body)) },
            confirmButton = {
                Button(onClick = viewModel::onDeleteConfirmed) {
                    Text(stringResource(R.string.categories_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDeleteDismissed) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
fun CategoriesContent(
    state: CategoriesUiState,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (CustomCategory) -> Unit,
) {
    ScreenContainer {
        Column(modifier = Modifier.fillMaxSize()) {
            QoodyDetailTopBar(title = stringResource(R.string.categories_title), onBackClick = onBack)
            when (state) {
                CategoriesUiState.Loading -> {
                    LoadingIndicator()
                }

                is CategoriesUiState.Content -> {
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
                                text = stringResource(R.string.categories_intro),
                                style = QoodyTheme.typography.bodyMd,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                        item { CategoryList(state.custom, onEdit) }
                        item {
                            PrimaryButton(
                                text = stringResource(R.string.categories_add),
                                onClick = onAdd,
                                leadingIcon = R.drawable.ic_add,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryList(
    custom: List<CustomCategory>,
    onEdit: (CustomCategory) -> Unit,
) {
    if (custom.isEmpty()) {
        Text(
            text = stringResource(R.string.categories_empty),
            style = QoodyTheme.typography.bodyMd,
            color = MaterialTheme.colorScheme.tertiary,
        )
        return
    }
    QoodyCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues()) {
        custom.forEachIndexed { index, category ->
            if (index > 0) HairlineDivider()
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { onEdit(category) }
                        .heightIn(min = QoodyTheme.sizes.settingRowMinHeight)
                        .padding(QoodyTheme.spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.cozy),
            ) {
                Text(text = category.emoji, style = QoodyTheme.typography.titleMd)
                Text(
                    text = category.name,
                    style = QoodyTheme.typography.titleMd,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                QoodyIcon(
                    R.drawable.ic_edit,
                    contentDescription = stringResource(R.string.categories_edit, category.name),
                    tint = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Composable
private fun CategorySheet(
    editor: CategoryEditor,
    onNameChange: (String) -> Unit,
    onEmojiSelected: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    QoodyModalSheet(onDismiss = onDismiss) {
        Text(
            text =
                stringResource(
                    if (editor.id ==
                        null
                    ) {
                        R.string.categories_sheet_new
                    } else {
                        R.string.categories_sheet_edit
                    },
                ),
            style = QoodyTheme.typography.headlineSm,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Column(verticalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.xs)) {
            SectionLabel(stringResource(R.string.categories_name_label))
            QoodyTextField(
                value = editor.name,
                onValueChange = onNameChange,
                placeholder = stringResource(R.string.categories_name_hint),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )
            if (editor.isDuplicate) {
                Text(
                    text = stringResource(R.string.categories_duplicate),
                    style = QoodyTheme.typography.bodySm,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.sm)) {
            SectionLabel(stringResource(R.string.categories_emoji_label))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.xs),
                verticalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.xs),
            ) {
                CategoryEmojis.forEach { emoji ->
                    EmojiOption(emoji, selected = emoji == editor.emoji, onClick = { onEmojiSelected(emoji) })
                }
            }
        }
        PrimaryButton(
            text = stringResource(R.string.action_save),
            onClick = onSave,
            enabled = editor.canSave,
            modifier = Modifier.fillMaxWidth(),
        )
        if (editor.id != null) {
            GhostButton(
                text = stringResource(R.string.categories_delete),
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
                contentColor = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun EmojiOption(
    emoji: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .size(QoodyTheme.sizes.touchTarget)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        shape = QoodyTheme.shapes.field,
        color = if (selected) QoodyTheme.colors.accentSoft else MaterialTheme.colorScheme.surfaceContainerLow,
        border = if (selected) BorderStroke(QoodyTheme.sizes.hairline, MaterialTheme.colorScheme.primary) else null,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = emoji, style = QoodyTheme.typography.titleMd)
        }
    }
}
