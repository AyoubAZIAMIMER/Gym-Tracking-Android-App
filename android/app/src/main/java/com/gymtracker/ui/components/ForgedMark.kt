// Purpose: The Forged barbell mark — a loaded bar with symmetric plates. Replaces the old
//          crossed-diamond spark. Five rounded rects on a 32 x 32 grid, drawn in code so it
//          can take any tint (accent on Ink, onAccent on the ember CTA, Ink on Paper).
// Inputs: size, tint
// Outputs: ForgedMark(), ForgedWordmark()
package com.gymtracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Barbell mark. Geometry on a 32 x 32 grid (x, y, w, h, r):
 *   bar          9.0, 14.25, 14.0, 3.50, r 1.5
 *   inner plates 6.5 / 22.0, 9.5, 3.5 x 13.0, r 1.6
 *   outer plates 3.0 / 26.0, 12.0, 3.0 x 8.0, r 1.4
 * Also shipped as res/drawable/ic_forged_mark.xml for launcher / notification use.
 */
@Composable
fun ForgedMark(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    Canvas(modifier.size(size)) {
        val u = this.size.minDimension / 32f
        fun bar(x: Float, y: Float, w: Float, h: Float, r: Float) = drawRoundRect(
            color = tint,
            topLeft = Offset(x * u, y * u),
            size = Size(w * u, h * u),
            cornerRadius = CornerRadius(r * u, r * u),
        )
        bar(9f, 14.25f, 14f, 3.5f, 1.5f)   // bar
        bar(6.5f, 9.5f, 3.5f, 13f, 1.6f)   // inner plate L
        bar(22f, 9.5f, 3.5f, 13f, 1.6f)    // inner plate R
        bar(3f, 12f, 3f, 8f, 1.4f)         // outer plate L
        bar(26f, 12f, 3f, 8f, 1.4f)        // outer plate R
    }
}

/** Mark + FORGED lockup used in the Home header. Anton, 15 sp, tracking +1.5. */
@Composable
fun ForgedWordmark(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
        ForgedMark(size = 24.dp, tint = tint)
        Text(
            text = "FORGED",
            // Anton is the forge voice — titleLarge would fake a weight Anton doesn't have
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 15.sp, letterSpacing = 1.5.sp),
            color = labelColor,
        )
    }
}
