// Purpose: All Forged colors — Identity v8 "Blue Hour" (2026-07-21): cold machine,
//          hot body. Blue-biased near-black floor, graphite surfaces, chalk type.
//          ONE interface signal = ELECTRIC INDIGO (nav, action, active, work done).
//          Heat (ember ramp) is reserved for the BODY only — muscle readiness/fatigue.
//          Gold is strictly the earned (PRs, top rank). Cyan is calm time/data, sparingly.
//          Identifier names kept from earlier identities so theme wiring is stable.
// Inputs: none (constants)
// Outputs: Color values consumed by Theme.kt (never reference these directly from screens)
package com.gymtracker.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

// --- Dark palette (the designed, default theme: the gym at blue hour) ----------
val Ink = Color(0xFF06070B)            // blue-biased near-black — screen background
val SurfaceDark = Color(0xFF12141B)    // graphite — cards
val SurfaceRaised = Color(0xFF191C25)  // nested cards, input fields
val OutlineDark = Color(0xFF242835)
val OutlineFaint = Color(0xFF1B1E28)

// Primary: ELECTRIC INDIGO — the single interface signal (the machine)
val AccentPrimary = Color(0xFF5B5BF7)
val AccentPrimaryDim = Color(0xFF1B1D3A)     // tinted containers / icon tiles
val AccentPrimaryBright = Color(0xFF8A8CFF)  // on-container text / brightest intensity
val OnAccentPrimary = Color(0xFFFFFFFF)      // chalk on the indigo action surface

// Secondary: ICE — calm time/data (rest, durations, 4-week average). Used sparingly.
val Cyan = Color(0xFF64D2FF)
val CyanDim = Color(0xFF12262F)

// Failure sets / errors (iOS red)
val ActivityPink = Color(0xFFFF453A)

val TextPrimary = Color(0xFFF3F5FB)    // chalk white (a half-step cool)
val TextSecondary = Color(0xFF98A0B0)
val TextHint = Color(0xFF5B6272)

// Completed work IS the interface signal — logging a set closes the ring in indigo
val Success = Color(0xFF5B5BF7)
val SuccessDim = Color(0xFF15173A)
val PrGold = Color(0xFFFFCB45)         // the earned — PRs & top rank only, never decoration
val ErrorRed = ActivityPink

// Set-tag letter chips (W/D/N/T/F) — iOS system colors (semantic, kept distinct)
val TagWarmup = Color(0xFFFFCB45)
val TagDropset = Color(0xFFBF5AF2)
val TagNegative = Color(0xFFFF9F0A)
val TagTempo = Color(0xFF64D2FF)
val TagFailure = Color(0xFFFF453A)

// Rank medals (subtle gamification, v8). Gold rank reuses PrGold; Olympian uses chalk.
val RankWood = Color(0xFF8A7A66)
val RankBronze = Color(0xFFB57C4A)
val RankSilver = Color(0xFFB9C0CC)
val RankWoodLight = Color(0xFF6E5E4A)
val RankBronzeLight = Color(0xFF9A6636)
val RankSilverLight = Color(0xFF71798A)

// --- Readiness scale — the ember ramp, the ONLY warm color, and only on the body --
// v8: fully recovered snaps to INDIGO (cool machine "ready"); anything worked graduates
// through a warm ember sweep tan → orange → red. The snap avoids a muddy blue→tan lerp.
// Screens never hand-pick these hues — they ask GymTheme.colors.heat.at().
@Immutable
class HeatScale(
    val ready: Color,   // fully recovered — indigo (cool "go")
    val worn: Color,    // mid recovery — warm tan
    val hot: Color,     // recently worked — orange
    val spent: Color,   // just trained / maximal fatigue — red
) {
    /** Maps freshness (1f = fully recovered … 0f = just worked) onto the ramp. */
    fun at(freshness: Float): Color {
        val fatigue = 1f - freshness.coerceIn(0f, 1f)
        return when {
            fatigue <= 0.10f -> ready                                       // recovered → indigo
            fatigue <= 0.55f -> lerp(worn, hot, (fatigue - 0.10f) / 0.45f)  // tan → orange
            else -> lerp(hot, spent, (fatigue - 0.55f) / 0.45f)            // orange → red
        }
    }
}

// Blue-hour floor: crisp indigo "ready", warm ember sweep for worked muscle
val DarkHeat = HeatScale(
    ready = AccentPrimary,
    worn = Color(0xFFC98A4E),
    hot = Color(0xFFFF7A2F),
    spent = Color(0xFFFF4D3D),
)

// Daylight: every stop deepened so it holds ≥3:1 contrast on paper white
val LightHeat = HeatScale(
    ready = Color(0xFF4A45E0),  // = AccentPrimaryLight (declared below; literal
                                //   avoids a forward reference in top-level init order)
    worn = Color(0xFFB06A28),
    hot = Color(0xFFD9591E),
    spent = Color(0xFFD70015),
)

val HeatWhite = Color(0xFFEAF0FF)      // flash highlights only (celebrations) — cool white

// --- Liquid glass tokens ------------------------------------------------------
// Fallback wash for glass panels that sit inside the blur source (cards)
val GlassTintDark = Color(0xB312141B)        // ~70% SurfaceDark
val GlassTintBlurDark = Color(0x6612141B)    // wash over real backdrop blur
val GlassHighlightDark = Color(0x22FFFFFF)   // neutral hairline top edge
val GlassOutlineDark = Color(0x12FFFFFF)     // neutral hairline bottom edge

val GlassTintLight = Color(0xCCFFFFFF)
val GlassTintBlurLight = Color(0x99FFFFFF)
val GlassHighlightLight = Color(0xF2FFFFFF)
val GlassOutlineLight = Color(0x33D0D3D8)

// --- Light palette (daylight: cool paper-grey, iron text, deepened indigo) ------
val PaperLight = Color(0xFFF4F5F8)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceRaisedLight = Color(0xFFECEEF3)
val OutlineLight = Color(0xFFDCDFE6)
val OutlineFaintLight = Color(0xFFE7E9EF)
val TextPrimaryLight = Color(0xFF111219)
val TextSecondaryLight = Color(0xFF5C616E)
val TextHintLight = Color(0xFF9AA0AC)
val AccentPrimaryLight = Color(0xFF4A45E0)       // indigo, deepened for light surfaces
val AccentContainerLight = Color(0xFFE5E5FB)
val OnAccentContainerLight = Color(0xFF25239A)
val CyanLight = Color(0xFF0B7FAD)                // ice, darkened
val SuccessLight = Color(0xFF4A45E0)             // completed = the indigo signal
val SuccessDimLight = Color(0xFFE5E5FB)
val PrGoldLight = Color(0xFFB08900)
val ErrorLight = Color(0xFFD70015)
