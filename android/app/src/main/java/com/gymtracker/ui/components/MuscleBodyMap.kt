// Purpose: The Forged physique — anatomically drawn front + back human figures with
//          individual muscle bellies tinted by forge heat (Identity v5: quenched steel
//          = recovered, glowing red = just worked; hot muscles emit a halo)
// Inputs: freshness map (canonical muscle → percent 0..100) or a target-muscle list
// Outputs: none (pure visualization)
package com.gymtracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import com.gymtracker.ui.theme.GymTheme
import kotlin.math.min

// Figures are authored in a 100×170 unit space (≈7.7 head heights — heroic but human),
// left side only; the right side is a programmatic mirror so the physique stays symmetric.
private const val BODY_W = 100f
private const val BODY_H = 170f

private fun path(d: String): Path = PathParser().parsePathString(d).toPath()

private fun Path.mirrored(): Path {
    val m = Matrix()
    m.translate(BODY_W, 0f)
    m.scale(-1f, 1f, 1f)
    return Path().apply {
        addPath(this@mirrored)
        transform(m)
    }
}

/** One muscle belly: canonical group name + its path(s), mirrored across the spine. */
private class MusclePath(val muscle: String, d: String, mirror: Boolean = true) {
    val paths: List<Path> = buildList {
        val p = path(d)
        add(p)
        if (mirror) add(p.mirrored())
    }
}

// --- Base silhouette (shared front/back): the unlit forging blank -------------
private val Silhouette: Path by lazy {
    val head = path(
        "M50 2 C54.5 2 56.5 5.5 56.5 10 C56.5 14.5 54 18 50 18 " +
            "C46 18 43.5 14.5 43.5 10 C43.5 5.5 45.5 2 50 2 Z"
    )
    val torso = path(
        "M46.2 16 L46.2 21 C42.5 22.6 36.5 24.4 31.5 27 C29 30.5 28.6 34.5 30 38 " +
            "C31.6 39.4 33.2 41 34 43.5 C35 51 36 58 36.5 64 C36.9 69 37.5 74 38 78 " +
            "C40 84 44.8 88.6 50 89.2 C55.2 88.6 60 84 62 78 C62.5 74 63.1 69 63.5 64 " +
            "C64 58 65 51 66 43.5 C66.8 41 68.4 39.4 70 38 C71.4 34.5 71 30.5 68.5 27 " +
            "C63.5 24.4 57.5 22.6 53.8 21 L53.8 16 Z"
    )
    val armLeft = path(
        "M31 26.5 C27.6 27.6 25.2 30.8 24.8 34.8 C24.3 40 23.6 46 22.8 52 " +
            "C21.8 55.8 20.8 59 20.3 62.5 C19.6 67.5 19 72.5 18.6 77 " +
            "C18.2 80.4 17.8 83.2 18.4 85.6 C19.4 88.2 22 88.3 23.1 85.7 " +
            "C24 82.4 24.3 79.4 24.6 77.2 C25.6 71.2 26.6 65.2 27.6 60 " +
            "C28.6 55.8 29.5 52 30.1 49 C31.1 45 32.4 42.2 34.2 40.2 " +
            "C34.3 34.6 33.6 29.2 31 26.5 Z"
    )
    val legLeft = path(
        "M38 78 C36.6 84 35.6 90 35.9 96 C36.2 104 37.2 112 39.2 119.5 " +
            "C38.6 125.5 37.7 129.5 37.5 133.5 C37.2 140 38.6 148 40.6 155.5 " +
            "C40.9 158.8 40.6 161.8 39.6 164.2 L45.9 165 C46.4 162 46.3 159 46.1 156 " +
            "C46.9 148 47.1 140 46.6 132.5 C46.1 126.5 45.3 122.5 45.1 119.5 " +
            "C45.6 112 46.6 102 47.6 94 C48.6 89 49.5 86 50 84.5 L50 80.5 " +
            "C46 79.6 41 78.6 38 78 Z"
    )
    // Single Path with non-zero fill = the union fills once, so the low-alpha base
    // never double-stacks where limbs overlap the torso
    Path().apply {
        addPath(head)
        addPath(torso)
        addPath(armLeft)
        addPath(armLeft.mirrored())
        addPath(legLeft)
        addPath(legLeft.mirrored())
    }
}

// --- Front muscles ------------------------------------------------------------
private val FrontMuscles: List<MusclePath> by lazy {
    listOf(
        MusclePath( // upper traps visible from the front, along the neck slope
            "Back",
            "M46.4 20.8 C42.8 22.2 37.4 24 33 26.4 C36.6 27.2 41.6 26.8 44.8 24.8 " +
                "C45.7 23.4 46.2 22 46.4 20.8 Z"
        ),
        MusclePath(
            "Shoulders",
            "M31.4 27 C27.9 28.1 25.9 31.3 25.7 35.3 C25.7 38.2 26.6 40.2 28.1 40.8 " +
                "C30.5 41.3 32.7 39.4 33.5 36 C34.1 32.5 33.5 28.8 31.4 27 Z"
        ),
        MusclePath(
            "Chest",
            "M49.3 30.5 C44 30 38.2 30.6 34.7 32.6 C34 35.1 34 38 35 40.4 " +
                "C37.2 44.4 42 46.4 46.4 45.9 C48.4 45.4 49.2 43.9 49.3 41.5 Z"
        ),
        MusclePath(
            "Biceps",
            "M26.6 42.6 C25.7 46 25.1 50.4 25.3 53.9 C26.4 56.4 28.8 56.7 30.2 54.4 " +
                "C31 51 31.4 46.6 30.8 43.6 C29.5 41.6 27.7 41.9 26.6 42.6 Z"
        ),
        MusclePath( // rectus abdominis; the mirror gap forms the linea alba
            "Abs",
            "M45.2 48.6 C42.8 49.1 41.6 50.1 41.4 52 L41.4 75.5 " +
                "C41.5 78.8 43.1 81.4 45.6 82.4 C47.3 83.1 49 83.4 49.5 83.4 L49.5 48.8 Z"
        ),
        MusclePath( // obliques flank the rectus
            "Abs",
            "M38.3 50.5 C37.4 55.5 37 60.5 37.3 65.5 C37.8 70 38.8 74 40.2 77 " +
                "C40.8 72.8 40.8 67.5 40.5 62 C40.2 56.8 39.5 52.8 38.3 50.5 Z"
        ),
        MusclePath( // quad mass with the medialis teardrop toward the inner knee
            "Quads",
            "M37.2 82 C36.2 88 36 95 36.4 101 C37 108 38.4 113.8 40.4 118.2 " +
                "C42.6 119.2 44.4 117.8 44.7 114.8 C45.3 107.8 46.1 99.8 46.9 92 " +
                "C47.3 87 46.1 83.6 43.6 82.2 C41.4 81.2 39 81.2 37.2 82 Z"
        ),
        MusclePath( // lower leg from the front (tibialis + visible calf edges)
            "Calves",
            "M37.9 123.5 C37.1 130 36.9 138 37.6 144.8 C38.4 150.8 39.7 154.8 41.6 156.8 " +
                "C43.4 155.2 44.8 150.8 45.6 144.8 C46.4 138 46.3 130.5 45.6 124.5 " +
                "C43.1 122.6 40.1 122.6 37.9 123.5 Z"
        ),
    )
}

// Ab cut lines — stroked, not filled; drawn in background ink for definition
private val FrontDetails: List<Path> by lazy {
    val cuts = path(
        "M41.8 57.5 C44 58.2 47.2 58.4 49.4 58.2 " +
            "M41.8 63.8 C44 64.5 47.2 64.7 49.4 64.5 " +
            "M41.8 70.2 C44 70.9 47.2 71.1 49.4 70.9"
    )
    listOf(cuts, cuts.mirrored())
}

// --- Back muscles ---------------------------------------------------------------
private val BackMuscles: List<MusclePath> by lazy {
    listOf(
        MusclePath( // trapezius diamond, neck to mid-spine
            "Back",
            "M49.8 20.5 C46.4 22 41.6 24.2 38.2 26.2 C41 27.6 44.2 30.1 46.2 33.7 " +
                "C48.2 38.2 49.5 43.8 49.8 48.8 Z"
        ),
        MusclePath(
            "Shoulders",
            "M31.4 27 C27.9 28.1 25.9 31.3 25.7 35.3 C25.7 38.2 26.6 40.2 28.1 40.8 " +
                "C30.5 41.3 32.7 39.4 33.5 36 C34.1 32.5 33.5 28.8 31.4 27 Z"
        ),
        MusclePath(
            "Triceps",
            "M26.4 42.2 C25.5 46.6 24.9 51.4 25.2 55.4 C26.3 57.9 28.7 58.1 30.1 55.7 " +
                "C31 51.5 31.3 46.7 30.7 43.1 C29.4 40.9 27.5 41.3 26.4 42.2 Z"
        ),
        MusclePath( // lats sweeping armpit → waist: the V-taper
            "Back",
            "M34.6 40.2 C33.9 44 34.1 48 35.1 52 C36.6 57.4 39.5 61.8 43.4 65.2 " +
                "C45.9 67.2 48.4 68.3 49.6 68.6 L49.6 55 C49.6 50 48 45.2 45.1 42.7 " +
                "C41.7 40.2 37.6 39.5 34.6 40.2 Z"
        ),
        MusclePath( // spinal erectors
            "Back",
            "M46.1 69.2 C44.6 72.2 44.1 76 44.4 79.8 C45.6 82.2 47.8 83.3 49.6 83.5 " +
                "L49.6 69.7 C48.4 69.6 47.1 69.4 46.1 69.2 Z"
        ),
        MusclePath(
            "Glutes",
            "M43.1 84.2 C39.6 85.2 37.6 88.2 37.4 92.2 C37.3 96.6 39.1 100.1 42.6 101.6 " +
                "C46.1 103 48.9 101.8 49.6 98.6 C49.9 94.2 49.8 89.2 49.4 85.7 " +
                "C47.6 84.2 45.1 83.8 43.1 84.2 Z"
        ),
        MusclePath(
            "Hamstrings",
            "M38.1 104.2 C37.3 110 37.1 116 37.7 121 C38.5 125 40.1 127.4 42.1 128.4 " +
                "C44.1 127.4 45.7 125 46.4 121 C47.1 115.4 47.1 109.2 46.5 104.6 " +
                "C43.9 102.9 40.6 102.9 38.1 104.2 Z"
        ),
        MusclePath( // gastrocnemius diamond
            "Calves",
            "M38.3 131.2 C37.6 136 37.6 141.6 38.5 146.4 C39.5 150.9 41.1 153.9 42.3 154.9 " +
                "C43.6 153.9 45.1 150.9 46 146.4 C46.8 141.6 46.7 136 45.9 131.2 " +
                "C43.4 129.3 40.6 129.3 38.3 131.2 Z"
        ),
    )
}

/**
 * Recovery hero: front + back physique, every muscle tinted by forge heat.
 * No % pills on the body (they hid the anatomy in v4) — exact numbers belong to the
 * BY MUSCLE list; the body reads by color against the heat legend.
 */
@Composable
fun MuscleBodyMap(freshness: Map<String, Int>, modifier: Modifier = Modifier) {
    val base = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    val noData = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.13f)
    val ink = MaterialTheme.colorScheme.background
    val hint = GymTheme.colors.hint
    val heatScale = GymTheme.colors.heat

    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) {
            val slotW = size.width * 0.44f
            listOf(
                FrontMuscles to size.width * 0.045f,
                BackMuscles to size.width * 0.515f,
            ).forEach { (muscles, slotX) ->
                drawPhysique(
                    muscles = muscles,
                    details = if (muscles === FrontMuscles) FrontDetails else emptyList(),
                    slotX = slotX, slotW = slotW,
                    base = base, noData = noData, ink = ink,
                    tintFor = { m -> freshness[m]?.let { heatScale.at(it / 100f) } },
                    glowFor = { m ->
                        val f = freshness[m]
                        if (f == null) 0f
                        else (((1f - f / 100f) - 0.35f) / 0.65f).coerceIn(0f, 1f)
                    },
                )
            }
        }
        Row(Modifier.fillMaxWidth()) {
            listOf("FRONT", "BACK").forEach { label ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = hint)
                }
            }
        }
    }
}

/**
 * Exercise detail: same physique, target muscles at full ember glow, the rest cold —
 * works offline for every exercise, including custom ones.
 */
@Composable
fun MuscleTargetFigure(muscles: List<String>, modifier: Modifier = Modifier) {
    val base = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    val cold = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val hot = MaterialTheme.colorScheme.primary
    val ink = MaterialTheme.colorScheme.background
    val targets = muscles.toSet()

    Canvas(
        modifier
            .fillMaxWidth()
            .height(210.dp)
    ) {
        val slotW = size.width * 0.44f
        listOf(
            FrontMuscles to size.width * 0.045f,
            BackMuscles to size.width * 0.515f,
        ).forEach { (group, slotX) ->
            drawPhysique(
                muscles = group,
                details = if (group === FrontMuscles) FrontDetails else emptyList(),
                slotX = slotX, slotW = slotW,
                base = base, noData = cold, ink = ink,
                tintFor = { m -> if (m in targets) hot else null },
                glowFor = { m -> if (m in targets) 1f else 0f },
            )
        }
    }
}

private fun DrawScope.drawPhysique(
    muscles: List<MusclePath>,
    details: List<Path>,
    slotX: Float,
    slotW: Float,
    base: Color,
    noData: Color,
    ink: Color,
    tintFor: (String) -> Color?,
    glowFor: (String) -> Float,
) {
    val s = min(slotW / BODY_W, size.height / BODY_H)
    val dx = slotX + (slotW - BODY_W * s) / 2f
    val dy = (size.height - BODY_H * s) / 2f
    withTransform({
        translate(dx, dy)
        scale(s, s, pivot = Offset.Zero)
    }) {
        drawPath(Silhouette, base)
        muscles.forEach { m ->
            val tint = tintFor(m.muscle)
            val glow = if (tint != null) glowFor(m.muscle) else 0f
            m.paths.forEach { p ->
                if (tint == null) {
                    drawPath(p, noData)
                } else {
                    if (glow > 0f) {
                        // hot metal radiates: two widening translucent strokes as halo
                        drawPath(
                            p, tint, alpha = 0.16f * glow,
                            style = Stroke(width = 6f, join = StrokeJoin.Round),
                        )
                        drawPath(
                            p, tint, alpha = 0.30f * glow,
                            style = Stroke(width = 2.5f, join = StrokeJoin.Round),
                        )
                    }
                    drawPath(p, tint, alpha = 0.95f)
                    // ink seam so adjacent hot muscles stay individually readable
                    drawPath(p, ink, alpha = 0.35f, style = Stroke(width = 0.5f))
                }
            }
        }
        details.forEach {
            drawPath(
                it, ink, alpha = 0.45f,
                style = Stroke(width = 0.7f, cap = StrokeCap.Round),
            )
        }
    }
}
