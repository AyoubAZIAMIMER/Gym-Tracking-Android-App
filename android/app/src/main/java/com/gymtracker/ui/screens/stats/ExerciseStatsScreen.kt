// Purpose: Per-exercise analytics page — e1RM + volume charts, plateau/overload/trend badges,
//          best-stats figures, recent session history (Progression's exercise stats, extended).
//          Restyled onto the redesign: one forgeHero, flat sections, stamped headings.
// Inputs: ExerciseStatsViewModel (route arg exerciseId)
// Outputs: onBack navigation
package com.gymtracker.ui.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gymtracker.domain.AnalyticsEngine
import com.gymtracker.ui.components.ForgedScreenTitle
import com.gymtracker.ui.components.ForgedSectionHeader
import com.gymtracker.ui.components.GlowBackground
import com.gymtracker.ui.components.LineChart
import com.gymtracker.ui.components.RowRule
import com.gymtracker.ui.components.SectionRule
import com.gymtracker.ui.components.StampText
import com.gymtracker.ui.components.forgeHero
import com.gymtracker.ui.theme.Dim
import com.gymtracker.ui.theme.FONT_FEATURE_TABULAR
import com.gymtracker.ui.theme.GymTheme
import com.gymtracker.utils.Formats
import com.gymtracker.utils.PlateCalculator
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFmt = DateTimeFormatter.ofPattern("d MMM yy", Locale.ENGLISH)

@Composable
fun ExerciseStatsScreen(
    onBack: () -> Unit = {},
    vm: ExerciseStatsViewModel = viewModel(),
) {
    val state by vm.ui.collectAsStateWithLifecycle()
    val analytics = state.analytics

    GlowBackground(glowAlpha = 0.10f) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding(),
        ) {
            ForgedScreenTitle(state.name, onBack = onBack)

            // THE hero for this screen (ForgedSurfaces §forgeHero): everything below stays flat
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dim.screenPadH)
                    .forgeHero()
                    .padding(18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.FitnessCenter,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    // the name already leads the screen title — the hero carries what it works
                    Column(Modifier.weight(1f)) {
                        StampText("TRAINS")
                        Text(
                            text = state.muscles.ifBlank { "No muscle assigned yet" },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(18.dp))

            if (analytics == null ||
                analytics.e1rmSeries.isEmpty() && analytics.recentSessions.isEmpty()
            ) {
                SectionRule()
                Text(
                    text = "No logged sets for this exercise yet.",
                    fontSize = 14.sp,
                    color = GymTheme.colors.hint,
                    modifier = Modifier.padding(horizontal = Dim.screenPadH, vertical = 18.dp),
                )
            } else {
                SectionRule()
                if (hasBadges(analytics)) {
                    BadgeRow(analytics)
                }

                ForgedSectionHeader("BESTS")
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = Dim.screenPadH, end = Dim.screenPadH, bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    HeadlineStat(
                        analytics.bestE1rm?.let { Formats.kg1(it) } ?: "–",
                        if (analytics.bestE1rm != null) "kg" else null,
                        "best e1RM",
                    )
                    HeadlineStat(
                        analytics.bestWeightKg?.let { Formats.kg1(it) } ?: "–",
                        if (analytics.bestWeightKg != null) "kg" else null,
                        "best weight",
                    )
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = Dim.screenPadH, end = Dim.screenPadH, bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    HeadlineStat("${analytics.totalSets}", null, "total sets")
                    HeadlineStat(Formats.volumeKg(analytics.totalVolumeKg), "kg", "total volume")
                }

                if (analytics.e1rmSeries.size >= 2) {
                    SectionRule()
                    ChartBlock("ESTIMATED 1RM", "best set per session · gold dot = all-time best") {
                        LineChart(analytics.e1rmSeries, valueSuffix = " kg")
                    }
                }
                if (analytics.volumeSeries.size >= 2) {
                    SectionRule()
                    ChartBlock("SESSION VOLUME", "working sets · weight × reps") {
                        LineChart(analytics.volumeSeries, valueSuffix = " kg")
                    }
                }

                if (analytics.recentSessions.isNotEmpty()) {
                    SectionRule()
                    ForgedSectionHeader("RECENT SESSIONS", bottomPadding = 4.dp)
                    analytics.recentSessions.forEach { s ->
                        RowRule()
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Dim.screenPadH, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = Instant.ofEpochMilli(s.time)
                                        .atZone(ZoneId.systemDefault()).toLocalDate().format(dateFmt),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "${s.sets} sets" +
                                        (s.workoutName.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = s.topWeightKg?.let {
                                        "${PlateCalculator.fmt(it)} kg × ${s.topReps ?: "–"}"
                                    } ?: "bodyweight",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontSize = 15.sp,
                                        fontFeatureSettings = FONT_FEATURE_TABULAR,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                s.bestE1rm?.let {
                                    Text(
                                        text = "e1RM ${PlateCalculator.fmt(it)}",
                                        fontSize = 11.5.sp,
                                        color = GymTheme.colors.hint,
                                        modifier = Modifier.padding(top = 2.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.navigationBarsPadding().height(Dim.listBottomSpacer))
        }
    }
}

private fun hasBadges(a: AnalyticsEngine.ExerciseAnalytics): Boolean =
    a.trendKgPerWeek != null || a.plateaued || a.overloadSuggestionKg != null

@Composable
private fun BadgeRow(analytics: AnalyticsEngine.ExerciseAnalytics) {
    // three badges do not fit a 412dp phone once one reads "READY: +2.5 kg" — scroll, never wrap
    Row(
        Modifier
            .horizontalScroll(rememberScrollState())
            .padding(top = 16.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Spacer(Modifier.width(Dim.screenPadH))
        analytics.trendKgPerWeek?.let { trend ->
            val positive = trend >= 0
            Badge(
                label = (if (positive) "▲ " else "▼ ") + "%.1f kg/wk".format(kotlin.math.abs(trend)),
                tint = if (positive) GymTheme.colors.success else MaterialTheme.colorScheme.error,
            )
        }
        if (analytics.plateaued) {
            Badge("PLATEAU · 4 sessions", MaterialTheme.colorScheme.error)
        }
        analytics.overloadSuggestionKg?.let {
            Badge("READY: +${PlateCalculator.fmt(it)} kg", GymTheme.colors.success)
        }
        Spacer(Modifier.width(Dim.screenPadH))
    }
}

@Composable
private fun Badge(label: String, tint: Color) {
    Box(
        Modifier
            .height(30.dp)
            .clip(RoundedCornerShape(50))
            .background(tint.copy(alpha = 0.16f))
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        StampText(label, color = tint)
    }
}

/** Anton figure with a small unit, over a muted caption (shared with History). */
@Composable
private fun HeadlineStat(value: String, unit: String?, caption: String) {
    Column {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 22.sp,
                    fontFeatureSettings = FONT_FEATURE_TABULAR,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (unit != null) {
                Text(
                    text = unit,
                    fontSize = 11.sp,
                    color = GymTheme.colors.hint,
                    modifier = Modifier.padding(start = 3.dp, bottom = 2.dp),
                )
            }
        }
        Text(
            text = caption,
            fontSize = 11.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 1.dp),
        )
    }
}

@Composable
private fun ChartBlock(title: String, subtitle: String, content: @Composable () -> Unit) {
    ForgedSectionHeader(title, bottomPadding = 2.dp)
    Text(
        text = subtitle,
        fontSize = 11.5.sp,
        color = GymTheme.colors.hint,
        modifier = Modifier.padding(start = Dim.screenPadH, end = Dim.screenPadH, bottom = 12.dp),
    )
    Box(Modifier.padding(start = Dim.screenPadH, end = Dim.screenPadH, bottom = 18.dp)) {
        content()
    }
}
