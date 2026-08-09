// Purpose: Recovery state — muscle freshness from real training history, sample fallback,
//          plus the prototype's single weighted readiness score and its "what to train" call
// Inputs: WorkoutRepository.muscleFreshness() + homeStats() + nextProgramDay()
// Outputs: RecoveryUiState via StateFlow (auto-reloads when the workout count changes)
package com.gymtracker.ui.screens.recovery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gymtracker.data.ProgressionImporter
import com.gymtracker.data.SampleData
import com.gymtracker.data.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class RecoveryUiState(
    val items: List<WorkoutRepository.MuscleFreshness> = emptyList(),
    val daysSinceLast: Int? = null,
    val isSample: Boolean = true,
    /** 0..100, weighted by muscle size — the prototype's hero numeral. */
    val readinessPercent: Int = 0,
    val readinessNote: String = "",
    /** The prototype's ember call-to-action strip; null when there's nothing to say. */
    val callTitle: String? = null,
    val callBody: String? = null,
    val callDayId: String? = null,
) {
    val freshCount: Int get() = items.count { it.freshnessPercent >= 80 }
    val freshnessByMuscle: Map<String, Int> get() = items.associate { it.muscle to it.freshnessPercent }
}

/** muscleFreshness()'s lookback; past it, a group counts as fully cooled. */
private const val FRESHNESS_LOOKBACK_DAYS = 14

class RecoveryViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = WorkoutRepository.get(app)
    private val _ui = MutableStateFlow(RecoveryUiState())
    val ui = _ui.asStateFlow()

    init {
        viewModelScope.launch { repo.workoutCount.collect { reload() } }
    }

    fun refresh() = viewModelScope.launch { reload() }

    private suspend fun reload() {
        val real = repo.muscleFreshness()
        val items = if (real.isEmpty()) {
            SampleData.muscles.map {
                WorkoutRepository.MuscleFreshness(it.muscle, it.lastTrainedDaysAgo, it.freshnessPercent)
            }.sortedBy { it.freshnessPercent }
        } else {
            // muscleFreshness() only reports groups trained inside its lookback window. A group
            // it omits is fully recovered, not missing — without this, training only legs read
            // as "0% ready to train" for the whole body.
            val trained = real.associateBy { it.muscle }
            ProgressionImporter.CANONICAL_MUSCLES
                .map { group ->
                    trained[group] ?: WorkoutRepository.MuscleFreshness(
                        muscle = group,
                        lastTrainedDaysAgo = FRESHNESS_LOOKBACK_DAYS,
                        freshnessPercent = 100,
                    )
                }
                .sortedBy { it.freshnessPercent }
        }

        val readiness = weightedReadiness(items)
        val freshest = items.maxByOrNull { it.freshnessPercent }
        val sorest = items.minByOrNull { it.freshnessPercent }
        val next = repo.nextProgramDay()

        _ui.value = RecoveryUiState(
            items = items,
            daysSinceLast = if (real.isEmpty()) SampleData.lastWorkoutDaysAgo
            else repo.homeStats().lastWorkoutDaysAgo,
            isSample = real.isEmpty(),
            readinessPercent = readiness,
            readinessNote = when {
                readiness >= 85 -> "Everything has cooled. Pick the heaviest thing you own."
                readiness >= 60 -> "Most of you is ready — train around what's still hot."
                readiness >= 35 -> "Half-recovered. Keep the volume honest today."
                else -> "Still glowing. A rest day is the training."
            },
            callTitle = freshest?.let { "${it.muscle.replaceFirstChar(Char::uppercase)} are prime" },
            callBody = buildString {
                next?.let { append("Train ${it.day.name} today") } ?: append("Train what's cooled")
                sorest?.takeIf { it.freshnessPercent < 60 }?.let {
                    append(" — ${it.muscle.lowercase()} still cooling.")
                } ?: append(".")
            },
            callDayId = next?.day?.id,
        )
    }

    /**
     * One number for "how ready am I". A plain average would let calves outvote the back, so
     * each group carries a rough share of trainable muscle mass (the prototype labels this
     * "WEIGHTED BY MUSCLE SIZE"). Unlisted groups fall back to 1.0.
     */
    private fun weightedReadiness(items: List<WorkoutRepository.MuscleFreshness>): Int {
        if (items.isEmpty()) return 0
        val weighted = items.sumOf { MuscleMass[it.muscle.lowercase()] ?: 1.0 }
        if (weighted <= 0.0) return 0
        val score = items.sumOf {
            (MuscleMass[it.muscle.lowercase()] ?: 1.0) * it.freshnessPercent
        }
        return (score / weighted).roundToInt().coerceIn(0, 100)
    }

    private companion object {
        val MuscleMass = mapOf(
            "quads" to 2.4, "hamstrings" to 1.8, "glutes" to 2.0, "back" to 2.4,
            "chest" to 1.6, "shoulders" to 1.2, "delts" to 1.2, "triceps" to 0.9,
            "biceps" to 0.7, "calves" to 0.7, "abs" to 0.6, "core" to 0.6,
            "forearms" to 0.4, "traps" to 0.9,
        )
    }
}
