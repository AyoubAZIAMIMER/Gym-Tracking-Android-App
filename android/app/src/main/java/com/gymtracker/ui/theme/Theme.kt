// Purpose: Material3 theme wiring — dark-first color schemes + extended colors (success/gold/tags)
// Inputs: system dark-mode flag
// Outputs: GymTrackerTheme composable + GymTheme accessor for extended colors
package com.gymtracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Colors Material3 has no slot for; kept out of ColorScheme so usage stays explicit
@Immutable
data class ExtendedColors(
    val success: Color,
    val successDim: Color,
    val prGold: Color,
    val hint: Color,
    val tagWarmup: Color,
    val tagDropset: Color,
    val tagNegative: Color,
    val tagTempo: Color,
    val tagFailure: Color,
    // liquid glass tokens
    val glassTint: Color,
    val glassTintBlur: Color,
    val glassHighlight: Color,
    val glassOutline: Color,
    // Identity v5 heat spectrum — per-theme so daylight keeps contrast
    val heat: HeatScale,
)

private val DarkExtended = ExtendedColors(
    success = Success,
    successDim = SuccessDim,
    prGold = PrGold,
    hint = TextHint,
    tagWarmup = TagWarmup,
    tagDropset = TagDropset,
    tagNegative = TagNegative,
    tagTempo = TagTempo,
    tagFailure = TagFailure,
    glassTint = GlassTintDark,
    glassTintBlur = GlassTintBlurDark,
    glassHighlight = GlassHighlightDark,
    glassOutline = GlassOutlineDark,
    heat = DarkHeat,
)

private val LightExtended = ExtendedColors(
    success = SuccessLight,
    successDim = SuccessDimLight,
    prGold = PrGoldLight,
    hint = TextHintLight,
    tagWarmup = PrGoldLight,
    tagDropset = TagDropset,
    tagNegative = TagNegative,
    tagTempo = CyanLight,
    tagFailure = ErrorLight,
    glassTint = GlassTintLight,
    glassTintBlur = GlassTintBlurLight,
    glassHighlight = GlassHighlightLight,
    glassOutline = GlassOutlineLight,
    heat = LightHeat,
)

val LocalExtendedColors = staticCompositionLocalOf { DarkExtended }

private val DarkColors = darkColorScheme(
    primary = AccentPrimary,
    onPrimary = OnAccentPrimary,
    primaryContainer = AccentPrimaryDim,
    onPrimaryContainer = AccentPrimaryBright,
    secondary = Cyan,
    onSecondary = Ink,
    secondaryContainer = CyanDim,
    onSecondaryContainer = Cyan,
    tertiary = PrGold,
    onTertiary = Ink,
    background = Ink,
    onBackground = TextPrimary,
    surface = Ink,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceRaised,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = Color(0xFF14120F),
    surfaceContainerLow = SurfaceDark,
    surfaceContainer = SurfaceDark,
    surfaceContainerHigh = SurfaceRaised,
    surfaceContainerHighest = SurfaceRaised,
    outline = OutlineDark,
    outlineVariant = OutlineFaint,
    error = ErrorRed,
    onError = Color.White,
)

private val LightColors = lightColorScheme(
    primary = AccentPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = AccentContainerLight,
    onPrimaryContainer = OnAccentContainerLight,
    secondary = CyanLight,
    onSecondary = Color.White,
    tertiary = PrGoldLight,
    onTertiary = Color.White,
    background = PaperLight,
    onBackground = TextPrimaryLight,
    surface = PaperLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceRaisedLight,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerLowest = SurfaceLight,
    surfaceContainerLow = SurfaceLight,
    surfaceContainer = SurfaceLight,
    surfaceContainerHigh = SurfaceRaisedLight,
    surfaceContainerHighest = SurfaceRaisedLight,
    outline = OutlineLight,
    outlineVariant = OutlineFaintLight,
    error = ErrorLight,
    onError = Color.White,
)

@Composable
fun GymTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val extended = if (darkTheme) DarkExtended else LightExtended
    CompositionLocalProvider(LocalExtendedColors provides extended) {
        MaterialTheme(
            colorScheme = colors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

// Access point for extended colors: GymTheme.colors.success etc.
object GymTheme {
    val colors: ExtendedColors
        @Composable get() = LocalExtendedColors.current
}
