// Purpose: Per-exercise analytics state (e1RM/volume series, badges, recent sessions)
// Inputs: exerciseId from the nav route (SavedStateHandle); AnalyticsEngine over a DB snapshot
// Outputs: ExerciseStatsUiState via StateFlow
package com.gymtracker.ui.screens.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.gymtracker.data.WorkoutRepository
import com.gymtracker.domain.AnalyticsEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ExerciseStatsUiState(
    val name: String = "",
    val muscles: String = "",
    val analytics: AnalyticsEngine.ExerciseAnalytics? = null,
)

class ExerciseStatsViewModel(
    app: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(app) {

    private val repo = WorkoutRepository.get(app)
    private val exerciseId: String = savedStateHandle["exerciseId"] ?: ""

    private val _ui = MutableStateFlow(ExerciseStatsUiState())
    val ui = _ui.asStateFlow()

    init {
        viewModelScope.launch { repo.setCount.collect { reload() } }
    }

    private suspend fun reload() {
        val state = withContext(Dispatchers.Default) {
            val snap = repo.analyticsSnapshot()
            val exercise = snap.exercises.firstOrNull { it.id == exerciseId }
            ExerciseStatsUiState(
                name = exercise?.name ?: "Exercise",
                muscles = exercise?.muscles.orEmpty(),
                analytics = AnalyticsEngine.forExercise(exerciseId, snap.sets, snap.workouts),
            )
        }
        _ui.value = state
    }
}
