// Purpose: Typography scale — Anton (bundled, OFL) as the forge display voice for
//          hero numbers and screen titles; system grotesk for everything readable
// Inputs: res/font/anton.ttf
// Outputs: AppTypography used by Theme.kt; FONT_FEATURE_TABULAR for aligned digits
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

// Anton ships one weight only — never pair it with a bold fontWeight (faux-bold smears it).
// It's loud by design: display/headline/titleLarge only; body text stays system grotesk.
val Forge = FontFamily(Font(R.font.anton))

val AppTypography = Typography(
    // Hero numerals ("3 Trainings", "820 kg" in the references)
    displayLarge = TextStyle(fontFamily = Forge, fontSize = 44.sp, lineHeight = 48.sp, letterSpacing = 0.5.sp),
    displaySmall = TextStyle(fontFamily = Forge, fontSize = 32.sp, lineHeight = 36.sp, letterSpacing = 0.5.sp),
    headlineLarge = TextStyle(fontFamily = Forge, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = 0.5.sp),
    headlineMedium = TextStyle(fontFamily = Forge, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = 0.5.sp),
    titleLarge = TextStyle(fontFamily = Forge, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = 0.4.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 1.2.sp),
)
