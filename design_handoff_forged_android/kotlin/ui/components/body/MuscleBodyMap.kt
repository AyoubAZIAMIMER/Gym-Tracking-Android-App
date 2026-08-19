// Purpose: Recovery body map — front / back silhouettes with per-muscle heat tint.
//          Path data lives in MuscleBodyPaths.kt (generated). Heat is data: a muscle's colour
//          comes ONLY from GymTheme.colors.heat.at(freshness), never a hand-picked hex.
//          Per IDENTITY_V5 §2: no % pills on the body — the numbers live in the BY MUSCLE list.
// Inputs: freshness map (slug -> 0f just worked .. 1f fully recovered)
// Outputs: MuscleBodyMap()
package com.gymtracker.ui.components.body

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

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
    val shapes = if (side == BodySide.Front) MuscleBodyPaths.Front else MuscleBodyPaths.Back
    val viewport = if (side == BodySide.Front) MuscleBodyPaths.FrontViewport else MuscleBodyPaths.BackViewport

    // Parse once per side; PathParser is not cheap and these are ~35 groups.
    val parsed = remember(side) {
        shapes.map { shape -> shape.slug to shape.paths.map { PathParser().parsePathString(it).toPath() } }
    }

    Canvas(
        modifier
            .fillMaxWidth()
            .aspectRatio(viewport.width / viewport.height)
    ) {
        val scale = size.width / viewport.width
        translate(-viewport.left * scale, -viewport.top * scale) {
            scale(scale) {
                parsed.forEach { (slug, paths) ->
                    val isMuscle = slug !in MuscleBodyPaths.Silhouette
                    val f = freshness[slug]
                    val fill = when {
                        !isMuscle -> silhouette
                        f == null -> silhouette
                        else -> heatAt(f)
                    }
                    paths.forEach { p ->
                        drawPath(p, color = fill)
                        drawPath(p, color = outline, style = Stroke(width = 1.2f))
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
 * Legend strip under the maps. v5 wording — COOLED · READY  ←→  GLOWING.
 * Never "FRESH / FATIGUED"; that copy is stale.
 */
@Composable
fun HeatLegend(
    heatAt: (Float) -> Color,
    modifier: Modifier = Modifier,
    steps: Int = 10,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(steps) { i ->
            val f = 1f - (i / (steps - 1f))
            Canvas(Modifier.weight(1f).aspectRatio(2.6f)) {
                drawRect(heatAt(f))
            }
        }
    }
}
