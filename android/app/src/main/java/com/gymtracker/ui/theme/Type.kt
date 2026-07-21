// Purpose: Typography scale — Identity v7 "Night Session": the platform grotesk at
//          Black weight with tight tracking for display (the Apple-on-Android move;
//          Anton retired with the forge identity), system weights for everything else
// Inputs: none (system fonts)
// Outputs: AppTypography used by Theme.kt; FONT_FEATURE_TABULAR for aligned digits
package com.gymtracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Apply via TextStyle.copy(fontFeatureSettings = FONT_FEATURE_TABULAR) wherever
// digits must align in columns (sets table, timers, stat tiles)
const val FONT_FEATURE_TABULAR = "tnum"

// Display voice: heavy + tight, like SF Pro Display Bold — not a poster font.
private val Display = FontWeight.Black

val AppTypography = Typography(
    // Hero numerals ("47.5", "3 workouts")
    displayLarge = TextStyle(fontWeight = Display, fontSize = 44.sp, lineHeight = 48.sp, letterSpacing = (-1.0).sp),
    displaySmall = TextStyle(fontWeight = Display, fontSize = 32.sp, lineHeight = 36.sp, letterSpacing = (-0.6).sp),
    headlineLarge = TextStyle(fontWeight = Display, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.4).sp),
    headlineMedium = TextStyle(fontWeight = Display, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 1.2.sp),
)
