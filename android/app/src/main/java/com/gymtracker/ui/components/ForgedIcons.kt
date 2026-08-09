// Purpose: The Forged icon set — hand-drawn stroke glyphs lifted verbatim from the redesign
//          prototype (design/redesign-2026-07/prototype/"Forged Prototype.dc.html", `iconP`
//          + `barbell`). Material's stock icons were close but generic; these are the marks
//          the design was actually drawn with, and they share one visual language:
//          24×24 viewport, 1.9 stroke, round caps and joins, no fill. The brand mark itself
//          lives in ForgedMark.kt (shipped by the handoff) — not here.
// Inputs: none (vector constants; tint comes from the Icon() call site)
// Outputs: ForgedIcons.* ImageVectors
package com.gymtracker.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

// Icons are tinted by Icon(tint = …) via a ColorFilter, so the baked colour is irrelevant.
private val Ink = SolidColor(Color.Black)

/** Prototype stroke spec: 24-unit box, 1.9 width, round cap + join, fill:none. */
private fun strokeIcon(name: String, pathData: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = addPathNodes(pathData),
        fill = null,
        stroke = Ink,
        strokeLineWidth = 1.9f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    ).build()

object ForgedIcons {
    /** Bottom-nav marks, in GlassBottomNav.kt tab order. */
    val Home: ImageVector by lazy { strokeIcon("ForgedHome", "M4 11 L12 4 L20 11 M6 10 V20 H18 V10") }
    val History: ImageVector by lazy {
        strokeIcon("ForgedHistory", "M12 8 V12 L15 14 M3.5 12 a8.5 8.5 0 1 0 2.6 -6.1 M3 4 v3.5 h3.5")
    }
    val Plan: ImageVector by lazy {
        strokeIcon("ForgedPlan", "M4 6 h16 v14 h-16 Z M4 10 h16 M8 3 v4 M16 3 v4")
    }
    val Library: ImageVector by lazy {
        strokeIcon("ForgedLibrary", "M4 9 v6 M20 9 v6 M7 7 v10 M17 7 v10 M7 12 h10")
    }
    val Body: ImageVector by lazy { strokeIcon("ForgedBody", "M3 12 h4 L10 5 L14 19 L17 12 h4") }
    val Stats: ImageVector by lazy { strokeIcon("ForgedStats", "M5 20 V11 M12 20 V5 M19 20 V14") }

    /** Clock face — rest timer, durations. */
    val Clock: ImageVector by lazy {
        strokeIcon("ForgedClock", "M12 7 V12 L15 14 M12 3 a9 9 0 1 0 .1 0")
    }
}
