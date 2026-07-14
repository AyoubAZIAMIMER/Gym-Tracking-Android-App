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

data class StatsUiState(
    val hasData: Boolean = false,
    val weeklyVolume: List<Pair<LocalDate, Double>> = emptyList(),
    val weekDeltaPct: Int? = null,
    val calendar: Map<LocalDate, Double> = emptyMap(),
    val durations: List<AnalyticsEngine.Point> = emptyList(),
    val prs: List<AnalyticsEngine.PrEvent> = emptyList(),
    val top: List<AnalyticsEngine.TopExercise> = emptyList(),
)

class StatsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = WorkoutRepository.get(app)
    private val _ui = MutableStateFlow(StatsUiState())
    val ui = _ui.asStateFlow()

    init {
        viewModelScope.launch { repo.setCount.collect { reload() } }
    }

    private suspend fun reload() {
        val state = withContext(Dispatchers.Default) {
            val snap = repo.analyticsSnapshot()
            if (snap.workouts.isEmpty()) return@withContext StatsUiState()
            val weekly = AnalyticsEngine.weeklyVolume(snap.sets)
            val delta = weekly.takeLast(2).let { last ->
                if (last.size == 2 && last[0].second > 0) {
                    (((last[1].second - last[0].second) / last[0].second) * 100).roundToInt()
                } else null
            }
            StatsUiState(
                hasData = true,
                weeklyVolume = weekly,
                weekDeltaPct = delta,
                calendar = AnalyticsEngine.calendarVolume(snap.sets),
                durations = AnalyticsEngine.durationSeries(snap.workouts),
                prs = AnalyticsEngine.prTimeline(snap.sets, snap.exercises),
                top = AnalyticsEngine.topExercises(snap.sets, snap.exercises),
            )
        }
        _ui.value = state
    }
}
