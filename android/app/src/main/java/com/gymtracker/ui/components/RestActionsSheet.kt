// Purpose: The rest countdown and the manual stopwatch, together, reached by tapping the live
//          rest readout mid-workout (Strike caption / Slate pill). Two flat hairline-ruled
//          sections stacked in one sheet — not tabs — so both are visible at once instead of
//          one hiding behind the other. Deliberately not the generic blue segmented-tab +
//          grey-ring layout a reference app used for this same idea: the rest ring carries the
//          app's own heat ramp (it's live data racing a target), the stopwatch has no ring at
//          all (it isn't racing anything, and a static ring around it would just be decoration
//          with no data behind it — the one thing this app's identity work has tried hardest
//          to avoid).
// Inputs: live rest state (nullable) + stopwatch state, both hoisted in WorkoutSessionScreen
// Outputs: onFinishRest, onToggleStopwatch, onResetStopwatch, onDismiss
package com.gymtracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.gymtracker.ui.theme.FONT_FEATURE_TABULAR
import com.gymtracker.ui.theme.GymTheme
import com.gymtracker.utils.TimeFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestActionsSheet(
    restRemainingSec: Int?,
    restTotalSec: Int,
    onFinishRest: () -> Unit,
    stopwatchElapsedMs: Long,
    stopwatchRunning: Boolean,
    onToggleStopwatch: () -> Unit,
    onResetStopwatch: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StampText("REST")
            RestRing(restRemainingSec, restTotalSec)
            if (restRemainingSec != null) {
                ForgedCta(
                    label = "Finish rest",
                    onClick = { onFinishRest(); onDismiss() },
                    modifier = Modifier.padding(top = 4.dp),
                )
            } else {
                Text(
                    "No rest running",
                    style = MaterialTheme.typography.labelMedium,
                    color = GymTheme.colors.hint,
                )
            }

            SectionRule()

            StampText("STOPWATCH")
            Text(
                text = TimeFormat.clock(stopwatchElapsedMs),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFeatureSettings = FONT_FEATURE_TABULAR,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 6.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                IconButton(onClick = onResetStopwatch, enabled = stopwatchElapsedMs > 0) {
                    Icon(
                        Icons.Rounded.RestartAlt,
                        contentDescription = "Reset stopwatch",
                        tint = if (stopwatchElapsedMs > 0) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            GymTheme.colors.hint
                        },
                    )
                }
                IconButton(onClick = onToggleStopwatch) {
                    Icon(
                        if (stopwatchRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (stopwatchRunning) "Pause stopwatch" else "Start stopwatch",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                // balances the Reset button so Play/Pause sits dead centre
                Spacer(Modifier.size(48.dp))
            }
        }
    }
}

/**
 * Same heat ramp as the overlay bubble and the in-session readouts (quenched steel -> ember ->
 * red, flat spent-red once you're over) — one ring, drawn three places, always means the same
 * thing. No progress ring for the stopwatch below: there's no target to show a fraction of, and
 * a static circle drawn just to echo this one would be decoration wearing data's clothes.
 */
@Composable
private fun RestRing(remainingSec: Int?, totalSec: Int) {
    val heat = GymTheme.colors.heat
    val track = MaterialTheme.colorScheme.outlineVariant
    val overtime = (remainingSec ?: 0) < 0
    val fraction = when {
        remainingSec == null -> 0f
        overtime -> 1f
        else -> (remainingSec / totalSec.toFloat()).coerceIn(0f, 1f)
    }
    val ringColor = when {
        remainingSec == null -> GymTheme.colors.hint
        overtime -> heat.spent
        fraction > 0.5f -> lerp(heat.hot, heat.ready, (fraction - 0.5f) / 0.5f)
        else -> lerp(heat.spent, heat.hot, (fraction / 0.5f).coerceIn(0f, 1f))
    }
    Box(Modifier.size(176.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(176.dp)) {
            val stroke = 10.dp.toPx()
            val d = size.minDimension - stroke
            val topLeft = Offset((size.width - d) / 2f, (size.height - d) / 2f)
            val arcSize = Size(d, d)
            drawArc(track, -90f, 360f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            if (remainingSec != null) {
                drawArc(
                    ringColor, -90f, -360f * fraction, false,
                    topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        Text(
            text = if (remainingSec != null) TimeFormat.signedMmss(remainingSec) else "—",
            style = MaterialTheme.typography.displayLarge.copy(
                fontFeatureSettings = FONT_FEATURE_TABULAR,
            ),
            color = if (overtime) ringColor else MaterialTheme.colorScheme.onSurface,
        )
    }
}
