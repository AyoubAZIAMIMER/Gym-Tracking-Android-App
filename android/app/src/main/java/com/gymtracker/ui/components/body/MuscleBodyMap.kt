// Purpose: Recovery body map — front / back silhouettes with per-muscle heat tint.
//          Path data lives in MuscleBodyPaths.kt (generated). Heat is data: a muscle's colour
//          comes ONLY from GymTheme.colors.heat.at(freshness), never a hand-picked hex — but
//          the raw stop is chalk-muted before it fills a ~35-region body (2026-08-21): the same
//          hex that's correct on a small text badge reads as a saturated anatomy-poster fill at
//          this size, which fights the app's restrained chrome. See `mutedFill` below.
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.PathParser

enum class BodySide { Front, Back }

/**
 * The real drawing bounds of each side, measured from the parsed paths rather than the declared
 * viewport. The generated viewports (640 x 1260) are approximations — the feet clear the bottom
 * edge — so the old code added a blind `* 1.10f` of headroom and clipped. That left every figure
 * floating with ~10% dead space beneath it, which is what "not perfectly fitted" was.
 *
 * Both sides are measured together and share one box, so front and back render at exactly the
 * same scale and their feet line up. Each side is centred horizontally inside that shared box.
 */
internal object BodyArt {
    private data class Bounds(val left: Float, val top: Float, val right: Float, val bottom: Float) {
        val width get() = right - left
        val height get() = bottom - top
    }

    private fun boundsOf(shapes: List<MuscleShape>): Bounds {
        var l = Float.MAX_VALUE; var t = Float.MAX_VALUE
        var r = -Float.MAX_VALUE; var b = -Float.MAX_VALUE
        shapes.forEach { shape ->
            shape.paths.forEach { d ->
                val rect = PathParser().parsePathString(d).toPath().getBounds()
                if (rect.width > 0f && rect.height > 0f) {
                    if (rect.left < l) l = rect.left
                    if (rect.top < t) t = rect.top
                    if (rect.right > r) r = rect.right
                    if (rect.bottom > b) b = rect.bottom
                }
            }
        }
        return Bounds(l, t, r, b)
    }

    private val front by lazy { boundsOf(MuscleBodyPaths.Front) }
    private val back by lazy { boundsOf(MuscleBodyPaths.Back) }

    /** Half the outline stroke (drawPath uses Stroke(1.2f)); it paints outside the path bounds. */
    private const val STROKE_PAD = 0.6f

    /** Shared box both sides are drawn into. */
    val boxWidth: Float by lazy { maxOf(front.width, back.width) + STROKE_PAD * 2f }
    val boxHeight: Float by lazy { maxOf(front.height, back.height) + STROKE_PAD * 2f }
    val aspect: Float by lazy { boxWidth / boxHeight }


    /** Translation, in path units, that centres a side inside the shared box. */
    fun offsetX(side: BodySide): Float =
        (if (side == BodySide.Front) front.left else back.left) -
            (boxWidth - (if (side == BodySide.Front) front.width else back.width)) / 2f

    fun offsetY(side: BodySide): Float =
        (if (side == BodySide.Front) front.top else back.top) -
            (boxHeight - (if (side == BodySide.Front) front.height else back.height)) / 2f
}

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

    // Parse once per side; PathParser is not cheap and these are ~35 groups.
    val parsed = remember(side) {
        shapes.map { shape -> shape.slug to shape.paths.map { PathParser().parsePathString(it).toPath() } }
    }

    Canvas(
        modifier
            .fillMaxWidth()
            // measured bounds, so the figure fills its box exactly — no dead space, no clipping
            .aspectRatio(BodyArt.aspect)
            // guard only: Canvas does not clip by default, and anything that escaped would paint
            // over the FRONT/BACK caption beneath it
            .clipToBounds()
    ) {
        val scale = size.width / BodyArt.boxWidth
        translate(-BodyArt.offsetX(side) * scale, -BodyArt.offsetY(side) * scale) {
            scale(scale) {
                parsed.forEachIndexed { index, (slug, paths) ->
                    val isMuscle = slug !in MuscleBodyPaths.Silhouette
                    val f = freshness[slug]
                    val fill = when {
                        !isMuscle -> silhouette
                        f == null -> silhouette
                        else -> mutedFill(heatAt(f), silhouette)
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
                        // the silhouette's own boundary pieces still want a crisp cut; the ~35
                        // muscle regions inside it don't — a full-strength line on every fibre
                        // seam is what made this read as a dissected anatomy plate rather than
                        // a heat field on a body
                        val strokeWidth = if (isMuscle) 0.9f else 1.2f
                        val strokeAlpha = if (isMuscle) alpha * 0.4f else alpha
                        drawPath(p, color = outline, style = Stroke(width = strokeWidth), alpha = strokeAlpha)
                    }
                }
            }
        }
    }
}

/**
 * Grounds a raw heat stop in the figure's own silhouette tone before it fills a muscle region.
 * `heat.at()` is tuned to read correctly as a small badge or a thin bar (Stats, Home, the
 * readiness segments on this same screen) — at ~35-region body-fill size the same hex reads as
 * a saturated anatomy-poster color instead of the app's muted iron/chalk chrome. Blending toward
 * the silhouette keeps every stop's hue and relative heat legible while taming the brightness.
 */
private fun mutedFill(raw: Color, silhouette: Color): Color = lerp(raw, silhouette, 0.32f)

// --- tiny local helpers so the file has no extra dependencies -------------------------------

private inline fun DrawScope.translate(dx: Float, dy: Float, block: DrawScope.() -> Unit) {
    drawContext.transform.translate(dx, dy)
    block()
    drawContext.transform.translate(-dx, -dy)
}

/**
 * Uniform scale about the ORIGIN.
 *
 * DrawTransform.scale defaults its pivot to the centre of the canvas, which makes
 * `translate(-left*s, -top*s) { scale(s) { … } }` mean something other than "map path units to
 * pixels" — the reason the measured-bounds fit first came out mis-positioned and clipped.
 * Pinning the pivot to zero makes the pair exactly (p - topLeft) * s.
 */
private inline fun DrawScope.scale(s: Float, block: DrawScope.() -> Unit) {
    drawContext.transform.scale(s, s, Offset.Zero)
    block()
    drawContext.transform.scale(1f / s, 1f / s, Offset.Zero)
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
            val parsed = remember(side) {
                shapes.map { shape ->
                    shape.slug to shape.paths.map { PathParser().parsePathString(it).toPath() }
                }
            }
            Canvas(
                Modifier
                    .weight(1f)
                    .aspectRatio(BodyArt.aspect)
                    .clipToBounds()
            ) {
                val scale = size.width / BodyArt.boxWidth
                translate(-BodyArt.offsetX(side) * scale, -BodyArt.offsetY(side) * scale) {
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
