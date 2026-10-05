package com.qoody.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import com.qoody.app.ui.theme.QoodyTheme

enum class FieldStyle {
    /** White field with a hairline frame that turns to ink when focused (forms). */
    Outlined,

    /** Quiet grey field that brightens when focused (inline editing, e.g. a note). */
    Filled,
}

/** The app's single-line text field. */
@Composable
fun QoodyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    style: FieldStyle = FieldStyle.Outlined,
    textStyle: TextStyle = QoodyTheme.typography.bodyMd,
    placeholder: String? = null,
    minHeight: Dp = QoodyTheme.sizes.textFieldHeight,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    readOnly: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val colors = MaterialTheme.colorScheme

    val container =
        when (style) {
            FieldStyle.Outlined -> colors.surfaceContainerLowest
            FieldStyle.Filled -> if (focused) colors.surfaceContainerLowest else colors.surfaceContainerLow
        }
    val borderColor =
        when {
            focused -> colors.onSurface
            style == FieldStyle.Outlined -> QoodyTheme.colors.hairline
            else -> container
        }

    Surface(
        modifier = modifier.fillMaxWidth().heightIn(min = minHeight),
        shape = QoodyTheme.shapes.field,
        color = container,
        border = BorderStroke(QoodyTheme.sizes.hairline, borderColor),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = QoodyTheme.spacing.cozy),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.sm),
        ) {
            leading?.invoke()
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty() && placeholder != null) {
                    Text(text = placeholder, style = textStyle, color = colors.tertiary)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = textStyle.copy(color = colors.onSurface),
                    cursorBrush = SolidColor(colors.primaryContainer),
                    singleLine = true,
                    readOnly = readOnly,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    visualTransformation = visualTransformation,
                    interactionSource = interactionSource,
                )
            }
            trailing?.invoke()
        }
    }
}
