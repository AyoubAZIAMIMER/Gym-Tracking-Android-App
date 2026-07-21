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
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gymtracker.ui.components.CalendarHeatmap
import com.gymtracker.ui.components.FlatRow
import com.gymtracker.ui.components.GlassSurface
import com.gymtracker.ui.components.GlowBackground
import com.gymtracker.ui.components.LineChart
import com.gymtracker.ui.components.WeeklyBarChart
import com.gymtracker.domain.Rank
import com.gymtracker.ui.theme.FONT_FEATURE_TABULAR
import com.gymtracker.ui.theme.GymTheme
import com.gymtracker.ui.theme.forgedEntrance
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
    // §10: sections rise in once per screen entry, never on tab return
    var entered by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

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
                RankVolumeCard(state, Modifier.forgedEntrance(0, entered))

                ChartCard(
                    title = "Weekly volume",
                    subtitle = "working sets · dashed = 4-week average",
                    trailing = state.weekDeltaPct?.let { d -> (if (d >= 0) "▲ $d%" else "▼ ${-d}%") to (d >= 0) },
                    modifier = Modifier.forgedEntrance(0, entered),
                ) {
                    WeeklyBarChart(state.weeklyVolume)
                }

                ChartCard(
                    title = "Training calendar",
                    subtitle = "last 20 weeks",
                    modifier = Modifier.forgedEntrance(1, entered),
                ) {
                    CalendarHeatmap(state.calendar)
                }

                if (state.durations.size >= 2) {
                    ChartCard(
                        title = "Session duration",
                        subtitle = "minutes · last 30 sessions",
                        modifier = Modifier.forgedEntrance(2, entered),
                    ) {
                        LineChart(state.durations)
                    }
                }

                // supporting lists render flat — the charts above stay the hero surfaces
                if (state.prs.isNotEmpty()) {
                    Column(Modifier.forgedEntrance(3, entered)) {
                        Text(
                            text = "RECENT PRS",
                            style = MaterialTheme.typography.labelSmall,
                            color = GymTheme.colors.hint,
                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                        )
                        state.prs.forEachIndexed { i, pr ->
                            FlatRow(divider = i != state.prs.lastIndex) {
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

                if (state.top.isNotEmpty()) {
                    Column(Modifier.forgedEntrance(4, entered)) {
                        Text(
                            text = "MOST TRAINED · 30 DAYS",
                            style = MaterialTheme.typography.labelSmall,
                            color = GymTheme.colors.hint,
                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                        )
                        state.top.forEachIndexed { i, t ->
                            FlatRow(
                                onClick = { onOpenExercise(t.exerciseId) },
                                divider = i != state.top.lastIndex,
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

            // clearance for the floating bottom nav (measured, not the OS bar alone)
            Spacer(
                Modifier
                    .navigationBarsPadding()
                    .height(112.dp)
            )
        }
    }
}

/**
 * Blue Hour rank header: the Movo "Total Weight" tile made real, with a SUBTLE rank cue
 * (Wood → Olympian by lifetime tonnage) — one quiet medal pill and a "N to next" line, no
 * badges or level-up theatre.
 */
@Composable
private fun RankVolumeCard(state: StatsUiState, modifier: Modifier = Modifier) {
    val standing = remember(state.totalVolumeKg) { Rank.standing(state.totalVolumeKg) }
    val medal = rankColor(standing.rank)
    GlassSurface(modifier = modifier) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.FitnessCenter,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Total volume moved",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = Formats.volumeKg(state.totalVolumeKg),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFeatureSettings = FONT_FEATURE_TABULAR
                            ),
                        )
                        Text(
                            text = " kg",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 3.dp),
                        )
                    }
                }
                RankPill(standing.rank, medal)
            }
            val progress = standing.progress
            if (progress != null && standing.next != null && standing.toNextKg != null) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(progress)
                                .height(6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(medal),
                        )
                    }
                    Text(
                        text = "${Formats.volumeKg(standing.toNextKg)} kg to ${standing.next.label}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFeatureSettings = FONT_FEATURE_TABULAR
                        ),
                        color = GymTheme.colors.hint,
                    )
                }
            } else {
                Text(
                    text = "Top rank — ${state.totalWorkouts} sessions logged",
                    style = MaterialTheme.typography.labelMedium,
                    color = medal,
                )
            }
        }
    }
}

@Composable
private fun RankPill(rank: Rank, color: Color) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(color))
        Text(rank.label, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

@Composable
private fun rankColor(rank: Rank): Color = when (rank) {
    Rank.WOOD -> GymTheme.colors.rankWood
    Rank.BRONZE -> GymTheme.colors.rankBronze
    Rank.SILVER -> GymTheme.colors.rankSilver
    Rank.GOLD -> GymTheme.colors.prGold
    Rank.OLYMPIAN -> MaterialTheme.colorScheme.onSurface
}

@Composable
private fun ChartCard(
    title: String,
    subtitle: String,
    trailing: Pair<String, Boolean>? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    GlassSurface(modifier = modifier) {
        // tight chrome: the chart is the content, the card is just its plate
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
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
