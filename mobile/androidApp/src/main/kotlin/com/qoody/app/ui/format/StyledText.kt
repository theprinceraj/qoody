package com.qoody.app.ui.format

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

/** A value substituted into a string resource, optionally with its own text style. */
class StyledArg(
    val text: String,
    val style: SpanStyle? = null,
)

private val placeholderPattern = Regex("%(\\d+)[$]s")

/**
 * Like `stringResource(id, args)` but lets individual arguments carry a [SpanStyle] (bold, accent colour)
 * while the surrounding sentence stays a single translatable resource.
 */
@Composable
fun styledStringResource(
    @StringRes id: Int,
    vararg args: StyledArg,
): AnnotatedString {
    val template = stringResource(id)
    return buildAnnotatedString {
        var cursor = 0
        for (match in placeholderPattern.findAll(template)) {
            append(template.substring(cursor, match.range.first))
            val arg = args[match.groupValues[1].toInt() - 1]
            if (arg.style != null) withStyle(arg.style) { append(arg.text) } else append(arg.text)
            cursor = match.range.last + 1
        }
        append(template.substring(cursor))
    }
}
