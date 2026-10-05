package com.qoody.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.Dp
import com.qoody.app.ui.theme.QoodyTheme

/** A white "stationery leaf" on the warm canvas, framed by a hairline instead of a shadow. */
@Composable
fun QoodyCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
    contentPadding: PaddingValues = PaddingValues(QoodyTheme.spacing.md),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = QoodyTheme.shapes.card,
        color = containerColor,
        border = BorderStroke(QoodyTheme.sizes.hairline, QoodyTheme.colors.hairline),
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

/** A quieter inset panel with no frame, used for secondary information inside or beside cards. */
@Composable
fun QoodyInset(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(QoodyTheme.spacing.cozy),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = QoodyTheme.shapes.tile,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

@Composable
fun HairlineDivider(
    modifier: Modifier = Modifier,
    color: Color = QoodyTheme.colors.hairline,
) {
    Box(modifier = modifier.fillMaxWidth().height(QoodyTheme.sizes.hairline).background(color))
}

/** The dashed "tear here" line along the top of a receipt. */
@Composable
fun PerforationRule(modifier: Modifier = Modifier) {
    val color = QoodyTheme.colors.perforation
    val dash = QoodyTheme.sizes.perforationDash
    val gap = QoodyTheme.sizes.perforationGap
    val thickness = QoodyTheme.sizes.hairline
    Canvas(modifier = modifier.fillMaxWidth().height(thickness).alpha(QoodyTheme.alphas.perforation)) {
        drawLine(
            color = color,
            start = Offset.Zero,
            end = Offset(size.width, 0f),
            strokeWidth = thickness.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), gap.toPx())),
        )
    }
}

/** The small grabber shown at the top of a sheet. */
@Composable
fun SheetHandle(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(QoodyTheme.sizes.sheetHandleWidth, QoodyTheme.sizes.sheetHandleHeight),
        shape = QoodyTheme.shapes.pill,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {}
}

/** A solid dot, used as a category marker or status light. */
@Composable
fun Dot(
    color: Color,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.size(size).background(color, QoodyTheme.shapes.pill))
}
