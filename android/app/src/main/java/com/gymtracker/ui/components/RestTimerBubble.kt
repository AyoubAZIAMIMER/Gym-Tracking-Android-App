// Purpose: Draggable liquid-glass rest timer bubble; tap to expand +15s/Skip controls
// Inputs: remaining/total seconds from RestTimerService.state; HazeState for backdrop blur
// Outputs: onAdd15 / onSkip events (routed back to the service)
package com.gymtracker.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.gymtracker.ui.theme.FONT_FEATURE_TABULAR
import com.gymtracker.ui.theme.Motion
import com.gymtracker.utils.TimeFormat
import dev.chrisbanes.haze.HazeState
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun RestTimerBubble(
    remainingSec: Int,
    totalSec: Int,
    onAdd15: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
) {
    var offset by remember { mutableStateOf(Offset.Zero) }
    var expanded by remember { mutableStateOf(false) }

    GlassSurface(
        modifier = modifier
            .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offset += dragAmount
                }
            },
        shape = RoundedCornerShape(32.dp),
        hazeState = hazeState,
        onClick = { expanded = !expanded },
    ) {
        Row(
            Modifier.padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Forged Motion §7.3: the session's one live ember — the ring breathes ±2%
            // at a resting rate while you rest (temper curve, ambient only)
            val breath = rememberInfiniteTransition(label = "restBreath")
            val breathScale by breath.animateFloat(
                initialValue = 0.98f,
                targetValue = 1.02f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2_000, easing = Motion.Temper),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "breathScale",
            )
            Box(
                Modifier
                    .size(52.dp)
                    .graphicsLayer {
                        scaleX = breathScale
                        scaleY = breathScale
                    },
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    progress = { if (totalSec == 0) 0f else remainingSec / totalSec.toFloat() },
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outlineVariant,
                    strokeWidth = 4.dp,
                )
                // §7.4: numbers have mass — a +15 s jump rolls; normal ticking stays calm
                AnimatedContent(
                    targetState = remainingSec,
                    transitionSpec = {
                        if (abs(targetState - initialState) > 2) {
                            (slideInVertically(Motion.settle(Motion.STANDARD)) { -it } +
                                fadeIn(Motion.settle(Motion.FAST))) togetherWith
                                (slideOutVertically(Motion.cool(Motion.FAST)) { it } +
                                    fadeOut(Motion.cool(Motion.FAST)))
                        } else {
                            fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                        }
                    },
                    label = "restRoll",
                ) { sec ->
                    Text(
                        text = TimeFormat.mmss(sec),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFeatureSettings = FONT_FEATURE_TABULAR
                        ),
                    )
                }
            }
            if (expanded) {
                TextButton(onClick = onAdd15) { Text("+15s") }
                IconButton(onClick = onSkip) {
                    Icon(
                        Icons.Rounded.SkipNext,
                        contentDescription = "Skip rest",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
