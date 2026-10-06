package com.qoody.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import com.qoody.app.ui.theme.QoodyTheme
import com.qoody.app.ui.theme.categoryEmoji
import com.qoody.app.ui.theme.categoryName
import com.qoody.shared.domain.model.Category

/** A selectable filter pill: dark ink when chosen, white paper otherwise. */
@Composable
fun FilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        selected = selected,
        onClick = onClick,
        modifier = modifier.heightIn(min = QoodyTheme.sizes.chipHeight),
        shape = QoodyTheme.shapes.pill,
        color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.secondary,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = QoodyTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = if (selected) QoodyTheme.typography.bodySmMedium else QoodyTheme.typography.bodySm,
            )
        }
    }
}

/** A small rounded status label led by a coloured dot, e.g. "Active cycle". */
@Composable
fun StatusPill(
    label: String,
    dotColor: Color,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = QoodyTheme.typography.bodySm,
) {
    Surface(
        modifier = modifier,
        shape = QoodyTheme.shapes.pill,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.secondary,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = QoodyTheme.spacing.sm, vertical = QoodyTheme.spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Dot(color = dotColor, size = QoodyTheme.sizes.dotXs)
            Text(text = label, style = textStyle)
        }
    }
}

/** An uppercase micro-label on a grey capsule, e.g. "OPTIONAL". */
@Composable
fun BadgePill(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = QoodyTheme.shapes.pill,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.secondary,
    ) {
        Text(
            text = text.uppercaseForLocale(),
            style = QoodyTheme.typography.labelCaps,
            modifier = Modifier.padding(horizontal = QoodyTheme.spacing.sm, vertical = QoodyTheme.spacing.xs),
        )
    }
}

/** A square-cornered tag such as "RENT SHARE". */
@Composable
fun TagChip(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = QoodyTheme.shapes.tag,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.secondary,
    ) {
        Text(
            text = text.uppercaseForLocale(),
            style = QoodyTheme.typography.labelCaps,
            modifier = Modifier.padding(horizontal = QoodyTheme.spacing.xs, vertical = QoodyTheme.spacing.xxs),
        )
    }
}

/** The category with its emoji, on a white pill. Optionally tinted with the category's colour. */
@Composable
fun CategoryPill(
    category: Category,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val tint = QoodyTheme.colors.forCategory(category).tint
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier.padding(horizontal = QoodyTheme.spacing.cozy, vertical = QoodyTheme.spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = categoryEmoji(category), style = QoodyTheme.typography.bodyMd)
            Text(text = categoryName(category), style = QoodyTheme.typography.bodyMdMedium)
        }
    }
    val color = if (onClick != null) tint else MaterialTheme.colorScheme.surfaceContainerLowest
    if (onClick != null) {
        Surface(
            selected = selected,
            onClick = onClick,
            modifier = modifier.heightIn(min = QoodyTheme.sizes.touchTarget),
            shape = QoodyTheme.shapes.pill,
            color = if (selected) MaterialTheme.colorScheme.onSurface else color,
            contentColor = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
            content = content,
        )
    } else {
        Surface(
            modifier = modifier,
            shape = QoodyTheme.shapes.pill,
            color = color,
            contentColor = MaterialTheme.colorScheme.onSurface,
            content = content,
        )
    }
}
