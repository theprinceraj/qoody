package com.qoody.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.qoody.app.ui.theme.QoodyTheme

/** Shared body of every pill button: label, optional icons and a tonal "stamp" change while pressed. */
@Composable
private fun PillButton(
    text: String,
    onClick: () -> Unit,
    container: Color,
    pressedContainer: Color,
    content: Color,
    modifier: Modifier,
    border: BorderStroke?,
    minHeight: Dp,
    textStyle: TextStyle,
    @DrawableRes leadingIcon: Int?,
    @DrawableRes trailingIcon: Int?,
    enabled: Boolean,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val background by animateColorAsState(
        targetValue = if (pressed) pressedContainer else container,
        animationSpec = tween(QoodyTheme.motion.quickMillis),
    )
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = minHeight),
        shape = QoodyTheme.shapes.pill,
        color = background,
        contentColor = content,
        border = border,
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = QoodyTheme.spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.sm, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) QoodyIcon(leadingIcon, contentDescription = null)
            Text(text = text, style = textStyle, textAlign = TextAlign.Center)
            if (trailingIcon != null) QoodyIcon(trailingIcon, contentDescription = null)
        }
    }
}

/** The single vermilion call to action on a screen. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes leadingIcon: Int? = null,
    @DrawableRes trailingIcon: Int? = null,
    enabled: Boolean = true,
    compact: Boolean = false,
) = PillButton(
    text = text,
    onClick = onClick,
    container = MaterialTheme.colorScheme.primaryContainer,
    pressedContainer = QoodyTheme.colors.accentPressed,
    content = MaterialTheme.colorScheme.onPrimary,
    modifier = modifier,
    border = null,
    minHeight = if (compact) QoodyTheme.sizes.touchTarget else QoodyTheme.sizes.heroButtonHeight,
    textStyle = if (compact) QoodyTheme.typography.buttonSm else QoodyTheme.typography.titleMd,
    leadingIcon = leadingIcon,
    trailingIcon = trailingIcon,
    enabled = enabled,
)

/** A white paper button with a hairline frame, for secondary actions. */
@Composable
fun PaperButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes leadingIcon: Int? = null,
) = PillButton(
    text = text,
    onClick = onClick,
    container = MaterialTheme.colorScheme.surfaceContainerLowest,
    pressedContainer = QoodyTheme.colors.pressed,
    content = MaterialTheme.colorScheme.onSurface,
    modifier = modifier,
    border = BorderStroke(QoodyTheme.sizes.hairline, QoodyTheme.colors.hairline),
    minHeight = QoodyTheme.sizes.touchTarget,
    textStyle = QoodyTheme.typography.headlineSm,
    leadingIcon = leadingIcon,
    trailingIcon = null,
    enabled = true,
)

/** A grey tonal button for low-key actions that sit beside a primary one. */
@Composable
fun TonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes leadingIcon: Int? = null,
) = PillButton(
    text = text,
    onClick = onClick,
    container = MaterialTheme.colorScheme.surfaceContainerHigh,
    pressedContainer = MaterialTheme.colorScheme.surfaceContainerHighest,
    content = MaterialTheme.colorScheme.onSurface,
    modifier = modifier,
    border = null,
    minHeight = QoodyTheme.sizes.touchTarget,
    textStyle = QoodyTheme.typography.buttonSm,
    leadingIcon = leadingIcon,
    trailingIcon = null,
    enabled = true,
)

/** A borderless text-only button. */
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes leadingIcon: Int? = null,
    contentColor: Color = MaterialTheme.colorScheme.tertiary,
) = PillButton(
    text = text,
    onClick = onClick,
    container = Color.Transparent,
    pressedContainer = QoodyTheme.colors.pressed,
    content = contentColor,
    modifier = modifier,
    border = null,
    minHeight = QoodyTheme.sizes.touchTarget,
    textStyle = QoodyTheme.typography.bodySmMedium,
    leadingIcon = leadingIcon,
    trailingIcon = null,
    enabled = true,
)
