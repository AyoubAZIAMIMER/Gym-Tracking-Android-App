// Purpose: The Forged mark — an F built from struck bars, its mid-arm sheared off at 45° like
//          stock cut on the anvil. Replaces the barbell/dumbbell mark (owner's call 2026-08-09:
//          a dumbbell says "gym app", and every generator draws one). Single path on a 32 x 32
//          grid, drawn in code so it takes any tint — chalk on Ink, Ink on the chalk CTA.
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The F, cut. Vertices on a 32 x 32 grid — a full-width top arm, a short mid arm whose end is
 * sheared at 45°, and a stem. Straight lines only: it holds its shape down to 16 px, where a
 * rounded mark turns to mush in the status bar.
 *
 * Also shipped as res/drawable/ic_forged_mark.xml for launcher / notification use; keep the two
 * in step if the geometry ever changes.
 */
private val MarkVertices = listOf(
    7f to 5.5f, 26.5f to 5.5f, 26.5f to 11.2f, 13.4f to 11.2f,
    13.4f to 15f, 23f to 15f, 18.4f to 20.4f, 13.4f to 20.4f,
    13.4f to 26.8f, 7f to 26.8f,
)

@Composable
fun ForgedMark(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    // the path is scale-invariant; only the unit changes, so build it once per size
    val path = remember { Path() }
    Canvas(modifier.size(size)) {
        val u = this.size.minDimension / 32f
        path.rewind()
        MarkVertices.forEachIndexed { i, (x, y) ->
            if (i == 0) path.moveTo(x * u, y * u) else path.lineTo(x * u, y * u)
        }
        path.close()
        drawPath(path, tint)
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
