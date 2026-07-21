// Purpose: Per-exercise analytics page — e1RM + volume charts, plateau/overload/trend badges,
//          best-stats tiles, recent session history (Progression's exercise stats, extended)
// Inputs: ExerciseStatsViewModel (route arg exerciseId)
// Outputs: onBack navigation
package com.gymtracker.ui.screens.stats

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gymtracker.domain.AnalyticsEngine
import com.gymtracker.ui.components.GlassSurface
import com.gymtracker.ui.components.GlowBackground
import com.gymtracker.ui.components.LineChart
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

    GlowBackground {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "EXERCISE",
                    style = MaterialTheme.typography.labelSmall,
                    color = GymTheme.colors.hint,
                )
            }

            // Blue Hour indigo hero: the Movo exercise-detail header, on our tokens
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                lerp(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primaryContainer,
                                    0.72f,
                                ),
                            )
                        )
                    )
                    .padding(18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.FitnessCenter,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = state.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        if (state.muscles.isNotBlank()) {
                            Text(
                                text = state.muscles,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f),
                            )
                        }
                    }
                }
            }

            if (analytics == null || analytics.e1rmSeries.isEmpty() && analytics.recentSessions.isEmpty()) {
                GlassSurface {
                    Text(
                        text = "No logged sets for this exercise yet.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                BadgeRow(analytics)

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Best e1RM", analytics.bestE1rm?.let { "${Formats.kg1(it)} kg" } ?: "–", Modifier.weight(1f))
                    StatTile("Best weight", analytics.bestWeightKg?.let { "${Formats.kg1(it)} kg" } ?: "–", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Total sets", "${analytics.totalSets}", Modifier.weight(1f))
                    StatTile("Total volume", "${Formats.volumeKg(analytics.totalVolumeKg)} kg", Modifier.weight(1f))
                }

                if (analytics.e1rmSeries.size >= 2) {
                    ChartBlock("Estimated 1RM", "best set per session · gold dot = all-time best") {
                        LineChart(analytics.e1rmSeries, valueSuffix = " kg")
                    }
                }
                if (analytics.volumeSeries.size >= 2) {
                    ChartBlock("Session volume", "working sets · weight × reps") {
                        LineChart(analytics.volumeSeries, valueSuffix = " kg")
                    }
                }

                if (analytics.recentSessions.isNotEmpty()) {
                    GlassSurface {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Recent sessions", style = MaterialTheme.typography.titleMedium)
                            analytics.recentSessions.forEach { s ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = Instant.ofEpochMilli(s.time)
                                                .atZone(ZoneId.systemDefault()).toLocalDate().format(dateFmt),
                                            style = MaterialTheme.typography.titleSmall,
                                        )
                                        Text(
                                            text = "${s.sets} sets" +
                                                (s.workoutName.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = s.topWeightKg?.let {
                                                "${PlateCalculator.fmt(it)} kg × ${s.topReps ?: "–"}"
                                            } ?: "bodyweight",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontFeatureSettings = FONT_FEATURE_TABULAR
                                            ),
                                        )
                                        Text(
                                            text = s.bestE1rm?.let { "e1RM ${PlateCalculator.fmt(it)}" } ?: "",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = GymTheme.colors.hint,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(
                Modifier
                    .navigationBarsPadding()
                    .height(24.dp)
            )
        }
    }
}

@Composable
private fun BadgeRow(analytics: AnalyticsEngine.ExerciseAnalytics) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
    }
}

@Composable
private fun Badge(label: String, tint: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(tint.copy(alpha = 0.16f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = FONT_FEATURE_TABULAR),
            color = tint,
        )
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    GlassSurface(modifier = modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = FONT_FEATURE_TABULAR),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ChartBlock(title: String, subtitle: String, content: @Composable () -> Unit) {
    GlassSurface {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            content()
        }
    }
}
