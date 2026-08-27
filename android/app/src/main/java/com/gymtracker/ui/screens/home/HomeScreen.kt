// Purpose: Home's stateful shell — collects HomeViewModel, maps it onto the stateless
//          HomeSlateScreen (the flat, hairline-ruled layout), and owns the first-run
//          profile sheet. All Home layout lives in HomeSlateScreen.kt; nothing draws here.
// Inputs: HomeViewModel (real Room stats after import/logging; sample fallback), session state
// Outputs: onStartWorkout / onOpenData / onOpenPlan / onOpenHistory navigation events
package com.gymtracker.ui.screens.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
    onOpenWorkout: (String) -> Unit = {},
    onNeedsOnboarding: () -> Unit = {},
    vm: HomeViewModel = viewModel(),
) {
    val state by vm.ui.collectAsStateWithLifecycle()
    // active program may have changed on the Plan tab. Awaited (not fire-and-forget refresh())
    // so the onboarding redirect below reads a state that's actually current — a HomeViewModel
    // retained from before onboarding started still holds needsProfile=true until this
    // completes, and checking the stale value raced this refresh, bouncing straight back into
    // a blank onboarding after it had just been finished.
    LaunchedEffect(Unit) {
        vm.awaitRefresh()
        if (vm.ui.value.needsProfile) onNeedsOnboarding()
    }

    val heat = GymTheme.colors.heat
    // rebuilt only when the state or the live session actually changes — this runs on every
    // Home recomposition otherwise, and it allocates three lists
    val hub = remember(state, liveSession) {
        HomeUi(
            dateCaption = LocalDate.now().format(dateCaptionFmt),
            weekProgress = if (state.weeklyGoal > 0) {
                (state.workoutsThisWeek / state.weeklyGoal.toFloat()).coerceIn(0f, 1f)
            } else 0f,
            weekDone = state.workoutsThisWeek,
            weekGoal = state.weeklyGoal,
            doneWeekdays = state.doneWeekdays,
            hero = HeroUi(
                // the Slate's TODAY stamp wants "QUADS WORN", not "quads worn"
                readinessTag = state.readinessLabel?.uppercase() ?: "READY",
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
            upcoming = state.upcoming.map { UpcomingRowUi(it.name, it.muscles, it.dayLabel) },
            recent = state.recent.map { r ->
                RecentSessionUi(
                    id = r.id,
                    name = r.name,
                    caption = r.dayLabel + (r.durationMin?.let { " · $it min" } ?: ""),
                    volume = "${Formats.volumeKg(r.volumeKg)} kg",
                )
            },
            avatarInitial = state.userName.firstOrNull()?.uppercase().orEmpty(),
        )
    }

    HomeSlateScreen(
        ui = hub,
        heatAt = { heat.at(it) },
        // mid-session the hero becomes Resume; otherwise it starts today's programmed day
        onStart = { if (liveSession != null) onStartWorkout(null) else onStartWorkout(state.programDayId) },
        onOpenSettings = onOpenData,
        onOpenPlan = onOpenPlan,
        onOpenHistory = onOpenHistory,
        onOpenWorkout = onOpenWorkout,
    )

}
