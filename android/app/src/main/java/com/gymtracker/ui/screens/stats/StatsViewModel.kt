// Purpose: Overview analytics state — weekly volume, calendar, durations, PRs, top exercises
// Inputs: WorkoutRepository.analyticsSnapshot() crunched by AnalyticsEngine (off the main thread)
// Outputs: StatsUiState via StateFlow (auto-reloads when sets change)
package com.gymtracker.ui.screens.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gymtracker.data.WorkoutRepository
import com.gymtracker.domain.AnalyticsEngine
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The Week/Month/Year toggle above the volume block (handoff README §7). */
enum class StatsPeriod(val label: String, val days: Int) {
    Week("Week", 7), Month("Month", 30), Year("Year", 365)
}

data class StatsUiState(
    val hasData: Boolean = false,
    val totalVolumeKg: Double = 0.0,   // lifetime working-set tonnage (drives the rank)
    val totalWorkouts: Int = 0,
    val weeklyVolume: List<Pair<LocalDate, Double>> = emptyList(),
    // true once the account's earliest workout is >=4 weeks old — weeklyVolume itself is
    // always a fixed-length zero-padded list, so it can't answer this on its own
    val hasFourWeeksHistory: Boolean = false,
    val weekDeltaPct: Int? = null,   // always week-over-week — feeds the "WEEKLY VOLUME" section only
    val periodDeltaPct: Int? = null, // vs. the selected period toggle — feeds the hero badge
    val calendar: Map<LocalDate, Double> = emptyMap(),
    val durations: List<AnalyticsEngine.Point> = emptyList(),
    val prs: List<AnalyticsEngine.PrEvent> = emptyList(),
    val top: List<AnalyticsEngine.TopExercise> = emptyList(),
    val period: StatsPeriod = StatsPeriod.Week,
    /** Volume / sessions / hours for the selected period — the headline block. */
    val periodVolumeKg: Double = 0.0,
    val periodSessions: Int = 0,
    val periodHours: Double = 0.0,
    val periodPrs: Int = 0,
)

class StatsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = WorkoutRepository.get(app)
    private val _ui = MutableStateFlow(StatsUiState())
    val ui = _ui.asStateFlow()

    init {
        viewModelScope.launch { repo.setCount.collect { reload() } }
    }

    /** Re-aggregate for the chosen period without re-reading the DB snapshot cost twice. */
    fun setPeriod(period: StatsPeriod) {
        viewModelScope.launch { reload(period) }
    }

    private suspend fun reload(period: StatsPeriod = _ui.value.period) {
        val state = withContext(Dispatchers.Default) {
            val snap = repo.analyticsSnapshot()
            if (snap.workouts.isEmpty()) return@withContext StatsUiState(period = period)

            // window the snapshot to the selected period for the headline figures
            val since = System.currentTimeMillis() - period.days * 86_400_000L
            val periodWorkouts = snap.workouts.filter { it.startedAt >= since }
            val periodWorkoutIds = periodWorkouts.map { it.id }.toSet()
            val periodSets = snap.sets.filter { it.workoutId in periodWorkoutIds && it.tag != "W" }
            val periodVolume = periodSets.sumOf { (it.weightKg ?: 0.0) * (it.reps ?: 0) }
            val periodHours = periodWorkouts.sumOf { w ->
                w.endedAt?.let { (it - w.startedAt) / 3_600_000.0 } ?: 0.0
            }
            // previous period of the same length, for the hero's own delta badge — was
            // previously always week-over-week regardless of the Week/Month/Year toggle
            val prevSince = since - period.days * 86_400_000L
            val prevWorkoutIds = snap.workouts
                .filter { it.startedAt >= prevSince && it.startedAt < since }
                .map { it.id }.toSet()
            val prevVolume = snap.sets
                .filter { it.workoutId in prevWorkoutIds && it.tag != "W" }
                .sumOf { (it.weightKg ?: 0.0) * (it.reps ?: 0) }
            val periodDelta = if (prevVolume > 0) {
                (((periodVolume - prevVolume) / prevVolume) * 100).roundToInt()
            } else null
            val weekly = AnalyticsEngine.weeklyVolume(snap.sets)
            val earliestWorkout = snap.workouts.minOfOrNull { it.startedAt }
            val fourWeeksHistory = earliestWorkout != null &&
                earliestWorkout <= System.currentTimeMillis() - 28 * 86_400_000L
            val delta = weekly.takeLast(2).let { last ->
                if (last.size == 2 && last[0].second > 0) {
                    (((last[1].second - last[0].second) / last[0].second) * 100).roundToInt()
                } else null
            }
            val totalVolume = snap.sets
                .filter { it.tag != "W" }
                .sumOf { (it.weightKg ?: 0.0) * (it.reps ?: 0) }
            StatsUiState(
                hasData = true,
                totalVolumeKg = totalVolume,
                totalWorkouts = snap.workouts.size,
                weeklyVolume = weekly,
                hasFourWeeksHistory = fourWeeksHistory,
                weekDeltaPct = delta,
                periodDeltaPct = periodDelta,
                calendar = AnalyticsEngine.calendarVolume(snap.sets),
                durations = AnalyticsEngine.durationSeries(snap.workouts),
                prs = AnalyticsEngine.prTimeline(snap.sets, snap.exercises),
                top = AnalyticsEngine.topExercises(snap.sets, snap.exercises),
                period = period,
                periodVolumeKg = periodVolume,
                periodSessions = periodWorkouts.size,
                periodHours = periodHours,
                periodPrs = AnalyticsEngine.prTimeline(snap.sets, snap.exercises)
                    .count { it.time >= since },
            )
        }
        _ui.value = state
    }
}
