// Purpose: All RepForge colors — "Molten Forge" identity (2026-07-13): dark iron and
//          smoked steel with ONE hot color (ember orange); molten gold strictly for PRs,
//          quenched steel blue-grey for cool/secondary data. Owner-approved direction.
// Inputs: none (constants)
// Outputs: Color values consumed by Theme.kt (never reference these directly from screens)
package com.gymtracker.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

// --- Dark palette (the designed, default theme: a night forge) ----------------
val Ink = Color(0xFF0E0D0B)            // charcoal iron — screen background
val SurfaceDark = Color(0xFF1A1815)    // smoked steel — cards
val SurfaceRaised = Color(0xFF242019)  // nested cards, input fields (warm coal)
val OutlineDark = Color(0xFF35302A)
val OutlineFaint = Color(0xFF272319)

// Primary: ember orange — the single hot color; everything else stays metal
val AccentPrimary = Color(0xFFFF5A1F)
val AccentPrimaryDim = Color(0xFF3A1608)     // tinted containers / icon tiles
val AccentPrimaryBright = Color(0xFFFFB294)  // on-container text
val OnAccentPrimary = Color(0xFF1C0800)

// Secondary: quenched steel — cool blue-grey for timers/durations/secondary data
val Cyan = Color(0xFF9FB6C2)
val CyanDim = Color(0xFF232B30)

// Forge red — errors, failure sets (hotter, redder than the ember primary)
val ActivityPink = Color(0xFFFF4B36)

val TextPrimary = Color(0xFFF2EDE4)    // warm off-white, like light on hot metal
val TextSecondary = Color(0xFFA89F91)
val TextHint = Color(0xFF6B6357)

val Success = Color(0xFFB8C77A)        // tempered olive — completed sets
val SuccessDim = Color(0xFF262B12)
val PrGold = Color(0xFFFFC93C)         // molten gold — PRs only, never decoration
val ErrorRed = ActivityPink

// Set-tag letter chips (W/D/N/T/F)
val TagWarmup = Color(0xFFFFC93C)
val TagDropset = Color(0xFFC9A2E8)
val TagNegative = Color(0xFFFF8A6B)
val TagTempo = Color(0xFF9FB6C2)
val TagFailure = Color(0xFFFF4B36)

// --- Heat scale (Identity v5, design/IDENTITY_V5.md: "heat is data") ----------
// Steel reads its temperature: quenched blue = recovered/calm, glowing red = just
// worked. Screens never hand-pick a heat hue — they ask GymTheme.colors.heat.at().
@Immutable
class HeatScale(
    val steel: Color,   // quenched — fully recovered, ready to strike
    val bronze: Color,  // warming — mid recovery / moderate effort
    val ember: Color,   // working — the one hot color
    val red: Color,     // glowing — just trained / maximal effort
) {
    /** Maps freshness (1f = fully recovered … 0f = just worked) onto the spectrum. */
    fun at(freshness: Float): Color {
        val heat = 1f - freshness.coerceIn(0f, 1f)
        return when {
            heat <= 0.45f -> lerp(steel, bronze, heat / 0.45f)
            heat <= 0.75f -> lerp(bronze, ember, (heat - 0.45f) / 0.30f)
            else -> lerp(ember, red, (heat - 0.75f) / 0.25f)
        }
    }
}

// Night forge: hot hues read against dark iron
val DarkHeat = HeatScale(
    steel = Color(0xFF8FB4C7),
    bronze = Color(0xFFD08A45),
    ember = AccentPrimary,
    red = Color(0xFFFF3320),
)

// Daylight workshop: every stop deepened so it holds ≥3:1 contrast on bone paper
val LightHeat = HeatScale(
    steel = Color(0xFF4E7086),
    bronze = Color(0xFF9A5A1D),
    ember = Color(0xFFC63D08),  // = AccentPrimaryLight (declared below; literal avoids
                                //   a forward reference in top-level init order)
    red = Color(0xFFB3230F),
)

val HeatWhite = Color(0xFFFFE3C2)      // white-hot flash — momentary highlights only

// --- Liquid glass tokens ------------------------------------------------------
// Fallback wash for glass panels that sit inside the blur source (cards)
val GlassTintDark = Color(0xB31A1815)        // ~70% SurfaceDark — ember glow bleeds through
val GlassTintBlurDark = Color(0x661A1815)    // wash over real backdrop blur
val GlassHighlightDark = Color(0x2EFFD9B8)   // warm hairline top edge (firelight, not white)
val GlassOutlineDark = Color(0x0DFFB68A)     // warm hairline bottom edge

val GlassTintLight = Color(0xCCFFFCF6)
val GlassTintBlurLight = Color(0x99FFFCF6)
val GlassHighlightLight = Color(0xF2FFFFFF)
val GlassOutlineLight = Color(0x4DE8DCC8)

// --- Light palette (daylight workshop: warm bone paper, iron text, deep ember) -
val PaperLight = Color(0xFFF4EFE6)
val SurfaceLight = Color(0xFFFDFAF3)
val SurfaceRaisedLight = Color(0xFFEDE5D6)
val OutlineLight = Color(0xFFDCD2BF)
val OutlineFaintLight = Color(0xFFE8E0CF)
val TextPrimaryLight = Color(0xFF1B150E)
val TextSecondaryLight = Color(0xFF6C6152)
val TextHintLight = Color(0xFFA19684)
val AccentPrimaryLight = Color(0xFFC63D08)       // ember, deepened for light surfaces
val AccentContainerLight = Color(0xFFFFDCC9)
val OnAccentContainerLight = Color(0xFF571B00)
val CyanLight = Color(0xFF54707E)                // quenched steel, darkened
val SuccessLight = Color(0xFF5F7A2A)
val SuccessDimLight = Color(0xFFE9EFD3)
val PrGoldLight = Color(0xFFB07E0C)
val ErrorLight = Color(0xFFC02D14)
