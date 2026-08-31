// Purpose: Compose-Canvas chart kit for analytics — smooth line chart with PR highlight,
//          weekly bar chart, GitHub-style calendar heatmap (glass-styled, theme-aware)
// Inputs: series from AnalyticsEngine
// Outputs: pure visualizations (no interaction yet)
package com.gymtracker.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtracker.domain.AnalyticsEngine
import com.gymtracker.ui.theme.GymTheme
import com.gymtracker.ui.theme.Motion
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFmt = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

private fun fmtTime(ms: Long): String =
    Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFmt)

private fun fmtValue(v: Double): String =
    if (v >= 1000) "%.1fk".format(v / 1000) else if (v % 1.0 == 0.0) v.toInt().toString() else "%.1f".format(v)

/** Smooth line chart with gradient fill; the all-time max point is highlighted in gold. */
@Composable
fun LineChart(
    points: List<AnalyticsEngine.Point>,
    modifier: Modifier = Modifier,
    valueSuffix: String = "",
) {
    val measurer = rememberTextMeasurer()
    val lineColor = MaterialTheme.colorScheme.primary
    val gold = GymTheme.colors.prGold
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val labelStyle = TextStyle(fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

    // Forged Motion §10: the data tempers in left→right, led by the Hot Tip — a glow at
    // the leading edge, like drawn molten metal. Axes/labels are already there; only the
    // data moves. Plays once per screen entry (rememberSaveable), never on tab return.
    var played by rememberSaveable { mutableStateOf(false) }
    val draw by animateFloatAsState(
        targetValue = if (played) 1f else 0f,
        animationSpec = Motion.settle(Motion.SLOW),
        label = "chartTemper",
    )
    LaunchedEffect(Unit) { played = true }

    Canvas(modifier.fillMaxWidth().height(190.dp)) {
        if (points.size < 2) return@Canvas
        val insetTop = 18.dp.toPx()
        val insetBottom = 22.dp.toPx()
        val insetH = 6.dp.toPx()
        val chartW = size.width - insetH * 2
        val chartH = size.height - insetTop - insetBottom

        val minV = points.minOf { it.value }
        val maxV = points.maxOf { it.value }
        val pad = ((maxV - minV) * 0.10).coerceAtLeast(1.0)
        val lo = minV - pad
        val hi = maxV + pad
        val minT = points.first().time
        val maxT = points.last().time
        val spanT = (maxT - minT).coerceAtLeast(1)

        fun x(t: Long) = insetH + chartW * (t - minT) / spanT.toFloat()
        fun y(v: Double) = insetTop + chartH * (1f - ((v - lo) / (hi - lo)).toFloat())

        // gridlines + y labels
        listOf(hi - pad, (hi + lo) / 2, lo + pad).forEach { v ->
            val yy = y(v)
            drawLine(gridColor, Offset(insetH, yy), Offset(insetH + chartW, yy), strokeWidth = 1.dp.toPx())
            drawText(
                measurer.measure(AnnotatedString(fmtValue(v) + valueSuffix), labelStyle),
                topLeft = Offset(insetH, yy - 12.dp.toPx()),
            )
        }

        val coords = points.map { Offset(x(it.time), y(it.value)) }
        val line = Path().apply {
            moveTo(coords.first().x, coords.first().y)
            for (i in 1 until coords.size) {
                val prev = coords[i - 1]
                val cur = coords[i]
                val midX = (prev.x + cur.x) / 2f
                cubicTo(midX, prev.y, midX, cur.y, cur.x, cur.y)
            }
        }
        val fill = Path().apply {
            addPath(line)
            lineTo(coords.last().x, insetTop + chartH)
            lineTo(coords.first().x, insetTop + chartH)
            close()
        }
        val clipX = insetH + chartW * draw
        clipRect(right = clipX) {
            drawPath(
                fill,
                Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.30f), Color.Transparent),
                    startY = insetTop,
                    endY = insetTop + chartH,
                ),
            )
            drawPath(line, lineColor, style = Stroke(width = 2.5.dp.toPx()))
        }

        // dots — gold on the all-time max; they appear as the tip passes them
        val maxValue = points.maxOf { it.value }
        points.forEachIndexed { i, p ->
            if (coords[i].x > clipX) return@forEachIndexed
            val isMax = p.value == maxValue
            drawCircle(
                color = if (isMax) gold else lineColor,
                radius = (if (isMax) 4.5.dp else 2.5.dp).toPx(),
                center = coords[i],
            )
        }

        // the Hot Tip: white-gold glow leading the draw, cooling as it lands
        if (draw > 0.01f && draw < 0.995f) {
            val seg = coords.indexOfLast { it.x <= clipX }.coerceAtLeast(0)
            val tip = if (seg >= coords.lastIndex) coords.last() else {
                val a = coords[seg]
                val b = coords[seg + 1]
                val t = ((clipX - a.x) / (b.x - a.x)).coerceIn(0f, 1f)
                Offset(clipX, a.y + (b.y - a.y) * t)
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.9f),
                        gold.copy(alpha = 0.45f),
                        Color.Transparent,
                    ),
                    center = tip,
                    radius = 13.dp.toPx(),
                ),
                radius = 13.dp.toPx(),
                center = tip,
            )
        }

        // x labels: first + last date
        drawText(
            measurer.measure(AnnotatedString(fmtTime(minT)), labelStyle),
            topLeft = Offset(insetH, size.height - 14.dp.toPx()),
        )
        val lastLabel = measurer.measure(AnnotatedString(fmtTime(maxT)), labelStyle)
        drawText(
            lastLabel,
            topLeft = Offset(insetH + chartW - lastLabel.size.width, size.height - 14.dp.toPx()),
        )
    }
}

/** Weekly bar chart with a dashed 4-week-average line. */
@Composable
fun WeeklyBarChart(
    weeks: List<Pair<LocalDate, Double>>,
    // AnalyticsEngine.weeklyVolume always returns a fixed-length, zero-padded list — weeks.size
    // is constant regardless of how long the account has existed, so it can never gate "do we
    // have 4 real weeks of history" (verified live: a 1-week-old account still drew this line,
    // averaged over 3 zero-padded weeks it never trained in). The caller must compute this from
    // the actual earliest-workout date instead.
    hasFourWeeksHistory: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    val avgColor = MaterialTheme.colorScheme.secondary
    val barColor = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val labelStyle = TextStyle(fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

    // bars rise with a light stagger, once per screen entry (Forged Motion §10)
    var played by rememberSaveable { mutableStateOf(false) }
    val rise by animateFloatAsState(
        targetValue = if (played) 1f else 0f,
        animationSpec = Motion.settle(Motion.SLOW),
        label = "barsRise",
    )
    LaunchedEffect(Unit) { played = true }

    Canvas(modifier.fillMaxWidth().height(170.dp)) {
        if (weeks.isEmpty()) return@Canvas
        val insetTop = 18.dp.toPx()
        val insetBottom = 22.dp.toPx()
        val insetH = 6.dp.toPx()
        val chartW = size.width - insetH * 2
        val chartH = size.height - insetTop - insetBottom
        val maxV = weeks.maxOf { it.second }.coerceAtLeast(1.0)

        val slot = chartW / weeks.size
        val barW = slot * 0.55f
        val stagger = 0.5f / weeks.size.coerceAtLeast(1)
        weeks.forEachIndexed { i, (_, v) ->
            val barT = ((rise - i * stagger) / 0.5f).coerceIn(0f, 1f)
            val h = (chartH * (v / maxV)).toFloat() * barT
            // Handoff README §7: the bars are quiet history and ONLY the current week is
            // accent-filled. Volume is not a temperature, so it gets no heat ramp and no
            // intensity ladder — the eye goes to "now", then reads the shape around it.
            val isCurrent = i == weeks.lastIndex
            val tone = if (isCurrent) barColor else onSurface.copy(alpha = 0.26f)
            if (h > 0f) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(tone, tone.copy(alpha = tone.alpha * 0.55f)),
                        startY = insetTop + chartH - h,
                        endY = insetTop + chartH,
                    ),
                    topLeft = Offset(insetH + slot * i + (slot - barW) / 2f, insetTop + chartH - h),
                    size = Size(barW, h),
                    cornerRadius = CornerRadius(barW * 0.35f),
                )
            } else if (barT > 0f) {
                // a week with 0 volume is still a week — a bare baseline dot says "quiet",
                // not "missing data" (an empty slot with nothing else on the axis read as broken)
                drawCircle(
                    color = onSurface.copy(alpha = 0.14f),
                    radius = barW * 0.14f,
                    center = Offset(insetH + slot * i + slot / 2f, insetTop + chartH),
                )
            }
        }

        // 4-week average reference line — fades in after the bars have risen. Only draws
        // once 4 real weeks of history exist; under that, "4-week average" would silently
        // be an average of however many weeks exist (identical to the single bar itself
        // for a 1-week-old account) while still being labeled and drawn as if it were one.
        val avg = if (hasFourWeeksHistory) weeks.takeLast(4).map { it.second }.average() else 0.0
        if (avg > 0) {
            val yy = insetTop + chartH * (1f - (avg / maxV).toFloat())
            drawLine(
                avgColor.copy(alpha = avgColor.alpha * ((rise - 0.6f) / 0.4f).coerceIn(0f, 1f)),
                Offset(insetH, yy),
                Offset(insetH + chartW, yy),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
            )
        }

        drawText(
            measurer.measure(AnnotatedString("max " + fmtValue(maxV) + " kg"), labelStyle),
            topLeft = Offset(insetH, 2.dp.toPx()),
        )
        drawText(
            measurer.measure(AnnotatedString(weeks.first().first.format(dateFmt)), labelStyle),
            topLeft = Offset(insetH, size.height - 14.dp.toPx()),
        )
        val lastLabel = measurer.measure(AnnotatedString("this week"), labelStyle)
        drawText(
            lastLabel,
            topLeft = Offset(insetH + chartW - lastLabel.size.width, size.height - 14.dp.toPx()),
        )
    }
}

/**
 * Linear Hot Tip bar (§7.4): the fill is led by a small molten glow and fills once per
 * screen entry via `settle`/`slow`. Used by Recovery's per-muscle freshness rows.
 */
@Composable
fun ForgedBar(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    var played by rememberSaveable { mutableStateOf(false) }
    val target = progress.coerceIn(0f, 1f)
    val p by animateFloatAsState(
        targetValue = if (played) target else 0f,
        animationSpec = Motion.settle(Motion.SLOW),
        label = "barFill",
    )
    LaunchedEffect(Unit) { played = true }
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    Canvas(modifier.height(6.dp)) {
        val r = CornerRadius(size.height / 2f)
        drawRoundRect(track, cornerRadius = r)
        val w = size.width * p
        if (w > 0f) {
            drawRoundRect(color, size = Size(w, size.height), cornerRadius = r)
            if (p < target - 0.01f && p > 0.02f) {
                val tip = Offset(w, size.height / 2f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.8f),
                            color.copy(alpha = 0.4f),
                            Color.Transparent,
                        ),
                        center = tip,
                        radius = 7.dp.toPx(),
                    ),
                    radius = 7.dp.toPx(),
                    center = tip,
                )
            }
        }
    }
}

/** GitHub-style training calendar: columns = weeks, rows = Mon..Sun, tint = daily volume. */
@Composable
fun CalendarHeatmap(
    dayVolume: Map<LocalDate, Double>,
    modifier: Modifier = Modifier,
    weeks: Int = 20,
) {
    // the calendar encodes training intensity, so it reads from the heat scale rather than the
    // action colour — under Chalk & Iron the chrome is colourless and heat is the data channel
    val active = GymTheme.colors.heat.hot
    val empty = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    val todayOutline = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)

    // quartile thresholds over the non-zero days → 4 intensity levels
    val nonZero = dayVolume.values.filter { it > 0 }.sorted()
    fun level(v: Double): Float {
        if (nonZero.isEmpty() || v <= 0) return 0f
        // 1-based inclusive count, not the 0-based indexOfLast this replaced — the max
        // element must reach q=1.0 (full intensity) regardless of how many days exist.
        // indexOfLast capped the brightest cell at (N-1)/N, worst for a brand-new account
        // whose first (and only, and maximum) training day rendered at the dimmest tier.
        val q = nonZero.count { it <= v }.toFloat() / nonZero.size
        return 0.30f + 0.70f * q
    }

    Canvas(modifier.fillMaxWidth().height(132.dp)) {
        val gap = 3.dp.toPx()
        val cell = ((size.width - gap * (weeks - 1)) / weeks)
            .coerceAtMost((size.height - gap * 6) / 7)
        val today = LocalDate.now()
        val thisMonday = today.with(java.time.DayOfWeek.MONDAY)
        for (col in 0 until weeks) {
            val monday = thisMonday.minusWeeks((weeks - 1 - col).toLong())
            for (row in 0 until 7) {
                val day = monday.plusDays(row.toLong())
                if (day.isAfter(today)) continue
                val v = dayVolume[day] ?: 0.0
                val lv = level(v)
                val topLeft = Offset(col * (cell + gap), row * (cell + gap))
                drawRoundRect(
                    color = if (lv == 0f) empty else active.copy(alpha = lv),
                    topLeft = topLeft,
                    size = Size(cell, cell),
                    cornerRadius = CornerRadius(cell * 0.28f),
                )
                if (day == today) {
                    drawRoundRect(
                        color = todayOutline,
                        topLeft = topLeft,
                        size = Size(cell, cell),
                        cornerRadius = CornerRadius(cell * 0.28f),
                        style = Stroke(width = 1.5.dp.toPx()),
                    )
                }
            }
        }
    }
}
