// Purpose: One past workout in full — per-exercise sets with PR flags and totals
// Inputs: workoutId from the nav route (SavedStateHandle); WorkoutRepository.workoutDetail
// Outputs: WorkoutDetailUiState via StateFlow
package com.gymtracker.ui.screens.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.gymtracker.data.WorkoutRepository
import com.gymtracker.utils.Formats
import com.gymtracker.utils.TimeFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WorkoutDetailUiState(
    val title: String = "Workout",
    val dateLine: String = "",
    val durationText: String? = null,
    val totalSets: Int = 0,
    val totalVolume: String = "",
    val exerciseCount: Int = 0,
    val comment: String = "",
    val exercises: List<WorkoutRepository.DetailExercise> = emptyList(),
    val prCount: Int = 0,
    val editing: Boolean = false,
    val deleted: Boolean = false,
)

class WorkoutDetailViewModel(
    app: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(app) {

    private val repo = WorkoutRepository.get(app)
    val workoutId: String = savedStateHandle["workoutId"] ?: ""

    private val _ui = MutableStateFlow(WorkoutDetailUiState())
    val ui = _ui.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val detail = repo.workoutDetail(workoutId) ?: run {
            // the workout this screen was showing no longer exists (deleted) — leave, don't
            // reset to a blank "Workout" title first
            _ui.update { it.copy(deleted = true) }
            return
        }
        val zone = ZoneId.systemDefault()
        val started = Instant.ofEpochMilli(detail.workout.startedAt).atZone(zone)
        val editing = _ui.value.editing
        _ui.value = WorkoutDetailUiState(
            title = detail.workout.name.ifBlank { "Workout" },
            dateLine = started.format(
                DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy · HH:mm", Locale.getDefault())
            ),
            durationText = detail.workout.endedAt
                ?.let { it - detail.workout.startedAt }
                ?.takeIf { it > 0 }
                ?.let(TimeFormat::clock),
            totalSets = detail.totalSets,
            totalVolume = Formats.volumeKg(detail.totalVolumeKg),
            exerciseCount = detail.exercises.size,
            comment = detail.workout.comment,
            exercises = detail.exercises,
            prCount = detail.exercises.sumOf { ex -> ex.sets.count { it.isPr } },
            editing = editing,
        )
    }

    fun toggleEditing() = _ui.update { it.copy(editing = !it.editing) }

    fun updateSet(setId: String, weightKg: Double?, reps: Int?) = viewModelScope.launch {
        repo.updateSet(setId, weightKg, reps)
        load()
    }

    fun removeSet(setId: String) = viewModelScope.launch {
        repo.deleteSet(setId)
        load()
    }

    fun removeExercise(exerciseId: String) = viewModelScope.launch {
        repo.removeExerciseFromWorkout(workoutId, exerciseId)
        load()
    }

    fun deleteWorkout() = viewModelScope.launch {
        repo.deleteWorkout(workoutId)
        _ui.update { it.copy(deleted = true) }
    }
}
