// Purpose: History tab state — month calendar of training days + that month's workout log
// Inputs: WorkoutRepository.monthHistory; month paging clamped to [first workout, today]
// Outputs: HistoryUiState via StateFlow (reloads on workout-count changes and month moves)
package com.gymtracker.ui.screens.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gymtracker.data.WorkoutRepository
import com.gymtracker.utils.Formats
import com.gymtracker.utils.TimeFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HistoryListRow(
    val workoutId: String,
    val day: Int,
    val title: String,
    val subtitle: String,   // "Sat 12 · 18:24 · 1:12:03"
    val detail: String,     // "24 sets · 8,540 kg"
    val muscles: String,
)

data class HistoryUiState(
    val monthLabel: String = "",
    val canGoPrev: Boolean = false,
    val canGoNext: Boolean = false,
    val leadingBlanks: Int = 0,     // empty cells before day 1 (Monday-first grid)
    val daysInMonth: Int = 0,
    val todayDay: Int? = null,      // day-of-month when the shown month is the current one
    val volumeByDay: Map<Int, Double> = emptyMap(),
    val maxDayVolume: Double = 0.0,
    val selectedDay: Int? = null,
    val monthSummary: String = "",
    val monthWorkouts: Int = 0,         // for the count-up roll in the header
    val monthVolumeKg: Double = 0.0,
    val rows: List<HistoryListRow> = emptyList(),
)

class HistoryViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = WorkoutRepository.get(app)
    private val zone = ZoneId.systemDefault()

    private var month: YearMonth = YearMonth.now(zone)
    private var selectedDay: Int? = null
    private var allRows: List<HistoryListRow> = emptyList()

    private val _ui = MutableStateFlow(HistoryUiState())
    val ui = _ui.asStateFlow()

    init {
        viewModelScope.launch { repo.workoutCount.collect { reload() } }
    }

    fun previousMonth() = moveMonth(-1)
    fun nextMonth() = moveMonth(1)

    private fun moveMonth(delta: Long) {
        month = month.plusMonths(delta)
        selectedDay = null
        viewModelScope.launch { reload() }
    }

    /** Tap a training day to filter the list; tap it again to clear the filter. */
    fun toggleDay(day: Int) {
        selectedDay = if (selectedDay == day) null else day
        publish(_ui.value.copy())
    }

    private suspend fun reload() {
        val history = repo.monthHistory(month)
        val timeFmt = DateTimeFormatter.ofPattern("EEE d · HH:mm", Locale.getDefault())
        allRows = history.rows.map { row ->
            val started = Instant.ofEpochMilli(row.startedAt).atZone(zone)
            HistoryListRow(
                workoutId = row.workoutId,
                day = started.dayOfMonth,
                title = row.name,
                subtitle = started.format(timeFmt) +
                    (row.durationMillis?.let { " · ${TimeFormat.clock(it)}" } ?: ""),
                detail = "${row.setCount} sets · ${Formats.volumeKg(row.volumeKg)} kg",
                muscles = row.muscles,
            )
        }
        // an empty month can still be paged into; clamp to the user's first workout
        val earliestMonth = repo.earliestWorkoutStart()
            ?.let { YearMonth.from(Instant.ofEpochMilli(it).atZone(zone)) }
            ?: YearMonth.now(zone)
        val now = YearMonth.now(zone)
        if (selectedDay != null && allRows.none { it.day == selectedDay }) selectedDay = null

        publish(
            HistoryUiState(
                monthLabel = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                canGoPrev = month > earliestMonth,
                canGoNext = month < now,
                leadingBlanks = month.atDay(1).dayOfWeek.value - 1,
                daysInMonth = month.lengthOfMonth(),
                todayDay = if (month == now) LocalDate.now(zone).dayOfMonth else null,
                volumeByDay = history.volumeByDay,
                maxDayVolume = history.volumeByDay.values.maxOrNull() ?: 0.0,
                monthSummary = when {
                    history.rows.isEmpty() -> "No workouts this month"
                    else -> "${history.rows.size} workouts · " +
                        "${Formats.volumeKg(history.rows.sumOf { it.volumeKg })} kg"
                },
                monthWorkouts = history.rows.size,
                monthVolumeKg = history.rows.sumOf { it.volumeKg },
            )
        )
    }

    private fun publish(base: HistoryUiState) {
        _ui.value = base.copy(
            selectedDay = selectedDay,
            rows = selectedDay?.let { day -> allRows.filter { it.day == day } } ?: allRows,
        )
    }
}
