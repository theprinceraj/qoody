package com.qoody.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.qoody.app.ui.theme.QoodyTheme

/** A thin pill-shaped bar, for example spending against a budget. [fraction] is clamped to 0..1. */
@Composable
fun ProgressTrack(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primaryContainer,
) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(QoodyTheme.motion.chartMillis))
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
                    .background(color),
        )
    }
}
