package com.qoody.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.qoody.shared.domain.model.Category

/**
 * Brand palette from the "Warm Tactile Ledger" design system (docs: DESIGN.md).
 * Raw hex values live here and nowhere else; everything else reads roles via [QoodyTheme].
 */
private object Palette {
    val Surface = Color(0xFFFCF9F4)
    val SurfaceDim = Color(0xFFDCDAD5)
    val SurfaceContainerLowest = Color(0xFFFFFFFF)
    val SurfaceContainerLow = Color(0xFFF6F3EE)
    val SurfaceContainer = Color(0xFFF0EDE9)
    val SurfaceContainerHigh = Color(0xFFEBE8E3)
    val SurfaceContainerHighest = Color(0xFFE5E2DD)
    val OnSurface = Color(0xFF1C1C19)
    val OnSurfaceVariant = Color(0xFF5B403A)
    val InverseSurface = Color(0xFF31302D)
    val InverseOnSurface = Color(0xFFF3F0EB)
    val Outline = Color(0xFF8F7069)
    val OutlineVariant = Color(0xFFE3BEB6)
    val Primary = Color(0xFFB52603)
    val OnPrimary = Color(0xFFFFFFFF)
    val PrimaryContainer = Color(0xFFFF5A36)
    val OnPrimaryContainer = Color(0xFF5A0C00)
    val InversePrimary = Color(0xFFFFB4A3)
    val Secondary = Color(0xFF605E5A)
    val OnSecondary = Color(0xFFFFFFFF)
    val SecondaryContainer = Color(0xFFE6E2DC)
    val OnSecondaryContainer = Color(0xFF666460)
    val Tertiary = Color(0xFF615E58)
    val OnTertiary = Color(0xFFFFFFFF)
    val TertiaryContainer = Color(0xFF96928C)
    val OnTertiaryContainer = Color(0xFF2D2B26)
    val Error = Color(0xFFBA1A1A)
    val OnError = Color(0xFFFFFFFF)
    val ErrorContainer = Color(0xFFFFDAD6)
    val OnErrorContainer = Color(0xFF93000A)
    val PrimaryFixed = Color(0xFFFFDAD2)
    val OnPrimaryFixed = Color(0xFF3D0600)

    // Ledger-specific tones that Material 3 has no role for.
    val Hairline = Color(0xFFE8E4DC)
    val Pressed = Color(0xFFF3EFE6)
    val AccentPressed = Color(0xFFE04624)
    val ReceiptPerforation = Color(0xFFD5D0C5)
    val Positive = Color(0xFF059669)

    val SageDot = Color(0xFF688B77)
    val LavenderDot = Color(0xFF847596)
    val CoralDot = Color(0xFFD97E6A)
    val SlateDot = Color(0xFF8C887F)
    val SkyDot = Color(0xFF7EA8BE)
    val AmberDot = Color(0xFFD4A373)

    val SageTint = Color(0xFFE5EDE5)
    val PeachTint = Color(0xFFFDECE3)
    val LavenderTint = Color(0xFFEFEBF8)
    val SkyTint = Color(0xFFE4EEF5)
    val ButterTint = Color(0xFFFAF2D9)
    val MistTint = Color(0xFFEAECEF)
}

internal val QoodyColorScheme: ColorScheme =
    lightColorScheme(
        primary = Palette.Primary,
        onPrimary = Palette.OnPrimary,
        primaryContainer = Palette.PrimaryContainer,
        onPrimaryContainer = Palette.OnPrimaryContainer,
        inversePrimary = Palette.InversePrimary,
        secondary = Palette.Secondary,
        onSecondary = Palette.OnSecondary,
        secondaryContainer = Palette.SecondaryContainer,
        onSecondaryContainer = Palette.OnSecondaryContainer,
        tertiary = Palette.Tertiary,
        onTertiary = Palette.OnTertiary,
        tertiaryContainer = Palette.TertiaryContainer,
        onTertiaryContainer = Palette.OnTertiaryContainer,
        background = Palette.Surface,
        onBackground = Palette.OnSurface,
        surface = Palette.Surface,
        onSurface = Palette.OnSurface,
        surfaceVariant = Palette.SurfaceContainerHighest,
        onSurfaceVariant = Palette.OnSurfaceVariant,
        surfaceTint = Palette.Primary,
        inverseSurface = Palette.InverseSurface,
        inverseOnSurface = Palette.InverseOnSurface,
        error = Palette.Error,
        onError = Palette.OnError,
        errorContainer = Palette.ErrorContainer,
        onErrorContainer = Palette.OnErrorContainer,
        outline = Palette.Outline,
        outlineVariant = Palette.OutlineVariant,
        surfaceDim = Palette.SurfaceDim,
        surfaceBright = Palette.Surface,
        surfaceContainerLowest = Palette.SurfaceContainerLowest,
        surfaceContainerLow = Palette.SurfaceContainerLow,
        surfaceContainer = Palette.SurfaceContainer,
        surfaceContainerHigh = Palette.SurfaceContainerHigh,
        surfaceContainerHighest = Palette.SurfaceContainerHighest,
        primaryFixed = Palette.PrimaryFixed,
        onPrimaryFixed = Palette.OnPrimaryFixed,
    )

/** A category's solid marker colour and its soft background tint. */
@Immutable
data class CategoryColors(
    val dot: Color,
    val tint: Color,
)

/** Colours beyond the Material roles. Read via `QoodyTheme.colors`. */
@Immutable
data class QoodyColors(
    /** 1dp rules between ledger rows and around cards. */
    val hairline: Color = Palette.Hairline,
    /** Tonal fill of a pressed row or ghost button. */
    val pressed: Color = Palette.Pressed,
    /** The vermilion accent when held down. */
    val accentPressed: Color = Palette.AccentPressed,
    val perforation: Color = Palette.ReceiptPerforation,
    /** "Everything is working" status dot. */
    val positive: Color = Palette.Positive,
    val accentSoft: Color = Palette.PrimaryFixed,
    val onAccentSoft: Color = Palette.OnPrimaryFixed,
) {
    /** A user-made category takes one of the built-in pairs, in turn by id, so it fits the theme. */
    fun forCategory(category: Category): CategoryColors =
        when (category) {
            Category.FoodAndDrink -> Sage
            Category.Transport -> Slate
            Category.Shopping -> Coral
            Category.Bills -> Lavender
            Category.Friends -> Amber
            Category.Subscriptions -> Sky
            Category.Uncategorized -> CategoryColors(Palette.TertiaryContainer, Palette.MistTint)
            else -> customPalette[((category.customId ?: 0L) % customPalette.size).toInt()]
        }

    private companion object {
        val Sage = CategoryColors(Palette.SageDot, Palette.SageTint)
        val Slate = CategoryColors(Palette.SlateDot, Palette.MistTint)
        val Coral = CategoryColors(Palette.CoralDot, Palette.PeachTint)
        val Lavender = CategoryColors(Palette.LavenderDot, Palette.LavenderTint)
        val Amber = CategoryColors(Palette.AmberDot, Palette.ButterTint)
        val Sky = CategoryColors(Palette.SkyDot, Palette.SkyTint)
        val customPalette = listOf(Amber, Sky, Sage, Lavender, Coral, Slate)
    }
}
