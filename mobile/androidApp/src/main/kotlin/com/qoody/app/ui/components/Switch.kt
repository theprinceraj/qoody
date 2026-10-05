package com.qoody.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * The app's toggle: a vermilion track when on. Pass `onCheckedChange = null` when a parent row
 * already handles the toggle (so TalkBack announces one control, not two).
 */
@Composable
fun QoodySwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors =
            SwitchDefaults.colors(
                checkedThumbColor = colors.surfaceContainerLowest,
                checkedTrackColor = colors.primaryContainer,
                checkedBorderColor = Color.Transparent,
                uncheckedThumbColor = colors.surfaceContainerLowest,
                uncheckedTrackColor = colors.surfaceContainerHighest,
                uncheckedBorderColor = Color.Transparent,
            ),
    )
}
