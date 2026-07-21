// Purpose: Home dashboard — weekly goal ring, streak, Mo–Su strip, plan preview, stat tiles
// Inputs: HomeViewModel (real Room stats after import/logging; sample fallback), session state
// Outputs: onStartWorkout / onOpenData navigation events
package com.gymtracker.ui.screens.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gymtracker.data.WorkoutRepository
import com.gymtracker.ui.components.ForgedRing
import com.gymtracker.ui.components.GlassSurface
import com.gymtracker.ui.components.GlowBackground
import com.gymtracker.ui.components.MuscleTargetFigure
import com.gymtracker.ui.components.ProfileSheet
import com.gymtracker.ui.theme.FONT_FEATURE_TABULAR
import com.gymtracker.ui.theme.GymTheme
import com.gymtracker.ui.theme.forgedPress
import com.gymtracker.utils.Formats
import com.gymtracker.utils.TimeFormat
import java.time.DayOfWeek
import kotlin.math.PI
import kotlin.math.sin
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

// Live-session snapshot for the Now Card, derived from the activity-scoped session VM
data class LiveSessionInfo(
    val name: String,
    val completedSets: Int,
    val totalSets: Int,
    val startedAtMillis: Long,
    val currentExercise: String?,
)

@Composable
fun HomeScreen(
    onStartWorkout: (String?) -> Unit,
    liveSession: LiveSessionInfo? = null,
    onOpenData: () -> Unit = {},
    vm: HomeViewModel = viewModel(),
) {
    val state by vm.ui.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.refresh() }   // active program may have changed on the Plan tab

    GlowBackground {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            HeaderRow(state.userName, onOpenData)
            WeeklyGoalCard(state)
            // UX v6 Now Card: Home leads with the current state of the forge
            val forged = state.todayForged
            when {
                liveSession != null ->
                    LiveSessionCard(liveSession, onResume = { onStartWorkout(null) })
                forged != null -> {
                    ForgedTodayCard(forged)
                    PlanCard(state, onStartWorkout)   // demoted: next time's work
                }
                else -> PlanCard(state, onStartWorkout)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // forge-native tile icons: hammer = sessions struck, hourglass = cooling
                // since the last strike, blazing flame = the furnace kept burning
                StatTile(
                    value = "${state.workoutsThisWeek}/${state.weeklyGoal}",
                    label = "Sessions this week",
                    icon = Icons.Rounded.FitnessCenter,
                    iconTint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    value = state.lastWorkoutDaysAgo?.let { "${it}d" } ?: "–",
                    label = "Since last session",
                    icon = Icons.Rounded.HourglassBottom,
                    // v5: the hourglass cools like the metal — glowing right after a
                    // strike, quenched steel once you've rested ~3 days
                    iconTint = state.lastWorkoutDaysAgo
                        ?.let { GymTheme.colors.heat.at((it / 3f).coerceIn(0f, 1f)) }
                        ?: MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    value = "${state.streakWeeks}",
                    label = "Week streak",
                    icon = Icons.Rounded.Whatshot,
                    iconTint = GymTheme.colors.prGold,
                    modifier = Modifier.weight(1f),
                )
            }
            // clearance for the floating bottom nav
            Spacer(
                Modifier
                    .navigationBarsPadding()
                    .height(88.dp)
            )
        }
    }

    // first run: introduce yourself at the anvil (dismissible; also editable later
    // from Data → Profile)
    var profileSkipped by rememberSaveable { mutableStateOf(false) }
    if (state.needsProfile && !profileSkipped) {
        ProfileSheet(
            onSave = { name, weightKg, heightCm, weeklyGoal ->
                vm.saveProfile(name, weightKg, heightCm, weeklyGoal)
            },
            onDismiss = { profileSkipped = true },
        )
    }
}

@Composable
private fun HeaderRow(userName: String, onOpenData: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "Hi, ${userName.ifBlank { "smith" }}",
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                text = "Lights on.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        GlassSurface(shape = RoundedCornerShape(50)) {
            Text(
                text = LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH)),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
            )
        }
        IconButton(onClick = onOpenData) {
            Icon(
                Icons.Rounded.Settings,
                contentDescription = "Data & settings",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WeeklyGoalCard(state: HomeUiState) {
    GlassSurface {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)),
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
                        text = "${state.workoutsThisWeek} of ${state.weeklyGoal} workouts",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = "this week · ${state.streakWeeks}-week streak",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                    // Hot Tip ring: the weekly fill is led by a molten glow (§7.4)
                    ForgedRing(
                        progress = (state.workoutsThisWeek / state.weeklyGoal.toFloat()).coerceIn(0f, 1f),
                        modifier = Modifier.fillMaxSize(),
                    )
                    // Forged Motion §7.3, Law 5 — Home's one live ember: the streak flame
                    // flickers on two incommensurate sines (3.2 s + 5.1 s), below perception
                    val flicker = rememberInfiniteTransition(label = "flame")
                    val fa by flicker.animateFloat(
                        0f, 1f,
                        infiniteRepeatable(tween(3_200, easing = LinearEasing)),
                        label = "fa",
                    )
                    val fb by flicker.animateFloat(
                        0f, 1f,
                        infiniteRepeatable(tween(5_100, easing = LinearEasing)),
                        label = "fb",
                    )
                    Icon(
                        Icons.Rounded.LocalFireDepartment,
                        contentDescription = null,
                        modifier = Modifier
                            .size(22.dp)
                            .graphicsLayer {
                                val s = sin(fa * 2f * PI.toFloat()) * sin(fb * 2f * PI.toFloat())
                                alpha = 0.92f + 0.08f * (0.5f + 0.5f * s)
                            },
                        tint = GymTheme.colors.prGold,
                    )
                }
            }
            WeekStrip(state.doneWeekdays)
        }
    }
}

@Composable
private fun WeekStrip(doneWeekdays: Set<DayOfWeek>) {
    val today = LocalDate.now().dayOfWeek
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        DayOfWeek.entries.forEach { day ->
            val done = day in doneWeekdays
            val isToday = day == today
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = day.getDisplayName(JavaTextStyle.SHORT, Locale.ENGLISH).take(2),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isToday) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            if (done) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
                        )
                        .border(
                            width = 1.5.dp,
                            color = if (isToday && !done) MaterialTheme.colorScheme.primary
                            else Color.Transparent,
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (done) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White,
                        )
                    }
                }
            }
        }
    }
}

// Now Card, mid-session state: the anvil is hot — one tap back to it
@Composable
private fun LiveSessionCard(live: LiveSessionInfo, onResume: () -> Unit) {
    // ticking elapsed clock — the card must feel live, not archived
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1_000)
            now = System.currentTimeMillis()
        }
    }
    GlassSurface {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "IN SESSION · ${TimeFormat.clock(now - live.startedAtMillis)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(live.name, style = MaterialTheme.typography.titleLarge)
            Text(
                text = buildString {
                    append("${live.completedSets} of ${live.totalSets} sets logged")
                    live.currentExercise?.let { append(" · up now: $it") }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val pressSource = remember { MutableInteractionSource() }
            Button(
                onClick = onResume,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .forgedPress(pressSource),
                interactionSource = pressSource,
                shape = RoundedCornerShape(50),
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Resume session", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

// Now Card, finished-today state: the payoff — what you forged, glowing on the body
@Composable
private fun ForgedTodayCard(forged: WorkoutRepository.TodayForged) {
    GlassSurface {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "SESSION COMPLETE",
                style = MaterialTheme.typography.labelSmall,
                color = GymTheme.colors.success,
            )
            Text(forged.name, style = MaterialTheme.typography.titleLarge)
            MuscleTargetFigure(
                muscles = forged.muscles,
                modifier = Modifier.height(150.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                ForgedStat(Formats.volumeKg(forged.volumeKg), "kg moved")
                ForgedStat("${forged.workingSets}", "working sets")
                forged.durationMin?.let { ForgedStat("$it", "minutes") }
            }
            Text(
                text = "Logged. Recovery starts now.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ForgedStat(value: String, label: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontFeatureSettings = FONT_FEATURE_TABULAR
            ),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// Plan preview: active program day > repeat-last workout > sample
@Composable
private fun PlanCard(state: HomeUiState, onStartWorkout: (String?) -> Unit) {
    GlassSurface {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = state.planLabel,
                style = MaterialTheme.typography.labelSmall,
                color = GymTheme.colors.hint,
            )
            Text(state.planTitle, style = MaterialTheme.typography.titleLarge)
            // v5: today's targets glow on the physique — the muscle list as a body,
            // not a sentence (canonical names arrive comma-joined from the ViewModel)
            MuscleTargetFigure(
                muscles = state.planMuscles.split(",").map(String::trim),
                modifier = Modifier.height(170.dp),
            )
            state.planRows.forEach { row ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // v5: each exercise's dot is its primary muscle's heat — steel says
                    // "recovered, hit it", red says "still glowing from last time"
                    Box(
                        Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                row.freshness?.let { GymTheme.colors.heat.at(it / 100f) }
                                    ?: MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(row.name, style = MaterialTheme.typography.titleSmall)
                        if (row.muscle.isNotBlank()) {
                            Text(
                                text = row.muscle,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Text(
                        text = row.detail,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFeatureSettings = FONT_FEATURE_TABULAR
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            val pressSource = remember { MutableInteractionSource() }
            Button(
                onClick = { onStartWorkout(state.programDayId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .forgedPress(pressSource),
                interactionSource = pressSource,
                shape = RoundedCornerShape(50),
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Start session", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun StatTile(
    value: String,
    label: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
) {
    GlassSurface(modifier = modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = iconTint)
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFeatureSettings = FONT_FEATURE_TABULAR
                ),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
