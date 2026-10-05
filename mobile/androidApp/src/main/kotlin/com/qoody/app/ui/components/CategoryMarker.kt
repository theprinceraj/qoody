package com.qoody.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import com.qoody.app.ui.theme.QoodyTheme
import com.qoody.shared.domain.model.Category

/** The coloured dot that identifies a category. Uncategorized entries get an empty dashed ring. */
@Composable
fun CategoryMarker(
    category: Category,
    modifier: Modifier = Modifier,
) {
    val size = QoodyTheme.sizes.dot
    if (category == Category.Uncategorized) {
        val color = MaterialTheme.colorScheme.secondary
        val stroke = QoodyTheme.sizes.hairline
        val dash = QoodyTheme.spacing.xxs
        Canvas(modifier = modifier.size(size)) {
            drawCircle(
                color = color,
                radius = (this.size.minDimension - stroke.toPx()) / 2,
                style =
                    Stroke(
                        width = stroke.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), dash.toPx())),
                    ),
            )
        }
    } else {
        Dot(color = QoodyTheme.colors.forCategory(category).dot, size = size, modifier = modifier)
    }
}
