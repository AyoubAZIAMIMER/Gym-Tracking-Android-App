// Purpose: Spacing / sizing scale for the redesigned screens — the one token file the
//          repo was missing (Color, Type, Shape, Motion already exist; use those for
//          everything else and never hardcode a hex, size, radius or curve in a screen)
// Inputs: none (constants)
// Outputs: Dim.* consumed by the redesigned screens
package com.gymtracker.ui.theme

import androidx.compose.ui.unit.dp

object Dim {
    // structure
    val screenPadH = 22.dp
    val sectionTop = 18.dp
    val sectionBottom = 10.dp
    val rowPadV = 13.dp
    val hairline = 1.dp

    // bottom nav (floating, AppShapes.extraLarge)
    val navHeight = 64.dp
    val navInset = 14.dp
    val navItemRadius = 16.dp      // AppShapes.medium
    val listBottomSpacer = 112.dp  // last row must clear the floating nav

    // session
    val ctaHeight = 60.dp
    val stepperSize = 38.dp        // VISUAL only — pad the touch target to 48.dp
    val exerciseBadge = 46.dp
    val sessionBottomBar = 228.dp
    val scrollFade = 22.dp         // bottom mask so clipped rows read as scrollable

    // home / plan
    val weekCell = 34.dp
    val restStripHeight = 48.dp
    val restStripBottom = 86.dp    // sits above the nav

    val statusBand = 46.dp         // replace with WindowInsets.statusBars in production
}
