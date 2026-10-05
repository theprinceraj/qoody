package com.qoody.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalQoodyColors = staticCompositionLocalOf { QoodyColors() }
private val LocalQoodyTypography = staticCompositionLocalOf { QoodyTypography() }
private val LocalSpacing = staticCompositionLocalOf { Spacing() }
private val LocalSizes = staticCompositionLocalOf { Sizes() }
private val LocalShapes = staticCompositionLocalOf { QoodyShapes() }
private val LocalAlphas = staticCompositionLocalOf { Alphas() }
private val LocalMotion = staticCompositionLocalOf { Motion() }

/** Entry point to every design token. Prefer these over raw `Color`, `dp` or `sp` values. */
object QoodyTheme {
    val colors: QoodyColors
        @Composable @ReadOnlyComposable
        get() = LocalQoodyColors.current

    val typography: QoodyTypography
        @Composable @ReadOnlyComposable
        get() = LocalQoodyTypography.current

    val spacing: Spacing
        @Composable @ReadOnlyComposable
        get() = LocalSpacing.current

    val sizes: Sizes
        @Composable @ReadOnlyComposable
        get() = LocalSizes.current

    val shapes: QoodyShapes
        @Composable @ReadOnlyComposable
        get() = LocalShapes.current

    val alphas: Alphas
        @Composable @ReadOnlyComposable
        get() = LocalAlphas.current

    val motion: Motion
        @Composable @ReadOnlyComposable
        get() = LocalMotion.current
}

/** Applies the Warm Paper theme. The palette is fixed on purpose: no dynamic colour, no dark variant yet. */
@Composable
fun QoodyTheme(content: @Composable () -> Unit) {
    val typography = QoodyTypography()
    val shapes = QoodyShapes()
    CompositionLocalProvider(
        LocalQoodyTypography provides typography,
        LocalShapes provides shapes,
    ) {
        MaterialTheme(
            colorScheme = QoodyColorScheme,
            typography = typography.toMaterial(),
            shapes = shapes.toMaterial(),
            content = content,
        )
    }
}
