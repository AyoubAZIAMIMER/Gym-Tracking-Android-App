// Purpose: Recovery body map — front / back silhouettes with per-muscle heat tint.
//          Path data lives in MuscleBodyPaths.kt (generated). Heat is data: a muscle's colour
//          comes ONLY from GymTheme.colors.heat.at(freshness), never a hand-picked hex.
//          Per IDENTITY_V5 §2: no % pills on the body — the numbers live in the BY MUSCLE list.
// Inputs: freshness map (slug -> 0f just worked .. 1f fully recovered)
// Outputs: MuscleBodyMap()
package com.gymtracker.ui.components.body

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.dp
import com.gymtracker.data.ProgressionImporter
import com.gymtracker.ui.theme.ForgeExpression
import com.gymtracker.ui.theme.Motion
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.PathParser

enum class BodySide { Front, Back }

/**
 * @param freshness slug -> 0f (just trained, glowing) .. 1f (cooled, ready). Missing slugs draw neutral.
 * @param heatAt your existing GymTheme.colors.heat.at — passed in so this file owns no colour.
 */
@Composable
fun MuscleBodyMap(
    side: BodySide,
    freshness: Map<String, Float>,
    heatAt: (Float) -> Color,
    silhouette: Color,
    outline: Color,
    modifier: Modifier = Modifier,
) {
    // The prototype animates this map two ways (Forged Prototype.dc.html, `groups.forEach`):
    //   muscFade — each group fades/rises in, staggered 45 ms apart, once on reveal
    //   heatBreath — anything still hot keeps breathing 0.62 → 1 opacity, staggered
    // Both are gated on Energy != Calm there, so they are gated on ambientLoops here.
    val ambient = ForgeExpression.current.ambientLoops
    var revealed by rememberSaveable(side) { mutableStateOf(false) }
    LaunchedEffect(side) { revealed = true }
    val reveal by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = Motion.settle(Motion.SLOW),
        label = "muscFade",
    )
    val breath = rememberInfiniteTransition(label = "heatBreath")
    val breathT by breath.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2_600, easing = Motion.Temper),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "heatBreathT",
    )
    val shapes = if (side == BodySide.Front) MuscleBodyPaths.Front else MuscleBodyPaths.Back
    val viewport = if (side == BodySide.Front) MuscleBodyPaths.FrontViewport else MuscleBodyPaths.BackViewport

    // Parse once per side; PathParser is not cheap and these are ~35 groups.
    val parsed = remember(side) {
        shapes.map { shape -> shape.slug to shape.paths.map { PathParser().parsePathString(it).toPath() } }
    }

    Canvas(
        modifier
            .fillMaxWidth()
            // The paths run slightly past the declared viewport (feet clear its bottom edge), so
            // the box gets a little headroom and clips — otherwise the figure overlaps whatever
            // sits beneath it.
            .aspectRatio(viewport.width / (viewport.height * 1.10f))
            .clipToBounds()
    ) {
        val scale = size.width / viewport.width
        translate(-viewport.left * scale, -viewport.top * scale) {
            scale(scale) {
                parsed.forEachIndexed { index, (slug, paths) ->
                    val isMuscle = slug !in MuscleBodyPaths.Silhouette
                    val f = freshness[slug]
                    val fill = when {
                        !isMuscle -> silhouette
                        f == null -> silhouette
                        else -> heatAt(f)
                    }
                    // staggered reveal: group i starts 45 ms after group i-1
                    val stagger = (index * 0.045f)
                    val groupReveal = if (!ambient) 1f
                    else ((reveal - stagger) / (1f - stagger).coerceAtLeast(0.01f)).coerceIn(0f, 1f)
                    // a hot muscle breathes; a cooled one sits still
                    val hot = isMuscle && f != null && f < 0.6f
                    val breathAlpha = if (ambient && hot) {
                        0.62f + 0.38f * breathT
                    } else 1f
                    val alpha = groupReveal * breathAlpha
                    if (alpha <= 0.01f) return@forEachIndexed
                    paths.forEach { p ->
                        drawPath(p, color = fill, alpha = alpha)
                        drawPath(p, color = outline, style = Stroke(width = 1.2f), alpha = alpha)
                    }
                }
            }
        }
    }
}

// --- tiny local helpers so the file has no extra dependencies -------------------------------

private inline fun DrawScope.translate(dx: Float, dy: Float, block: DrawScope.() -> Unit) {
    drawContext.transform.translate(dx, dy)
    block()
    drawContext.transform.translate(-dx, -dy)
}

private inline fun DrawScope.scale(s: Float, block: DrawScope.() -> Unit) {
    drawContext.transform.scale(s, s)
    block()
    drawContext.transform.scale(1f / s, 1f / s)
}

/**
 * The repo tracks 10 canonical groups ("Quads", "Back"); this map draws ~35 anatomical slugs.
 * One canonical group lights every slug it covers, so "Back" tints traps + upper + lower back.
 */
private val CanonicalToSlugs: Map<String, List<String>> = mapOf(
    "Chest" to listOf("chest"),
    "Back" to listOf("upper-back", "lower-back", "trapezius"),
    "Shoulders" to listOf("deltoids"),
    "Biceps" to listOf("biceps", "forearm"),
    "Triceps" to listOf("triceps"),
    "Abs" to listOf("abs", "obliques"),
    "Glutes" to listOf("gluteal"),
    "Quads" to listOf("quadriceps", "adductors"),
    "Hamstrings" to listOf("hamstring"),
    "Calves" to listOf("calves", "tibialis"),
)

/**
 * Expand a canonical freshness map (percent 0..100) into the slug-keyed 0f..1f the Canvas wants.
 *
 * Every non-silhouette slug gets a value. The Canvas paints an untinted muscle in the flat
 * silhouette colour, which reads as a grey blob rather than anatomy — so a muscle we have no
 * data for defaults to fully cooled, which is also what it is: untrained inside the lookback.
 */
fun slugFreshness(byCanonicalPercent: Map<String, Int>): Map<String, Float> {
    val everyMuscle = (MuscleBodyPaths.Front + MuscleBodyPaths.Back)
        .map { it.slug }
        .filterNot { it in MuscleBodyPaths.Silhouette }
        .distinct()
    return buildMap {
        everyMuscle.forEach { put(it, 1f) }
        byCanonicalPercent.forEach { (group, percent) ->
            CanonicalToSlugs[group]?.forEach { slug -> put(slug, percent / 100f) }
        }
    }
}

/**
 * Exercise-target figure on the same anatomy as the readiness map (owner's call: one body
 * app-wide). The ordered muscle list drives PRIMARY (first) at full accent and SECONDARY (rest)
 * dimmer; everything else stays silhouette. Replaces the v5 `MuscleTargetFigure`, keeping its two
 * behaviours: the highlight fills in on every target change, and the primary movers breathe.
 *
 * @param muscles raw labels from the exercise ("Chest · Triceps"); bucketed via canonicalMuscle.
 */
@Composable
fun MuscleTargetFigure(
    muscles: List<String>,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val ambient = ForgeExpression.current.ambientLoops
    val accent = scheme.primary
    val silhouette = scheme.onSurface.copy(alpha = 0.10f)

    val canonical = remember(muscles) {
        muscles.mapNotNull { ProgressionImporter.canonicalMuscle(it) }
    }
    val primarySlugs = remember(canonical) {
        canonical.firstOrNull()?.let { CanonicalToSlugs[it] }.orEmpty().toSet()
    }
    val secondarySlugs = remember(canonical) {
        canonical.drop(1).flatMap { CanonicalToSlugs[it].orEmpty() }.toSet() - primarySlugs
    }

    // fill-in replays whenever the target changes (Motion §7)
    val lit = remember { Animatable(0f) }
    LaunchedEffect(muscles) {
        lit.snapTo(0f)
        lit.animateTo(1f, animationSpec = Motion.settle(Motion.SLOW))
    }
    // primary movers breathe — ambient, so Calm switches it off
    val breath = rememberInfiniteTransition(label = "targetBreath")
    val pulseRaw by breath.animateFloat(
        initialValue = 0.74f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_500, easing = Motion.Temper),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "targetPulse",
    )
    val pulse = if (ambient) pulseRaw else 1f

    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(BodySide.Front, BodySide.Back).forEach { side ->
            val shapes = if (side == BodySide.Front) MuscleBodyPaths.Front else MuscleBodyPaths.Back
            val viewport =
                if (side == BodySide.Front) MuscleBodyPaths.FrontViewport else MuscleBodyPaths.BackViewport
            val parsed = remember(side) {
                shapes.map { shape ->
                    shape.slug to shape.paths.map { PathParser().parsePathString(it).toPath() }
                }
            }
            Canvas(
                Modifier
                    .weight(1f)
                    .aspectRatio(viewport.width / (viewport.height * 1.10f))
                    .clipToBounds()
            ) {
                val scale = size.width / viewport.width
                translate(-viewport.left * scale, -viewport.top * scale) {
                    scale(scale) {
                        parsed.forEach { (slug, paths) ->
                            val isPrimary = slug in primarySlugs
                            val isSecondary = slug in secondarySlugs
                            val fill = when {
                                isPrimary -> accent
                                isSecondary -> accent.copy(alpha = 0.45f)
                                else -> silhouette
                            }
                            val alpha = when {
                                isPrimary -> lit.value * pulse
                                isSecondary -> lit.value
                                else -> 1f
                            }
                            paths.forEach { p ->
                                drawPath(p, color = fill, alpha = alpha)
                                drawPath(
                                    p,
                                    color = scheme.background,
                                    style = Stroke(width = 1.2f),
                                    alpha = alpha,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
