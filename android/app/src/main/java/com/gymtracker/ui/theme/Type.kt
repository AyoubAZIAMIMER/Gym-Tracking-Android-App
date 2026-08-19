// Purpose: Typography scale — Anton IS the display voice (displayLarge → titleLarge), the
//          platform grotesk carries titleMedium 16 → bodySmall 12, and labelSmall is the
//          stamped mono micro-label. Per the v2 handoff README §Design tokens. Anton has one
//          weight: styles above never set fontWeight. See design/IDENTITY_V9.md.
// Inputs: res/font/anton.ttf
// Outputs: AppTypography used by Theme.kt; Anton family; FONT_FEATURE_TABULAR
package com.gymtracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.gymtracker.R

// Apply via TextStyle.copy(fontFeatureSettings = FONT_FEATURE_TABULAR) wherever
// digits must align in columns (sets table, timers, stat tiles)
const val FONT_FEATURE_TABULAR = "tnum"

/** The forge voice — one weight, never paired with bold. Brand mark, CTA label, hero numerals. */
val Anton = FontFamily(Font(R.font.anton))

/** Stamped mono label ("TODAY", "THIS WEEK") — the prototype's 11sp/1.2sp section rule. */
val StampLabel = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Bold,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    letterSpacing = 1.2.sp,
)

// The forge voice IS the display scale (handoff README §Design tokens): Anton carries
// displayLarge 44 / displaySmall 32 / headlineLarge 28 / headlineMedium 24 / titleLarge 20.
// Anton has ONE weight — never set fontWeight on these, it synthesises a fake bold.
val AppTypography = Typography(
    // Hero numerals ("47.5", "48,210 kg")
    displayLarge = TextStyle(fontFamily = Anton, fontSize = 44.sp, lineHeight = 48.sp, letterSpacing = (-1.0).sp),
    displaySmall = TextStyle(fontFamily = Anton, fontSize = 32.sp, lineHeight = 36.sp, letterSpacing = (-0.6).sp),
    headlineLarge = TextStyle(fontFamily = Anton, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.4).sp),
    headlineMedium = TextStyle(fontFamily = Anton, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontFamily = Anton, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = 0.5.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 1.2.sp),
)
