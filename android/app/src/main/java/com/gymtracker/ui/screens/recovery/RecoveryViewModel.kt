// Purpose: Recovery state — muscle freshness from real training history, sample fallback
// Inputs: WorkoutRepository.muscleFreshness() + homeStats()
// Outputs: RecoveryUiState via StateFlow (auto-reloads when the workout count changes)
package com.gymtracker.ui.screens.recovery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gymtracker.data.SampleData
import com.gymtracker.data.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RecoveryUiState(
    val items: List<WorkoutRepository.MuscleFreshness> = emptyList(),
    val daysSinceLast: Int? = null,
    val isSample: Boolean = true,
) {
    val freshCount: Int get() = items.count { it.freshnessPercent >= 80 }
    val freshnessByMuscle: Map<String, Int> get() = items.associate { it.muscle to it.freshnessPercent }
}

class RecoveryViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = WorkoutRepository.get(app)
    private val _ui = MutableStateFlow(RecoveryUiState())
    val ui = _ui.asStateFlow()

    init {
        viewModelScope.launch { repo.workoutCount.collect { reload() } }
    }

    private suspend fun reload() {
        val real = repo.muscleFreshness()
        _ui.value = if (real.isEmpty()) {
            RecoveryUiState(
                items = SampleData.muscles.map {
                    WorkoutRepository.MuscleFreshness(it.muscle, it.lastTrainedDaysAgo, it.freshnessPercent)
                }.sortedBy { it.freshnessPercent },
                daysSinceLast = SampleData.lastWorkoutDaysAgo,
                isSample = true,
            )
        } else {
            RecoveryUiState(
                items = real,
                daysSinceLast = repo.homeStats().lastWorkoutDaysAgo,
                isSample = false,
            )
        }
    }
}
