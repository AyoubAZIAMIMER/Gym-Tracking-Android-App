// Purpose: The Forged physique — anatomically drawn front + back human figures with
//          individually sculpted muscle bellies (Blue Hour v8). Major groups are broken
//          into their real heads (quads → vastus lateralis / rectus femoris / vastus
//          medialis; hamstrings and calves into two heads; pec, lats, traps shaped from
//          anatomy) with an always-on striation layer for definition. A soft radial sheen
//          gives each belly depth; the Body screen tints them by ember readiness, the
//          exercise figure lights PRIMARY movers full indigo and SECONDARY movers dimmer,
//          springing in on target change with a gentle breath on the primaries.
// Inputs: freshness map (canonical muscle → percent 0..100) or an ordered target-muscle list
//         (first = primary mover, rest = secondary)
// Outputs: none (pure visualization)
package com.gymtracker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
import com.gymtracker.ui.theme.Motion
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

/**
 * One muscle belly: canonical group + its path, mirrored across the spine. Carries the
 * belly [center] and [radius] used to lay a soft radial sheen (the sculpted, 3-D read).
 * A canonical group (e.g. Quads) can have several of these — its anatomical heads.
 */
private class MusclePath(
    val muscle: String,
    d: String,
    val center: Offset,
    val radius: Float = 12f,
    mirror: Boolean = true,
) {
    val paths: List<Path>
    val centers: List<Offset>

    init {
        val p = path(d)
        if (mirror) {
            paths = listOf(p, p.mirrored())
            centers = listOf(center, Offset(BODY_W - center.x, center.y))
        } else {
            paths = listOf(p)
            centers = listOf(center)
        }
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
        MusclePath( // upper trapezius, along the neck slope
            "Back",
            "M47 18.5 C43.5 20 38.5 22.2 33.5 26 C37 27.2 42 26.6 45.2 24.4 " +
                "C46.3 22.6 46.9 20.6 47 18.5 Z",
            center = Offset(41f, 23f), radius = 9f,
        ),
        MusclePath( // anterior deltoid — the rounded cap
            "Shoulders",
            "M33.6 26 C29.2 27 25.9 30.6 25.4 35.2 C25.2 38.4 26.4 40.6 28.6 41.2 " +
                "C31.6 41.4 33.9 38.6 34.6 34.4 C35.1 30.8 34.9 27.6 33.6 26 Z",
            center = Offset(29.5f, 33f), radius = 9f,
        ),
        MusclePath( // pectoralis major (sternal + clavicular mass)
            "Chest",
            "M48.8 29.4 C43.5 28.8 37.8 30 34.7 32.8 C34 35.6 34.2 39.4 35.8 42.4 " +
                "C38.2 45.6 42.6 47.2 46.6 46.6 C48.2 46.1 48.8 44.3 48.8 42 Z",
            center = Offset(42.5f, 38f), radius = 12f,
        ),
        MusclePath( // biceps brachii peak
            "Biceps",
            "M26.6 42.4 C25.1 46 24.6 51 25.1 55 C26.1 57.5 28.6 57.8 30.1 55.5 " +
                "C31.1 51.5 31.4 46.6 30.6 43 C29.4 41.2 27.6 41.4 26.6 42.4 Z",
            center = Offset(28f, 48.5f), radius = 8f,
        ),
        MusclePath( // forearm flexors, biceps → wrist
            "Forearms",
            "M25.5 57 C24 61 22.5 66 21.6 71 C20.9 75.5 20.6 79.5 20.9 82.5 " +
                "C21.7 84 23.1 83.8 23.9 82 C24.8 78 25.4 73.5 26 69 " +
                "C26.6 64.5 27 60 26.8 57.3 C26.5 56.4 26 56.4 25.5 57 Z",
            center = Offset(23f, 69f), radius = 8f,
        ),
        MusclePath( // rectus abdominis column; the mirror gap = linea alba
            "Abs",
            "M48.9 47.5 C44.6 48 42 49.2 41.6 51.6 L41.6 76 C41.8 79.5 43.5 82 46 83 " +
                "C47.8 83.6 48.9 83.6 49.1 83.6 L49.1 47.5 Z",
            center = Offset(45.5f, 64f), radius = 11f,
        ),
        MusclePath( // external oblique flanking the rectus
            "Abs",
            "M41 50.5 C39 51.5 37.6 53.5 37.1 57 C36.7 61.5 37.1 66.5 38.3 70.5 " +
                "C39.1 73.5 40.3 76 41.1 77 L41.1 51 Z",
            center = Offset(39f, 61f), radius = 8f,
        ),
        MusclePath( // vastus lateralis — the outer quad sweep
            "Quads",
            "M37.2 82.5 C35.7 88 35.2 95 35.7 101 C36.2 107 37.2 112.5 38.7 116.5 " +
                "C39.5 114.5 40 108.5 40.1 101.5 C40.3 94.5 39.9 87.5 39 83 " +
                "C38.4 82 37.7 82 37.2 82.5 Z",
            center = Offset(37.7f, 98f), radius = 11f,
        ),
        MusclePath( // rectus femoris — the central quad
            "Quads",
            "M40.6 82.5 C39.9 88 39.9 95 40.3 102 C40.6 108 41.3 113 42.3 116.5 " +
                "C43.3 113 43.9 107 44.1 100 C44.3 93 43.9 86 43.1 82.5 " +
                "C42.3 81.5 41.3 81.8 40.6 82.5 Z",
            center = Offset(42f, 99f), radius = 10f,
        ),
        MusclePath( // vastus medialis — the teardrop above the inner knee
            "Quads",
            "M44.2 100 C44.7 104 45.4 108 46.4 111.5 C47.2 114 48 115.5 48.7 116 " +
                "C49 112 48.8 106 48.2 101 C47.5 99 45.7 99 44.2 100 Z",
            center = Offset(46.6f, 108f), radius = 7f,
        ),
        MusclePath( // tibialis / lower-leg front
            "Calves",
            "M38 123.5 C37 130 36.8 138 37.5 145 C38.3 151 39.6 155 41.5 157 " +
                "C43.3 155 44.7 151 45.5 145 C46.3 138 46.2 130.5 45.5 124.5 " +
                "C43 122.5 40 122.5 38 123.5 Z",
            center = Offset(41.5f, 139f), radius = 13f,
        ),
    )
}

// Striation / separation lines — stroked in background ink for anatomical definition
private val FrontDetails: List<Path> by lazy {
    val pecAndDelt = path(
        "M35 32.8 C39 31.2 44 30.9 48.6 31.6 " +          // clavicular pec border
            "M34.7 33 C34.2 37 34.8 41 36.1 43.7 " +      // front-delt / pec seam
            "M35.6 45.6 L37.4 48.3 M36.5 49.6 L38.3 52.3 M37.4 53.6 L39.1 56.2" // serratus fingers
    )
    val abCuts = path(
        "M41.8 57.5 C44 58.2 47.2 58.4 49.2 58.2 " +
            "M41.8 63.8 C44 64.5 47.2 64.7 49.2 64.5 " +
            "M41.8 70.2 C44 70.9 47.2 71.1 49.2 70.9"
    )
    val shin = path("M42 124.5 C41.6 134 41.4 145 41.7 155")
    listOf(pecAndDelt, pecAndDelt.mirrored(), abCuts, abCuts.mirrored(), shin, shin.mirrored())
}

// --- Back muscles ---------------------------------------------------------------
private val BackMuscles: List<MusclePath> by lazy {
    listOf(
        MusclePath( // trapezius diamond, neck to mid-spine
            "Back",
            "M49.5 18.5 C46 20 41 22.5 37.5 25.5 C40.5 27 43.5 29.5 45.5 33 " +
                "C47.5 37 49 42.5 49.5 47.5 Z",
            center = Offset(44f, 32f), radius = 13f,
        ),
        MusclePath( // posterior deltoid
            "Shoulders",
            "M33.6 26 C29.2 27 25.9 30.6 25.4 35.2 C25.2 38.4 26.4 40.6 28.6 41.2 " +
                "C31.6 41.4 33.9 38.6 34.6 34.4 C35.1 30.8 34.9 27.6 33.6 26 Z",
            center = Offset(29.5f, 33f), radius = 9f,
        ),
        MusclePath( // triceps horseshoe
            "Triceps",
            "M26.5 42 C25 46.5 24.5 51.5 25 55.5 C26 58 28.5 58.2 30 56 " +
                "C31 51.8 31.3 47 30.5 43 C29.3 41 27.5 41.3 26.5 42 Z",
            center = Offset(28f, 49f), radius = 8f,
        ),
        MusclePath( // forearm extensors
            "Forearms",
            "M25.5 57 C24 61 22.5 66 21.6 71 C20.9 75.5 20.6 79.5 20.9 82.5 " +
                "C21.7 84 23.1 83.8 23.9 82 C24.8 78 25.4 73.5 26 69 " +
                "C26.6 64.5 27 60 26.8 57.3 C26.5 56.4 26 56.4 25.5 57 Z",
            center = Offset(23f, 69f), radius = 8f,
        ),
        MusclePath( // teres / infraspinatus, below traps by the armpit
            "Back",
            "M34.5 37.5 C32.8 38.2 31.8 39.8 32 41.8 C32.5 43.8 34.5 44.8 36.5 44 " +
                "C38 42.8 38.3 40.5 37.6 38.5 C37 37.5 35.5 37.2 34.5 37.5 Z",
            center = Offset(35.5f, 41f), radius = 6f,
        ),
        MusclePath( // latissimus dorsi — the V-taper
            "Back",
            "M34.6 44 C33.6 48 34 53 35.4 57.5 C37 63 40 67.5 44 71 " +
                "C46.3 72.8 48.5 73.5 49.6 73.8 L49.6 57 C49.6 52 48 47.5 45 45 " +
                "C41.5 43 37.6 43 34.6 44 Z",
            center = Offset(42f, 56f), radius = 14f,
        ),
        MusclePath( // erector spinae — lower back
            "Back",
            "M46 72 C44.5 75 44 79 44.3 83 C45.5 85.5 47.8 86.5 49.6 86.7 " +
                "L49.6 72.5 C48.3 72.5 47 72.3 46 72 Z",
            center = Offset(47f, 78f), radius = 8f,
        ),
        MusclePath( // gluteus maximus
            "Glutes",
            "M43 85 C39.5 86 37.3 89 37.2 93.5 C37.1 98 39 101.5 42.5 103 " +
                "C46 104.3 48.9 103 49.6 99.5 C49.9 95 49.8 90 49.4 86.5 " +
                "C47.5 85 45 84.6 43 85 Z",
            center = Offset(43f, 93f), radius = 13f,
        ),
        MusclePath( // biceps femoris — outer hamstring
            "Hamstrings",
            "M38 105 C37 111 36.9 117 37.5 122 C38.2 126 39.5 128.5 41 129.5 " +
                "C41.5 125 41.7 118 41.5 111 C41.3 107 40.8 105 40 104.5 " +
                "C39 104 38.3 104.3 38 105 Z",
            center = Offset(39.5f, 114f), radius = 10f,
        ),
        MusclePath( // semitendinosus — inner hamstring
            "Hamstrings",
            "M42 105 C42.3 111 42.7 117 43.5 122 C44 125.5 45 128 46.3 128 " +
                "C47 123 47 116 46.5 109 C46.2 106 45.5 104.5 44.3 104.3 " +
                "C43.3 104.3 42.3 104.3 42 105 Z",
            center = Offset(44.5f, 114f), radius = 9f,
        ),
        MusclePath( // gastrocnemius lateral head
            "Calves",
            "M38.3 131 C37.6 136 37.7 141.5 38.6 146.5 C39.4 150.5 40.5 153 41.5 154 " +
                "C42 149 42.2 142 42 136 C41.8 132 41 130 40 130 C39 130 38.5 130.3 38.3 131 Z",
            center = Offset(39.7f, 141f), radius = 9f,
        ),
        MusclePath( // gastrocnemius medial head
            "Calves",
            "M42 131 C42.2 137 42.5 143 43.5 148 C44.2 151.5 45.2 153.5 46 153 " +
                "C46.8 148 46.8 141 46.2 135 C45.9 132 45 130.3 43.8 130.3 " +
                "C43 130.3 42.3 130.3 42 131 Z",
            center = Offset(44.3f, 140f), radius = 9f,
        ),
        MusclePath( // soleus — lower calf toward the ankle
            "Calves",
            "M39.5 150 C39 154 39.2 158 40 160.5 C40.6 162.3 41.3 163 42 163 " +
                "C42.7 162.3 43.4 160.5 44 158 C44.6 155 44.5 151.5 43.8 149.5 " +
                "C42 149.5 40.5 149.5 39.5 150 Z",
            center = Offset(42f, 156f), radius = 6f,
        ),
    )
}

private val BackDetails: List<Path> by lazy {
    val upper = path(
        "M49.2 20 C46.7 27 45.2 34 45.4 42 " +           // trapezius lateral border
            "M35.2 46 C37 53 40.2 61 44.6 68 " +          // lat outer border (the V)
            "M26.9 44.5 C28.2 48.5 28.4 52.5 27.6 56.5"   // tricep horseshoe
    )
    val spine = path("M49.4 47 L49.4 71")                  // spinal groove edge
    val fold = path("M38 104.5 C41 103.5 45 103.5 47.6 105") // gluteal fold
    listOf(upper, upper.mirrored(), spine, spine.mirrored(), fold, fold.mirrored())
}

/**
 * Recovery ("Body") hero: front + back physique, every muscle tinted by ember readiness
 * and sculpted with a soft sheen. Fills in once on entry. Exact numbers belong to the
 * BY MUSCLE list; the body reads by colour against the READY→SPENT legend.
 */
@Composable
fun MuscleBodyMap(freshness: Map<String, Int>, modifier: Modifier = Modifier) {
    val base = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    val baseHi = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.11f)
    val noData = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.13f)
    val ink = MaterialTheme.colorScheme.background
    val hint = GymTheme.colors.hint
    val heatScale = GymTheme.colors.heat

    // §10: the physique settles in once when the screen opens (never on tab return)
    val lit = remember { Animatable(0f) }
    LaunchedEffect(Unit) { lit.animateTo(1f, animationSpec = Motion.settle(Motion.SLOW)) }

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
                    details = if (muscles === FrontMuscles) FrontDetails else BackDetails,
                    slotX = slotX, slotW = slotW,
                    base = base, baseHi = baseHi, noData = noData, ink = ink,
                    litProgress = lit.value,
                    tintFor = { m -> freshness[m]?.let { heatScale.at(it / 100f) } },
                    emphasisFor = { 1f },
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
 * Exercise figure: the ordered muscle list drives PRIMARY (first) at full indigo and
 * SECONDARY (rest) dimmer. The highlight springs in whenever the target changes, and the
 * primary movers breathe gently so the eye lands on the working muscle. Works offline for
 * every exercise, including custom ones.
 */
@Composable
fun MuscleTargetFigure(muscles: List<String>, modifier: Modifier = Modifier) {
    val base = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    val baseHi = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.11f)
    val cold = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val hot = MaterialTheme.colorScheme.primary
    val ink = MaterialTheme.colorScheme.background

    val primary = muscles.firstOrNull()
    val secondary = muscles.drop(1).toSet() - setOfNotNull(primary)

    // fill-in that replays on every target change (spring settle, §7)
    val lit = remember { Animatable(0f) }
    LaunchedEffect(muscles) {
        lit.snapTo(0f)
        lit.animateTo(1f, animationSpec = Motion.settle(Motion.SLOW))
    }
    // ambient breath on the primary mover (temper curve; ambient only, §7.3)
    val breath = rememberInfiniteTransition(label = "targetBreath")
    val pulse by breath.animateFloat(
        initialValue = 0.74f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_500, easing = Motion.Temper), RepeatMode.Reverse),
        label = "targetPulse",
    )

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
                details = if (group === FrontMuscles) FrontDetails else BackDetails,
                slotX = slotX, slotW = slotW,
                base = base, baseHi = baseHi, noData = cold, ink = ink,
                litProgress = lit.value,
                tintFor = { m -> if (m == primary || m in secondary) hot else null },
                emphasisFor = { m ->
                    when {
                        m == primary -> pulse                    // full + breathing
                        m in secondary -> 0.42f                  // clearly dimmer
                        else -> 0f
                    }
                },
                glowFor = { m ->
                    when {
                        m == primary -> pulse
                        m in secondary -> 0.28f
                        else -> 0f
                    }
                },
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
    baseHi: Color,
    noData: Color,
    ink: Color,
    litProgress: Float,
    tintFor: (String) -> Color?,
    emphasisFor: (String) -> Float,
    glowFor: (String) -> Float,
) {
    val s = min(slotW / BODY_W, size.height / BODY_H)
    val dx = slotX + (slotW - BODY_W * s) / 2f
    val dy = (size.height - BODY_H * s) / 2f
    withTransform({
        translate(dx, dy)
        scale(s, s, pivot = Offset.Zero)
    }) {
        // sculpted base: a faint top-lit gradient gives the blank physique volume
        drawPath(
            Silhouette,
            Brush.verticalGradient(listOf(baseHi, base), startY = 0f, endY = BODY_H),
        )
        muscles.forEach { m ->
            val tint = tintFor(m.muscle)
            val emph = (if (tint != null) emphasisFor(m.muscle) else 0f) * litProgress
            val glow = (if (tint != null) glowFor(m.muscle) else 0f) * litProgress
            m.paths.forEachIndexed { i, p ->
                if (tint == null || emph <= 0.02f) {
                    drawPath(p, noData)
                } else {
                    if (glow > 0f) {
                        // radiating halo: two widening translucent strokes
                        drawPath(
                            p, tint, alpha = 0.16f * glow,
                            style = Stroke(width = 6f, join = StrokeJoin.Round),
                        )
                        drawPath(
                            p, tint, alpha = 0.30f * glow,
                            style = Stroke(width = 2.5f, join = StrokeJoin.Round),
                        )
                    }
                    // flat belly fill
                    drawPath(p, tint, alpha = 0.95f * emph)
                    // sculpt: a soft radial sheen at the belly centre = 3-D pop
                    drawPath(
                        p,
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.34f * emph),
                                Color.Transparent,
                            ),
                            center = m.centers[i],
                            radius = m.radius,
                        ),
                    )
                    // ink seam so adjacent hot muscles stay individually readable
                    drawPath(p, ink, alpha = 0.35f * emph, style = Stroke(width = 0.5f))
                }
            }
        }
        // always-on striations: anatomical definition, subtle on the resting figure
        details.forEach {
            drawPath(
                it, ink, alpha = 0.4f,
                style = Stroke(width = 0.7f, cap = StrokeCap.Round),
            )
        }
    }
}
