package com.qoody.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.qoody.app.ui.theme.QoodyTheme

/**
 * Keeps content to a comfortable reading width and centres it, so the layout still feels like a
 * notebook page on tablets and foldables instead of stretching edge to edge.
 */
@Composable
fun ScreenContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(modifier = Modifier.widthIn(max = QoodyTheme.sizes.contentMaxWidth).fillMaxSize()) {
            content()
        }
    }
}
