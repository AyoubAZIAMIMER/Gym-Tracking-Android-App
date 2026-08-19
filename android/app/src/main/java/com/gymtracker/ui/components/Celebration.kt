// Purpose: Reward motion — a lightweight confetti/particle burst and a trophy PR banner,
//          both on-brand (ember / gold / ice / chalk) and physics-driven from a single
//          Animatable so there are no per-frame allocations or animation leaks.
// Inputs: a `run` trigger (Boolean); optional origin + palette
// Outputs: none (pure overlay visualization); onFinished when a one-shot burst completes
package com.gymtracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.gymtracker.ui.theme.ForgeExpression
import com.gymtracker.ui.theme.GymTheme
import com.gymtracker.ui.theme.Motion
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private class Confetto(
    val xFrac: Float,      // start x across the width (0..1)
    val delay: Float,      // 0..0.35 stagger into the fall
    val drift: Float,      // horizontal travel over the fall (fraction of width)
    val swayAmp: Float,    // flutter amplitude
    val swayFreq: Float,
    val sizePx: Float,
    val rot0: Float,
    val rotSpeed: Float,
    val round: Boolean,
    val color: Color,
)

/**
 * A one-shot confetti fall. Set [run] true to play; particles rain from just above the top,
 * flutter with a little sway, spin, and fade out. Everything is derived from one progress
 * float, so a frame is a handful of transforms and draws — cheap even mid-workout.
 */
@Composable
fun ConfettiBurst(
    run: Boolean,
    modifier: Modifier = Modifier,
    count: Int = 46,
    durationMillis: Int = 2_100,
    onFinished: () -> Unit = {},
) {
    // Calm opts out of celebration motion altogether (ForgeExpression.ambientLoops)
    if (!ForgeExpression.current.ambientLoops) return
    val ember = MaterialTheme.colorScheme.primary
    val gold = GymTheme.colors.prGold
    val ice = MaterialTheme.colorScheme.secondary
    val chalk = MaterialTheme.colorScheme.onSurface
    val palette = remember(ember, gold, ice, chalk) { listOf(ember, gold, ice, chalk, ember, gold) }

    // seeded once per mount so the pattern is stable across recompositions
    val pieces = remember {
        val rnd = Random(System.nanoTime())
        List(count) {
            Confetto(
                xFrac = rnd.nextFloat(),
                delay = rnd.nextFloat() * 0.35f,
                drift = (rnd.nextFloat() - 0.5f) * 0.35f,
                swayAmp = 0.01f + rnd.nextFloat() * 0.03f,
                swayFreq = 2f + rnd.nextFloat() * 3f,
                sizePx = 8f + rnd.nextFloat() * 10f,
                rot0 = rnd.nextFloat() * 360f,
                rotSpeed = (rnd.nextFloat() - 0.5f) * 900f,
                round = rnd.nextBoolean(),
                color = palette[rnd.nextInt(palette.size)],
            )
        }
    }

    val progress = remember { Animatable(0f) }
    LaunchedEffect(run) {
        if (run) {
            progress.snapTo(0f)
            progress.animateTo(1f, animationSpec = tween(durationMillis, easing = Motion.Cool))
            onFinished()
        }
    }
    if (progress.value <= 0f || progress.value >= 1f) return

    Canvas(modifier.fillMaxSize()) {
        val p = progress.value
        pieces.forEach { c ->
            val t = ((p - c.delay) / (1f - c.delay)).coerceIn(0f, 1f)
            if (t <= 0f) return@forEach
            val yFrac = -0.08f + t * 1.16f                 // falls from just above top to below bottom
            val sway = c.swayAmp * sin(t * c.swayFreq * 2f * PI.toFloat())
            val x = (c.xFrac + c.drift * t + sway) * size.width
            val y = yFrac * size.height
            val alpha = when {
                t < 0.10f -> t / 0.10f
                t > 0.80f -> ((1f - t) / 0.20f)
                else -> 1f
            }.coerceIn(0f, 1f)
            val half = c.sizePx / 2f
            rotate(c.rot0 + c.rotSpeed * t, pivot = Offset(x, y)) {
                if (c.round) {
                    drawCircle(c.color, radius = half, center = Offset(x, y), alpha = alpha)
                } else {
                    drawRect(
                        c.color,
                        topLeft = Offset(x - half, y - half * 0.6f),
                        size = Size(c.sizePx, c.sizePx * 0.6f),
                        alpha = alpha,
                    )
                }
            }
        }
    }
}

/**
 * A PR banner that pops in with a trophy and the new best, then can be dismissed by the
 * caller flipping [visible]. Gold, by the gold-for-PR law.
 */
@Composable
fun PrBanner(visible: Boolean, label: String, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(Motion.springMass(), initialScale = 0.8f) + fadeIn(Motion.settle(Motion.STANDARD)),
        exit = scaleOut(Motion.cool(Motion.FAST), targetScale = 0.9f) + fadeOut(Motion.cool(Motion.FAST)),
        modifier = modifier,
    ) {
        Row(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(GymTheme.colors.prGold.copy(alpha = 0.16f))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            // trophy taps in on a gentle infinite shimmer would be overkill here; the reveal
            // (scale+fade) carries it. Static gold glyph keeps it premium-calm.
            Icon(
                Icons.Rounded.EmojiEvents,
                contentDescription = null,
                tint = GymTheme.colors.prGold,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = GymTheme.colors.prGold,
            )
        }
    }
}
