// Purpose: Stats overview tab — weekly volume, training calendar, duration trend, PRs, top exercises
// Inputs: StatsViewModel (real analytics once history exists)
// Outputs: onOpenExercise(id) navigation to the per-exercise stats page
package com.gymtracker.ui.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gymtracker.ui.components.CalendarHeatmap
import com.gymtracker.ui.components.GlassSurface
import com.gymtracker.ui.components.GlowBackground
import com.gymtracker.ui.components.LineChart
import com.gymtracker.ui.components.WeeklyBarChart
import com.gymtracker.ui.theme.FONT_FEATURE_TABULAR
import com.gymtracker.ui.theme.GymTheme
import com.gymtracker.utils.Formats
import com.gymtracker.utils.PlateCalculator
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val prDateFmt = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

@Composable
fun StatsScreen(
    onOpenExercise: (String) -> Unit = {},
    vm: StatsViewModel = viewModel(),
) {
    val state by vm.ui.collectAsStateWithLifecycle()

    GlowBackground {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Stats", style = MaterialTheme.typography.headlineLarge)

            if (!state.hasData) {
                GlassSurface {
                    Text(
                        text = "Import your history or log workouts to unlock analytics.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                ChartCard(
                    title = "Weekly volume",
                    subtitle = "working sets · dashed = 4-week average",
                    trailing = state.weekDeltaPct?.let { d -> (if (d >= 0) "▲ $d%" else "▼ ${-d}%") to (d >= 0) },
                ) {
                    WeeklyBarChart(state.weeklyVolume)
                }

                ChartCard(title = "Training calendar", subtitle = "last 20 weeks") {
                    CalendarHeatmap(state.calendar)
                }

                if (state.durations.size >= 2) {
                    ChartCard(title = "Session duration", subtitle = "minutes · last 30 sessions") {
                        LineChart(state.durations)
                    }
                }

                if (state.prs.isNotEmpty()) {
                    GlassSurface {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Recent PRs", style = MaterialTheme.typography.titleMedium)
                            state.prs.forEach { pr ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(GymTheme.colors.prGold.copy(alpha = 0.16f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            Icons.Rounded.Star, contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = GymTheme.colors.prGold,
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(pr.exerciseName, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            text = "${PlateCalculator.fmt(pr.value)} kg" +
                                                (pr.reps?.let { " × $it" } ?: ""),
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontFeatureSettings = FONT_FEATURE_TABULAR
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Text(
                                        text = Instant.ofEpochMilli(pr.time)
                                            .atZone(ZoneId.systemDefault()).toLocalDate().format(prDateFmt),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = GymTheme.colors.hint,
                                    )
                                }
                            }
                        }
                    }
                }

                if (state.top.isNotEmpty()) {
                    GlassSurface {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Most trained · 30 days", style = MaterialTheme.typography.titleMedium)
                            state.top.forEach { t ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onOpenExercise(t.exerciseId) }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(t.name, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            text = "${t.sets} sets",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Text(
                                        text = "${Formats.volumeKg(t.volumeKg)} kg",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontFeatureSettings = FONT_FEATURE_TABULAR
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Icon(
                                        Icons.Rounded.ChevronRight,
                                        contentDescription = "Open ${t.name} stats",
                                        tint = GymTheme.colors.hint,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(
                Modifier
                    .navigationBarsPadding()
                    .height(88.dp)
            )
        }
    }
}

@Composable
private fun ChartCard(
    title: String,
    subtitle: String,
    trailing: Pair<String, Boolean>? = null,
    content: @Composable () -> Unit,
) {
    GlassSurface {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                trailing?.let { (label, positive) ->
                    val tint = if (positive) GymTheme.colors.success else MaterialTheme.colorScheme.error
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(tint.copy(alpha = 0.16f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFeatureSettings = FONT_FEATURE_TABULAR
                            ),
                            color = tint,
                        )
                    }
                }
            }
            content()
        }
    }
}
