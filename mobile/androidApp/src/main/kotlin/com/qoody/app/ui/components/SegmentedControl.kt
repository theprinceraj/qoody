package com.qoody.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import com.qoody.app.ui.theme.QoodyTheme

/**
 * A pill-shaped segmented control. The chosen segment is a raised white pill.
 *
 * @param equalWidth when true every segment shares the width equally (full-width tab bars);
 * otherwise segments hug their label (compact toggles).
 */
@Composable
fun <T> SegmentedControl(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    equalWidth: Boolean = true,
    textStyle: TextStyle = QoodyTheme.typography.bodySm,
) {
    Surface(
        modifier = modifier,
        shape = QoodyTheme.shapes.pill,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(QoodyTheme.spacing.xs).height(IntrinsicSize.Min).selectableGroup(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            options.forEach { option ->
                Segment(
                    text = label(option),
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    textStyle = textStyle,
                    equalWidth = equalWidth,
                )
            }
        }
    }
}

@Composable
private fun RowScope.Segment(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    textStyle: TextStyle,
    equalWidth: Boolean,
) {
    Surface(
        selected = selected,
        onClick = onClick,
        modifier = (if (equalWidth) Modifier.weight(1f) else Modifier).heightIn(min = QoodyTheme.sizes.chipHeight),
        shape = QoodyTheme.shapes.pill,
        color =
            if (selected) {
                MaterialTheme.colorScheme.surfaceContainerLowest
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
        contentColor = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.secondary,
        border = if (selected) BorderStroke(QoodyTheme.sizes.hairline, QoodyTheme.colors.hairline) else null,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = QoodyTheme.spacing.cozy, vertical = QoodyTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        ) {
            Text(text = text, style = textStyle, textAlign = TextAlign.Center)
        }
    }
}
