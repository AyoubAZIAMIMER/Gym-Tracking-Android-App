// Purpose: Draggable liquid-glass rest timer bubble; tap to expand +15s/Skip controls.
//          The ring shrinks smoothly and shifts colour ember → orange → red as time runs
//          out, pulses through the final 5 s, and ticks a haptic each of those seconds with
//          a firmer buzz on completion.
// Inputs: remaining/total seconds from RestTimerService.state; HazeState for backdrop blur
// Outputs: onAdd15 / onSkip events (routed back to the service)
package com.gymtracker.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.gymtracker.ui.theme.FONT_FEATURE_TABULAR
import com.gymtracker.ui.theme.ForgeExpression
import com.gymtracker.ui.theme.GymTheme
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

    val finalCountdown = remainingSec in 1..5

    // Gentle haptics: a light tick each of the last 5 seconds, a firmer buzz on completion.
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(remainingSec) {
        when {
            remainingSec == 0 -> haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            remainingSec in 1..5 -> haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

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
            // Smooth sweep: the ring eases between each second's value instead of snapping.
            val fraction = if (totalSec == 0) 0f else (remainingSec / totalSec.toFloat()).coerceIn(0f, 1f)
            val animFraction by animateFloatAsState(
                targetValue = fraction,
                animationSpec = tween(if (finalCountdown) 250 else 950, easing = LinearEasing),
                label = "restSweep",
            )

            // Colour runs the primary signal (plenty of time) → orange → red (almost done).
            val signal = MaterialTheme.colorScheme.primary
            val heat = GymTheme.colors.heat
            val ringColor = if (animFraction > 0.5f) {
                lerp(heat.hot, signal, (animFraction - 0.5f) / 0.5f)
            } else {
                lerp(heat.spent, heat.hot, (animFraction / 0.5f).coerceIn(0f, 1f))
            }
            val track = MaterialTheme.colorScheme.outlineVariant

            // §7.3 ambient breath; through the final 5 s it quickens and deepens into a pulse.
            // Energy.Calm kills every ambient loop — reduce-motion means reduce motion.
            val ambient = ForgeExpression.current.ambientLoops
            val breath = rememberInfiniteTransition(label = "restBreath")
            val tRaw by breath.animateFloat(
                initialValue = 0f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(if (finalCountdown) 520 else 2_000, easing = Motion.Temper),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "restPulse",
            )
            val t = if (ambient) tRaw else 0f
            val amp = if (finalCountdown) 0.08f else 0.02f
            val scale = 1f + amp * (t * 2f - 1f)

            Box(
                Modifier
                    .size(52.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 4.dp.toPx()
                    val d = size.minDimension - stroke
                    val topLeft = Offset((size.width - d) / 2f, (size.height - d) / 2f)
                    val arc = Size(d, d)
                    drawArc(track, -90f, 360f, false, topLeft, arc, style = Stroke(stroke, cap = StrokeCap.Round))
                    // final-5 s halo behind the arc
                    if (finalCountdown) {
                        drawArc(
                            ringColor.copy(alpha = 0.35f * (0.4f + 0.6f * t)),
                            -90f, -360f * animFraction, false,
                            topLeft, arc, style = Stroke(stroke * 2.4f, cap = StrokeCap.Round),
                        )
                    }
                    drawArc(
                        ringColor, -90f, -360f * animFraction, false,
                        topLeft, arc, style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
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
                        color = if (finalCountdown) ringColor else MaterialTheme.colorScheme.onSurface,
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
