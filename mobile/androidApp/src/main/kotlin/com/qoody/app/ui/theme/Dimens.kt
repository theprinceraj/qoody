package com.qoody.app.ui.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Spacing scale. `xs`…`xl` follow the design tokens; the in-between steps are named for their role. */
@Immutable
data class Spacing(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val cozy: Dp = 12.dp,
    val md: Dp = 16.dp,
    val comfy: Dp = 20.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 40.dp,
)

/** Fixed dimensions of components and icons. */
@Immutable
data class Sizes(
    /** Minimum interactive size (accessibility and design system both require 48dp). */
    val touchTarget: Dp = 48.dp,
    val heroButtonHeight: Dp = 56.dp,
    val chipHeight: Dp = 36.dp,
    val topBarHeight: Dp = 64.dp,
    val logo: Dp = 32.dp,
    val appMark: Dp = 64.dp,
    val iconSm: Dp = 16.dp,
    val iconMd: Dp = 20.dp,
    val iconLg: Dp = 24.dp,
    val dot: Dp = 10.dp,
    val dotSm: Dp = 8.dp,
    val dotXs: Dp = 6.dp,
    val stepBadge: Dp = 28.dp,
    val settingIconBox: Dp = 36.dp,
    val featureIconBox: Dp = 40.dp,
    val circleIconButton: Dp = 40.dp,
    val reflectionIconBox: Dp = 36.dp,
    val rowMinHeight: Dp = 56.dp,
    val settingRowMinHeight: Dp = 60.dp,
    val hairline: Dp = 1.dp,
    val progressTrack: Dp = 6.dp,
    val categoryTrack: Dp = 4.dp,
    val chartHeight: Dp = 160.dp,
    val chartBarMaxWidth: Dp = 42.dp,
    val chartBaseline: Dp = 2.dp,
    /** Room above the tallest bar for its value label. */
    val chartLabelReserve: Dp = 32.dp,
    val chartBadgeHeight: Dp = 20.dp,
    val sheetHandleWidth: Dp = 40.dp,
    val sheetHandleHeight: Dp = 4.dp,
    val perforationDash: Dp = 8.dp,
    val perforationGap: Dp = 6.dp,
    val textFieldHeight: Dp = 52.dp,
    val amountFieldHeight: Dp = 64.dp,
    val noteFieldHeight: Dp = 44.dp,
    /** Reading width the content is centred within on tablets and foldables. */
    val contentMaxWidth: Dp = 600.dp,
    val onboardingMaxWidth: Dp = 440.dp,
    val summaryLoadingHeight: Dp = 160.dp,
)

@Immutable
data class QoodyShapes(
    /** Notebook cards and receipt sheets. */
    val card: CornerBasedShape = RoundedCornerShape(16.dp),
    val field: CornerBasedShape = RoundedCornerShape(12.dp),
    val tile: CornerBasedShape = RoundedCornerShape(12.dp),
    val tag: CornerBasedShape = RoundedCornerShape(4.dp),
    /** Bars of the pace chart: rounded on top only. */
    val barTop: CornerBasedShape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
    val sheet: CornerBasedShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    /** Pills: buttons, chips, status badges. */
    val pill: CornerBasedShape = RoundedCornerShape(percent = PILL_CORNER_PERCENT),
)

/** Corner radius that turns a rectangle into a pill, as a percentage of its shorter side. */
private const val PILL_CORNER_PERCENT = 50

internal fun QoodyShapes.toMaterial(): Shapes =
    Shapes(
        extraSmall = tag,
        small = field,
        medium = field,
        large = card,
        extraLarge = sheet,
    )

/** Alpha values for secondary emphasis and scrims. */
@Immutable
data class Alphas(
    val scrim: Float = 0.4f,
    val disabled: Float = 0.38f,
    val perforation: Float = 0.4f,
    val mutedFooter: Float = 0.7f,
)

/** Animation timings. */
@Immutable
data class Motion(
    val quickMillis: Int = 150,
    val standardMillis: Int = 200,
    val chartMillis: Int = 500,
)
