// Purpose: Fitbod-style body map — front + back stylized figures with muscle regions
//          tinted by recovery freshness and % pills on trained muscles
// Inputs: freshness map (canonical muscle name → percent 0..100)
// Outputs: none (pure visualization)
package com.gymtracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtracker.ui.theme.GymTheme

// Region in body-local unit coordinates (x: 0..1 of figure width, y: 0..1 of figure height)
private class Region(
    val muscle: String,
    val x0: Float, val y0: Float, val x1: Float, val y1: Float,
    val oval: Boolean = false,
)

private val silhouetteParts = listOf(
    Region("", 0.42f, 0.010f, 0.58f, 0.115f, oval = true),  // head
    Region("", 0.17f, 0.135f, 0.83f, 0.215f),               // shoulder girdle
    Region("", 0.30f, 0.130f, 0.70f, 0.465f),               // torso
    Region("", 0.165f, 0.145f, 0.290f, 0.445f),             // left arm
    Region("", 0.710f, 0.145f, 0.835f, 0.445f),             // right arm
    Region("", 0.320f, 0.440f, 0.680f, 0.535f),             // hips
    Region("", 0.325f, 0.510f, 0.485f, 0.950f),             // left leg
    Region("", 0.515f, 0.510f, 0.675f, 0.950f),             // right leg
)

private val frontRegions = listOf(
    Region("Shoulders", 0.185f, 0.140f, 0.325f, 0.210f, oval = true),
    Region("Shoulders", 0.675f, 0.140f, 0.815f, 0.210f, oval = true),
    Region("Chest", 0.335f, 0.165f, 0.495f, 0.260f),
    Region("Chest", 0.505f, 0.165f, 0.665f, 0.260f),
    Region("Biceps", 0.180f, 0.225f, 0.285f, 0.340f),
    Region("Biceps", 0.715f, 0.225f, 0.820f, 0.340f),
    Region("Abs", 0.395f, 0.285f, 0.605f, 0.445f),
    Region("Quads", 0.335f, 0.525f, 0.480f, 0.705f),
    Region("Quads", 0.520f, 0.525f, 0.665f, 0.705f),
)

private val backRegions = listOf(
    Region("Back", 0.335f, 0.155f, 0.665f, 0.345f),
    Region("Triceps", 0.180f, 0.225f, 0.285f, 0.340f),
    Region("Triceps", 0.715f, 0.225f, 0.820f, 0.340f),
    Region("Glutes", 0.350f, 0.440f, 0.650f, 0.530f),
    Region("Hamstrings", 0.335f, 0.550f, 0.480f, 0.715f),
    Region("Hamstrings", 0.520f, 0.550f, 0.665f, 0.715f),
    Region("Calves", 0.345f, 0.755f, 0.478f, 0.895f),
    Region("Calves", 0.522f, 0.755f, 0.655f, 0.895f),
)

/**
 * Anatomical target figure for one exercise — front + back body with the exercise's
 * muscles highlighted hot (training.fit-style illustration, drawn in Forged's own
 * language so it works offline for every exercise, including custom ones).
 */
@Composable
fun MuscleTargetFigure(muscles: List<String>, modifier: Modifier = Modifier) {
    val silhouette = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val inactive = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.13f)
    val hot = Color(0xFFE8402A)          // struck-metal red, like the reference charts
    val targets = muscles.toSet()

    Canvas(
        modifier
            .fillMaxWidth()
            .height(210.dp)
    ) {
        val figureWidth = size.width * 0.44f
        listOf(
            frontRegions to Offset(size.width * 0.03f, 0f),
            backRegions to Offset(size.width * 0.53f, 0f),
        ).forEach { (regions, origin) ->
            drawTargetFigure(
                regions = regions, origin = origin,
                w = figureWidth, h = size.height,
                targets = targets, silhouette = silhouette,
                inactive = inactive, hot = hot,
            )
        }
    }
}

private fun DrawScope.drawTargetFigure(
    regions: List<Region>,
    origin: Offset,
    w: Float,
    h: Float,
    targets: Set<String>,
    silhouette: Color,
    inactive: Color,
    hot: Color,
) {
    fun draw(r: Region, color: Color) {
        val topLeft = Offset(origin.x + r.x0 * w, origin.y + r.y0 * h)
        val size = Size((r.x1 - r.x0) * w, (r.y1 - r.y0) * h)
        if (r.oval) drawOval(color, topLeft, size)
        else drawRoundRect(color, topLeft, size, CornerRadius(size.minDimension * 0.45f))
    }
    silhouetteParts.forEach { draw(it, silhouette) }
    regions.forEach { region ->
        if (region.muscle in targets) {
            // hot core + a soft heat halo so the target reads instantly
            val topLeft = Offset(origin.x + region.x0 * w, origin.y + region.y0 * h)
            val sz = Size((region.x1 - region.x0) * w, (region.y1 - region.y0) * h)
            drawRoundRect(
                color = hot.copy(alpha = 0.35f),
                topLeft = Offset(topLeft.x - 3.dp.toPx(), topLeft.y - 3.dp.toPx()),
                size = Size(sz.width + 6.dp.toPx(), sz.height + 6.dp.toPx()),
                cornerRadius = CornerRadius(sz.minDimension * 0.5f),
            )
            draw(region, hot.copy(alpha = 0.92f))
        } else {
            draw(region, inactive)
        }
    }
}

@Composable
fun MuscleBodyMap(freshness: Map<String, Int>, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val silhouette = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val noData = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
    val success = GymTheme.colors.success
    val gold = GymTheme.colors.prGold
    val danger = MaterialTheme.colorScheme.error
    val labelStyle = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color.White)

    fun freshColor(pct: Int): Color = when {
        pct >= 80 -> success
        pct >= 40 -> gold
        else -> danger
    }

    Canvas(
        modifier
            .fillMaxWidth()
            .height(290.dp)
    ) {
        val figureWidth = size.width * 0.44f
        drawFigure(
            regions = frontRegions, origin = Offset(size.width * 0.03f, 0f),
            w = figureWidth, h = size.height,
            freshness = freshness, silhouette = silhouette, noData = noData,
            freshColor = ::freshColor, measurer = measurer, labelStyle = labelStyle,
        )
        drawFigure(
            regions = backRegions, origin = Offset(size.width * 0.53f, 0f),
            w = figureWidth, h = size.height,
            freshness = freshness, silhouette = silhouette, noData = noData,
            freshColor = ::freshColor, measurer = measurer, labelStyle = labelStyle,
        )
    }
}

private fun DrawScope.drawFigure(
    regions: List<Region>,
    origin: Offset,
    w: Float,
    h: Float,
    freshness: Map<String, Int>,
    silhouette: Color,
    noData: Color,
    freshColor: (Int) -> Color,
    measurer: TextMeasurer,
    labelStyle: TextStyle,
) {
    fun rectOf(r: Region): Pair<Offset, Size> {
        val topLeft = Offset(origin.x + r.x0 * w, origin.y + r.y0 * h)
        val size = Size((r.x1 - r.x0) * w, (r.y1 - r.y0) * h)
        return topLeft to size
    }

    fun draw(r: Region, color: Color) {
        val (topLeft, size) = rectOf(r)
        if (r.oval) {
            drawOval(color, topLeft, size)
        } else {
            drawRoundRect(color, topLeft, size, CornerRadius(size.minDimension * 0.45f))
        }
    }

    silhouetteParts.forEach { draw(it, silhouette) }
    regions.forEach { region ->
        val pct = freshness[region.muscle]
        draw(region, if (pct != null) freshColor(pct).copy(alpha = 0.80f) else noData)
    }

    // one % pill per muscle with data, anchored to that muscle's first region
    regions.distinctBy { it.muscle }.forEach { region ->
        val pct = freshness[region.muscle] ?: return@forEach
        val (topLeft, size) = rectOf(region)
        val center = Offset(topLeft.x + size.width / 2f, topLeft.y + size.height / 2f)
        val layout = measurer.measure(AnnotatedString("$pct%"), labelStyle)
        val padH = 5.dp.toPx()
        val padV = 2.dp.toPx()
        val pillW = layout.size.width + padH * 2
        val pillH = layout.size.height + padV * 2
        val pillTopLeft = Offset(center.x - pillW / 2f, center.y - pillH / 2f)
        drawRoundRect(
            color = freshColor(pct),
            topLeft = pillTopLeft,
            size = Size(pillW, pillH),
            cornerRadius = CornerRadius(pillH / 2f),
        )
        drawText(layout, topLeft = Offset(pillTopLeft.x + padH, pillTopLeft.y + padV))
    }
}
