// Purpose: All Forged colors — restored "Molten Forge" identity (v9, 2026-08-07): dark iron and
//          smoked steel with ONE hot color (ember orange) as the primary/action signal; molten
//          gold strictly for PRs; quenched steel blue-grey reads calm/recovered data. Restores the
//          v5 palette (superseded by v7 "Night Session" and v8 "Blue Hour") because the Claude
//          Design mockup at design/redesign-2026-07/ was built against it — see IDENTITY_V9.md.
// Inputs: none (constants)
// Outputs: Color values consumed by Theme.kt (never reference these directly from screens)
package com.gymtracker.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

// --- Dark palette (the designed, default theme: iron under work light) --------
val Ink = Color(0xFF111110)            // iron — screen background
val SurfaceDark = Color(0xFF1A1918)    // smoked steel — cards
val SurfaceRaised = Color(0xFF232221)  // nested cards, input fields
val OutlineDark = Color(0xFF2C2A27)
val OutlineFaint = Color(0xFF201F1D)

// --- The action colour (v10 "Chalk & Iron") -----------------------------------
// Chalk, not ember. The chrome is colourless so that warm hue means exactly one thing in this
// app: heat, i.e. data. Ember survives as an opt-in Heat setting, never as the default.
val Chalk = Color(0xFFEDE6D8)
val ChalkContainer = Color(0xFF262523)   // tinted containers / icon tiles / active nav pill
val ChalkBright = Color(0xFFFFFBF2)      // on-container text
val OnChalk = Color(0xFF151311)          // label on a chalk fill
val InkAction = Color(0xFF1B150E)        // the light-theme action fill

// Ember — NO LONGER the app's action colour. It is the hot stop of the data heat scale below,
// and the Heat.Ember opt-in. Do not reach for it as chrome.
val AccentPrimary = Color(0xFFFF5A1F)
val AccentPrimaryDim = Color(0xFF3A1608)     // tinted containers / icon tiles
val AccentPrimaryBright = Color(0xFFFFB294)  // on-container text
val OnAccentPrimary = Color(0xFF1C0800)

// Secondary: quenched steel — cool blue-grey for timers/durations/secondary data
val Cyan = Color(0xFF9FB6C2)
val CyanDim = Color(0xFF232B30)

// Forge red — errors, failure sets (hotter, redder than the ember primary)
val ActivityPink = Color(0xFFFF4B36)

val TextPrimary = Chalk                // chalk — 15:1 on Ink
val TextSecondary = Color(0xFF8A8378)  // 5.4:1 on Ink
val TextHint = Color(0xFF57524A)       // decorative only — never body copy

// Success is its own hue again — not merged into the one signal color (v5/mockup: "This
// Week" checkmarks are olive, distinct from the ember Start button)
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

// Rank medals (subtle gamification, v8 — unrelated to the ember/indigo axis, kept as-is).
// Gold rank reuses PrGold; Olympian uses chalk (onSurface).
val RankWood = Color(0xFF8A7A66)
val RankBronze = Color(0xFFB57C4A)
val RankSilver = Color(0xFFB9C0CC)
val RankWoodLight = Color(0xFF6E5E4A)
val RankBronzeLight = Color(0xFF9A6636)
val RankSilverLight = Color(0xFF71798A)

// --- Heat scale (Identity v5, design/IDENTITY_V5.md: "heat is data") ----------
// Steel reads its temperature: quenched blue = recovered/calm, glowing red = just worked.
// Screens never hand-pick a heat hue — they ask GymTheme.colors.heat.at(). Field names
// (ready/worn/hot/spent) kept from the v8 API so call sites across Recovery/Strike Mode/
// the rest timer don't need to change — only the values and curve are restored to v5.
@Immutable
class HeatScale(
    val ready: Color,   // quenched — fully recovered, ready to strike
    val worn: Color,    // warming — mid recovery / moderate effort
    val hot: Color,     // ember — working, the one hot color
    val spent: Color,   // glowing — just trained / maximal effort
) {
    /** Maps freshness (1f = fully recovered … 0f = just worked) onto the spectrum. */
    fun at(freshness: Float): Color {
        val heat = 1f - freshness.coerceIn(0f, 1f)
        return when {
            heat <= 0.45f -> lerp(ready, worn, heat / 0.45f)
            heat <= 0.75f -> lerp(worn, hot, (heat - 0.45f) / 0.30f)
            else -> lerp(hot, spent, (heat - 0.75f) / 0.25f)
        }
    }
}

// Night forge: hot hues read against dark iron
val DarkHeat = HeatScale(
    ready = Color(0xFF8FB4C7),
    worn = Color(0xFFD08A45),
    hot = AccentPrimary,
    spent = Color(0xFFFF3320),
)

// Daylight workshop: every stop deepened so it holds ≥3:1 contrast on bone paper
val LightHeat = HeatScale(
    ready = Color(0xFF4E7086),
    worn = Color(0xFF9A5A1D),
    hot = Color(0xFFC63D08),  // = AccentPrimaryLight (declared below; literal avoids
                               //   a forward reference in top-level init order)
    spent = Color(0xFFB3230F),
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
