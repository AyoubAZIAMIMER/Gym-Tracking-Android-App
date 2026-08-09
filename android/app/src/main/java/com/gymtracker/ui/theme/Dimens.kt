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
    val navBarRadius = 32.dp       // deliberately OFF AppShapes (extraLarge is 28)
    val navItemRadius = 20.dp
    val navIcon = 20.dp
    val listBottomSpacer = 112.dp  // last row must clear the floating nav

    // session
    val ctaHeight = 60.dp
    val stepperSize = 38.dp        // VISUAL only — pad the touch target to 48.dp
    val exerciseBadge = 46.dp
    val sessionBottomBar = 228.dp
    val scrollFade = 22.dp         // bottom mask so clipped rows read as scrollable

    // home hub (Dynamic Hub)
    val heroRadius = 24.dp
    val railCardWidth = 150.dp
    val railGap = 12.dp
    val railPeek = 22.dp           // = screenPadH, so the next card peeks by the gutter

    // home / plan
    val weekCell = 28.dp           // prototype squircle, not the earlier 34dp circle
    val weekCellRadius = 10.dp
    val ctaRadius = 16.dp
    val ctaSecondary = 56.dp       // the ⇄ swap button beside Start
    val avatar = 30.dp
    val restStripHeight = 48.dp
    val restStripBottom = 86.dp    // sits above the nav

    val statusBand = 46.dp         // replace with WindowInsets.statusBars in production
}
