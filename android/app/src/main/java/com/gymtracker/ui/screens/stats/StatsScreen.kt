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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.roundToInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gymtracker.ui.components.CalendarHeatmap
import com.gymtracker.ui.components.ForgedListRow
import com.gymtracker.ui.components.RowRule
import com.gymtracker.ui.components.GlowBackground
import com.gymtracker.ui.components.LineChart
import com.gymtracker.ui.components.WeeklyBarChart
import com.gymtracker.ui.components.forgeHero
import com.gymtracker.domain.Rank
import com.gymtracker.ui.theme.FONT_FEATURE_TABULAR
import com.gymtracker.ui.theme.GymTheme
import com.gymtracker.ui.components.ForgedScreenTitle
import com.gymtracker.ui.components.ForgedSectionHeader
import com.gymtracker.ui.components.SectionRule
import com.gymtracker.ui.theme.Dim
import com.gymtracker.ui.theme.forgedEntrance
import com.gymtracker.utils.Formats
import com.gymtracker.utils.PlateCalculator
import java.time.Instant
import java.time.ZoneId
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.width
import com.gymtracker.ui.theme.forgedPress
import com.gymtracker.ui.theme.rollUpValue
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

    GlowBackground(glowAlpha = 0.12f) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding(),
        ) {
            ForgedScreenTitle("Stats")
            PeriodToggle(state.period, onSelect = vm::setPeriod)

            if (!state.hasData) {
                Text(
                    text = "Import your history or log workouts to unlock analytics.",
                    modifier = Modifier.padding(horizontal = Dim.screenPadH, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                // ui-ux-pro-max pass, 2026-08-21: one hero per screen, matching every sibling
                // tab (Home's Start card, Plan's Up Next, Recovery's body map) — the headline
                // number and the all-time rank used to split across a flat block plus a
                // separate glass card, two elevated surfaces competing for the same "hero" job.
                StatsHero(state, Modifier.padding(top = 4.dp).forgedEntrance(0, entered))

                Column(Modifier.fillMaxWidth().forgedEntrance(1, entered)) {
                    SectionRule()
                    ForgedSectionHeader(
                        label = "WEEKLY VOLUME · 8 WKS",
                        bottomPadding = 14.dp,
                        trailing = state.weekDeltaPct?.let { d ->
                            {
                                val positive = d >= 0
                                Text(
                                    text = if (positive) "▲ $d%" else "▼ ${-d}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (positive) GymTheme.colors.success
                                    else MaterialTheme.colorScheme.error,
                                )
                            }
                        },
                    )
                    Box(Modifier.padding(horizontal = Dim.screenPadH).padding(bottom = 20.dp)) {
                        WeeklyBarChart(state.weeklyVolume.takeLast(8))
                    }
                }

                ChartCard(
                    title = "Training calendar",
                    subtitle = "20 wks",
                    modifier = Modifier.forgedEntrance(2, entered),
                ) {
                    CalendarHeatmap(state.calendar)
                }

                if (state.durations.size >= 2) {
                    ChartCard(
                        title = "Session duration",
                        subtitle = "last 30",
                        modifier = Modifier.forgedEntrance(3, entered),
                    ) {
                        LineChart(state.durations)
                    }
                }

                // supporting lists render flat — the charts above stay the hero surfaces
                if (state.prs.isNotEmpty()) {
                    Column(Modifier.forgedEntrance(4, entered)) {
                        SectionRule()
                        ForgedSectionHeader("RECENT PRS", bottomPadding = 4.dp)
                        state.prs.forEach { pr ->
                            RowRule()
                            ForgedListRow(
                                title = pr.exerciseName,
                                subtitle = "${PlateCalculator.fmt(pr.value)} kg" +
                                    (pr.reps?.let { " × $it" } ?: ""),
                                trailing = {
                                    Text(
                                        text = Instant.ofEpochMilli(pr.time)
                                            .atZone(ZoneId.systemDefault()).toLocalDate()
                                            .format(prDateFmt),
                                        fontSize = 12.sp,
                                        color = GymTheme.colors.hint,
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Icon(
                                        Icons.Rounded.Star, contentDescription = "Personal record",
                                        modifier = Modifier.size(16.dp),
                                        tint = GymTheme.colors.prGold,
                                    )
                                },
                            )
                        }
                    }
                }

                if (state.top.isNotEmpty()) {
                    Column(Modifier.forgedEntrance(5, entered)) {
                        SectionRule()
                        ForgedSectionHeader("MOST TRAINED · 30 DAYS", bottomPadding = 4.dp)
                        state.top.forEach { t ->
                            RowRule()
                            ForgedListRow(
                                title = t.name,
                                subtitle = "${t.sets} sets",
                                onClick = { onOpenExercise(t.exerciseId) },
                                chevron = true,
                                trailing = {
                                    Text(
                                        text = "${Formats.volumeKg(t.volumeKg)} kg",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontSize = 15.sp,
                                            fontFeatureSettings = FONT_FEATURE_TABULAR,
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                            )
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
    // the prototype has no chart cards: a stamped rule, then the chart on the page itself
    Column(modifier) {
        SectionRule()
        ForgedSectionHeader(
            label = "${title.uppercase()} · ${subtitle.uppercase()}",
            bottomPadding = 14.dp,
            trailing = trailing?.let { (label, positive) ->
                {
                    val tint = if (positive) GymTheme.colors.success else MaterialTheme.colorScheme.error
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = tint,
                    )
                }
            },
        )
        Box(Modifier.padding(horizontal = Dim.screenPadH).padding(bottom = 20.dp)) {
            content()
        }
    }
}

/**
 * Stats' one hero (forgeHero — matches Home's Start card, Plan's Up Next, Recovery's body map):
 * this period's tonnage as one big Anton numeral with its delta and session/PR/hours trio, a
 * hairline, then all-time rank standing — previously two competing elevated surfaces (a flat
 * headline block plus a separate glass card), now one.
 */
@Composable
private fun StatsHero(state: StatsUiState, modifier: Modifier = Modifier) {
    val standing = remember(state.totalVolumeKg) { Rank.standing(state.totalVolumeKg) }
    val medal = rankColor(standing.rank)
    val hours = state.periodHours
    Column(
        modifier
            .fillMaxWidth()
            .forgeHero()
            .padding(18.dp),
    ) {
        Text(
            text = "VOLUME THIS ${state.period.label.uppercase()}",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, letterSpacing = 1.6.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            Modifier.padding(top = 10.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = Formats.volumeKg(rollUpValue(state.periodVolumeKg.toFloat()).toDouble()),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 38.sp,
                        lineHeight = 38.sp,
                        fontFeatureSettings = FONT_FEATURE_TABULAR,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "kg",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 15.sp),
                    color = GymTheme.colors.hint,
                    modifier = Modifier.padding(start = 4.dp, bottom = 3.dp),
                )
            }
            state.weekDeltaPct?.let { d ->
                Text(
                    text = if (d >= 0) "▲$d%" else "▼${-d}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (d >= 0) GymTheme.colors.success else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
        }
        Row(
            Modifier.padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(26.dp),
        ) {
            StatFigure("${state.periodSessions}", "sessions", MaterialTheme.colorScheme.onSurface)
            // gold means earned (PRs, ranks) — a zero count is not an achievement to celebrate
            StatFigure(
                "${state.periodPrs}",
                "PRs",
                if (state.periodPrs > 0) GymTheme.colors.prGold else MaterialTheme.colorScheme.onSurface,
            )
            StatFigure(String.format(Locale.ENGLISH, "%.1f", hours), "hours", MaterialTheme.colorScheme.onSurface)
        }

        Box(
            Modifier
                .padding(vertical = 18.dp)
                .fillMaxWidth()
                .height(Dim.hairline)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
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
            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

/** Week / Month / Year — inline text with an accent underline, per the reference render. */
@Composable
private fun PeriodToggle(selected: StatsPeriod, onSelect: (StatsPeriod) -> Unit) {
    Row(
        Modifier.padding(start = Dim.screenPadH, end = Dim.screenPadH, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        StatsPeriod.entries.forEach { period ->
            val active = period == selected
            val source = remember { MutableInteractionSource() }
            Column(
                Modifier
                    .forgedPress(source, pressedScale = 0.97f)
                    .clickable(interactionSource = source, indication = null) { onSelect(period) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = period.label,
                    fontSize = 13.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    color = if (active) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(
                    Modifier
                        .padding(top = 5.dp)
                        .width(22.dp)
                        .height(2.dp)
                        .background(
                            if (active) MaterialTheme.colorScheme.primary else Color.Transparent
                        )
                )
            }
        }
    }
}

@Composable
private fun StatFigure(value: String, caption: String, color: androidx.compose.ui.graphics.Color) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontFeatureSettings = FONT_FEATURE_TABULAR,
            ),
            color = color,
        )
        Text(
            text = caption,
            fontSize = 11.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 1.dp),
        )
    }
}
