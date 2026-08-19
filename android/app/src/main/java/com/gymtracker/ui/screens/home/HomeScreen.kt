// Purpose: Home's stateful shell — collects HomeViewModel, maps it onto the stateless
//          HomeHubScreen the handoff ships (BUILD_ORDER step 6), and owns the first-run
//          profile sheet. All Home layout lives in HomeHubScreen.kt; nothing draws here.
// Inputs: HomeViewModel (real Room stats after import/logging; sample fallback), session state
// Outputs: onStartWorkout / onOpenData / onOpenPlan / onOpenHistory navigation events
package com.gymtracker.ui.screens.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gymtracker.ui.components.ProfileSheet
import com.gymtracker.ui.theme.GymTheme
import com.gymtracker.utils.Formats
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateCaptionFmt = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)

// Live-session snapshot for the Today block's resume state, derived from the activity-scoped session VM
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
    onOpenPlan: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onOpenRecovery: () -> Unit = {},
    vm: HomeViewModel = viewModel(),
) {
    val state by vm.ui.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.refresh() }   // active program may have changed on the Plan tab

    val heat = GymTheme.colors.heat
    // rebuilt only when the state or the live session actually changes — this runs on every
    // Home recomposition otherwise, and it allocates three lists
    val hub = remember(state, liveSession) {
        HomeUi(
            greeting = if (state.userName.isNotBlank()) "Hi, ${state.userName}" else "Hi",
            dateCaption = LocalDate.now().format(dateCaptionFmt),
            weekProgress = if (state.weeklyGoal > 0) {
                (state.workoutsThisWeek / state.weeklyGoal.toFloat()).coerceIn(0f, 1f)
            } else 0f,
            hero = HeroUi(
                readinessTag = state.readinessLabel?.lowercase() ?: "ready",
                readinessFreshness = state.readinessFreshness ?: 1f,
                sessionName = liveSession?.name ?: state.planTitle,
                ctaLabel = if (liveSession != null) "Resume" else "Start",
                meta = liveSession?.let { live ->
                    "${live.completedSets} of ${live.totalSets} sets logged" +
                        (live.currentExercise?.let { " · up now: $it" } ?: "")
                } ?: buildString {
                    append("${state.planRows.size} exercise${if (state.planRows.size == 1) "" else "s"}")
                    state.estimatedMinutes?.let { append(" · about $it min") }
                },
            ),
            readyToTrain = state.readyToTrain.map { m ->
                MuscleCardUi(
                    muscle = m.muscle.replaceFirstChar(Char::uppercase),
                    freshness = m.freshnessPercent / 100f,
                    caption = "${m.freshnessPercent}% · ${m.lastTrainedDaysAgo}d ago",
                )
            },
            jumpBackIn = state.recent.map { r ->
                RecentSessionUi(
                    name = r.name,
                    caption = r.dayLabel + (r.durationMin?.let { " · $it min" } ?: ""),
                    volume = "${Formats.volumeKg(r.volumeKg)} kg",
                )
            },
            avatarInitial = state.userName.firstOrNull()?.uppercase().orEmpty(),
        )
    }

    HomeHubScreen(
        ui = hub,
        heatAt = { heat.at(it) },
        // mid-session the hero becomes Resume; otherwise it starts today's programmed day
        onStart = { if (liveSession != null) onStartWorkout(null) else onStartWorkout(state.programDayId) },
        onOpenSettings = onOpenData,
        onOpenPlan = onOpenPlan,
        onOpenHistory = onOpenHistory,
        onOpenRecovery = onOpenRecovery,
    )

    // first run: introduce yourself (dismissible; also editable later from Data → Profile)
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
