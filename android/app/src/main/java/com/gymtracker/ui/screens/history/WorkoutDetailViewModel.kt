// Purpose: One past workout in full — per-exercise sets with PR flags and totals
// Inputs: workoutId from the nav route (SavedStateHandle); WorkoutRepository.workoutDetail
// Outputs: WorkoutDetailUiState via StateFlow
package com.gymtracker.ui.screens.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.gymtracker.data.WorkoutRepository
import com.gymtracker.domain.CalorieEstimator
import com.gymtracker.utils.Formats
import com.gymtracker.utils.PlateCalculator
import com.gymtracker.utils.TimeFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MuscleShare(val muscle: String, val sets: Int, val fraction: Float)

data class PrRowUi(val exerciseName: String, val oldValue: String, val newValue: String)

data class WorkoutDetailUiState(
    val title: String = "Workout",
    val dateLine: String = "",
    val durationText: String? = null,
    val totalSets: Int = 0,
    val totalVolume: String = "",
    val totalVolumeNumeric: Double = 0.0,
    val exerciseCount: Int = 0,
    val calories: Int = 0,
    val comment: String = "",
    val exercises: List<WorkoutRepository.DetailExercise> = emptyList(),
    val prCount: Int = 0,
    val editing: Boolean = false,
    val deleted: Boolean = false,
    val musclesSplit: List<MuscleShare> = emptyList(),
    val commentDialogOpen: Boolean = false,
    // true only for the one navigation straight out of finishing a session — drives the
    // celebratory header. Browsing the same workout later from History never sets this.
    val justFinished: Boolean = false,
    val prRows: List<PrRowUi> = emptyList(),
    val volumeDeltaPercent: Int? = null,
)

class WorkoutDetailViewModel(
    app: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(app) {

    private val repo = WorkoutRepository.get(app)
    val workoutId: String = savedStateHandle["workoutId"] ?: ""
    private val celebrate: Boolean = savedStateHandle["celebrate"] ?: false

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
        val durationMillis = detail.workout.endedAt
            ?.let { it - detail.workout.startedAt }
            ?.takeIf { it > 0 }
        val setSamples = detail.exercises.flatMap { ex ->
            ex.sets.map { CalorieEstimator.SetSample(ex.equipment, it.tag) }
        }
        val calories = CalorieEstimator.estimate(
            sets = setSamples,
            durationSeconds = (durationMillis ?: 0L) / 1000,
            bodyWeightKg = repo.profile().bodyWeightKg,
        )
        val totalSets = detail.totalSets.takeIf { it > 0 } ?: 1
        val musclesSplit = detail.exercises
            .flatMap { ex -> List(ex.sets.size) { ex.muscles.substringBefore("·").trim().ifEmpty { "Other" } } }
            .groupingBy { it }
            .eachCount()
            .entries.sortedByDescending { it.value }
            .map { (muscle, count) -> MuscleShare(muscle, count, count / totalSets.toFloat()) }
        val prRows = detail.prRows.map { row ->
            PrRowUi(
                exerciseName = row.exerciseName,
                oldValue = PlateCalculator.fmt(row.oldE1rm),
                newValue = PlateCalculator.fmt(row.newE1rm),
            )
        }
        val volumeDeltaPercent = repo.volumeDeltaVsLastSameNamed(
            workoutId = workoutId,
            name = detail.workout.name,
            currentVolumeKg = detail.totalVolumeKg,
        )
        _ui.value = WorkoutDetailUiState(
            title = detail.workout.name.ifBlank { "Workout" },
            dateLine = started.format(
                DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy · HH:mm", Locale.getDefault())
            ),
            durationText = durationMillis?.let(TimeFormat::clock),
            totalSets = detail.totalSets,
            totalVolume = Formats.volumeKg(detail.totalVolumeKg),
            totalVolumeNumeric = detail.totalVolumeKg,
            exerciseCount = detail.exercises.size,
            calories = calories,
            comment = detail.workout.comment,
            exercises = detail.exercises,
            prCount = detail.exercises.sumOf { ex -> ex.sets.count { it.isPr } },
            editing = editing,
            musclesSplit = musclesSplit,
            justFinished = celebrate,
            prRows = prRows,
            volumeDeltaPercent = volumeDeltaPercent,
        )
    }

    fun toggleEditing() = _ui.update { it.copy(editing = !it.editing) }

    fun openCommentDialog() = _ui.update { it.copy(commentDialogOpen = true) }
    fun closeCommentDialog() = _ui.update { it.copy(commentDialogOpen = false) }

    fun updateComment(text: String) = viewModelScope.launch {
        repo.updateWorkoutComment(workoutId, text)
        _ui.update { it.copy(commentDialogOpen = false) }
        load()
    }

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
