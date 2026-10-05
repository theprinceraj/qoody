package com.qoody.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.qoody.app.R

/** One variable-font resource exposed as a family with a face for each weight we use. */
private fun variableFamily(
    fontResource: Int,
    vararg weights: FontWeight,
): FontFamily =
    FontFamily(
        weights.map { weight ->
            Font(
                resId = fontResource,
                weight = weight,
                variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
            )
        },
    )

private val DisplayFamily =
    variableFamily(R.font.dm_sans, FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)
private val BodyFamily =
    variableFamily(R.font.plus_jakarta_sans, FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)
private val NumericFamily =
    variableFamily(R.font.jetbrains_mono, FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold)

private fun style(
    family: FontFamily,
    size: TextUnit,
    weight: FontWeight,
    lineHeight: TextUnit,
    letterSpacing: TextUnit = TextUnit.Unspecified,
) = TextStyle(
    fontFamily = family,
    fontSize = size,
    fontWeight = weight,
    lineHeight = lineHeight,
    letterSpacing = letterSpacing,
)

/**
 * The design system's type scale. Headlines use DM Sans, running text Plus Jakarta Sans and every
 * figure JetBrains Mono so decimals align down a column.
 */
@Immutable
data class QoodyTypography(
    val displayHero: TextStyle = style(DisplayFamily, 44.sp, FontWeight.Bold, 52.sp, (-0.02).em),
    val headlineLgMobile: TextStyle = style(DisplayFamily, 26.sp, FontWeight.SemiBold, 32.sp, (-0.01).em),
    val headlineMd: TextStyle = style(DisplayFamily, 22.sp, FontWeight.SemiBold, 28.sp, (-0.01).em),
    val headlineSm: TextStyle = style(DisplayFamily, 18.sp, FontWeight.SemiBold, 24.sp),
    val titleMd: TextStyle = style(DisplayFamily, 16.sp, FontWeight.SemiBold, 24.sp),
    val buttonSm: TextStyle = style(DisplayFamily, 14.sp, FontWeight.SemiBold, 20.sp),
    val wordmark: TextStyle = style(DisplayFamily, 18.sp, FontWeight.Bold, 24.sp, (-0.01).em),
    val bodyLg: TextStyle = style(BodyFamily, 16.sp, FontWeight.Normal, 24.sp),
    val bodyMd: TextStyle = style(BodyFamily, 14.sp, FontWeight.Normal, 20.sp),
    val bodyMdMedium: TextStyle = style(BodyFamily, 14.sp, FontWeight.Medium, 20.sp),
    val bodySm: TextStyle = style(BodyFamily, 12.sp, FontWeight.Normal, 18.sp),
    val bodySmMedium: TextStyle = style(BodyFamily, 12.sp, FontWeight.Medium, 18.sp),
    val labelCaps: TextStyle = style(BodyFamily, 11.sp, FontWeight.Bold, 16.sp, 0.06.em),
    val numericHero: TextStyle = style(NumericFamily, 36.sp, FontWeight.Medium, 44.sp, (-0.03).em),
    val numericLg: TextStyle = style(NumericFamily, 20.sp, FontWeight.Medium, 28.sp, (-0.02).em),
    val numericMd: TextStyle = style(NumericFamily, 15.sp, FontWeight.Medium, 22.sp, (-0.01).em),
    val numericSm: TextStyle = style(NumericFamily, 12.sp, FontWeight.Medium, 18.sp),
)

/** Maps the scale onto Material's slots so stock components (text fields, sheets) look right too. */
internal fun QoodyTypography.toMaterial(): Typography =
    Typography(
        displayLarge = displayHero,
        headlineLarge = headlineLgMobile,
        headlineMedium = headlineMd,
        headlineSmall = headlineSm,
        titleLarge = headlineMd,
        titleMedium = titleMd,
        titleSmall = headlineSm,
        bodyLarge = bodyLg,
        bodyMedium = bodyMd,
        bodySmall = bodySm,
        labelLarge = bodyMdMedium,
        labelMedium = bodySmMedium,
        labelSmall = labelCaps,
    )
