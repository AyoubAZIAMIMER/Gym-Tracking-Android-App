// Purpose: Home dashboard state — active-program day > repeat-last > sample, plus real stats
// Inputs: WorkoutRepository (stats, next program day, latest-workout template)
// Outputs: HomeUiState via StateFlow (reloads on workout-count changes and on refresh())
package com.gymtracker.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gymtracker.data.ProgressionImporter
import com.gymtracker.data.SampleData
import com.gymtracker.data.WorkoutRepository
import com.gymtracker.utils.Formats
import com.gymtracker.utils.PlateCalculator
import java.time.DayOfWeek
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// freshness = primary muscle's recovery % (heat-tints the row dot, Identity v5)
data class PlanRow(val name: String, val muscle: String, val detail: String, val freshness: Int? = null)

data class HomeUiState(
    val hasData: Boolean = false,
    val workoutsThisWeek: Int = SampleData.workoutsThisWeek,
    val weeklyGoal: Int = SampleData.weeklyGoal,
    val streakWeeks: Int = SampleData.streakWeeks,
    val lastWorkoutDaysAgo: Int? = SampleData.lastWorkoutDaysAgo,
    val doneWeekdays: Set<DayOfWeek> = SampleData.doneWeekdays,
    val planLabel: String = "UP NEXT",
    val planTitle: String = SampleData.todaysPlanName,
    val planMuscles: String = SampleData.todaysPlanMuscles,
    val planRows: List<PlanRow> = SampleData.todaysPlan.map {
        PlanRow(it.name, it.muscleGroup, "${it.sets}×${it.reps} · ${PlateCalculator.fmt(it.weightKg)} kg")
    },
    val programDayId: String? = null,   // non-null when the plan card shows the active program
    val userName: String = "",
    val needsProfile: Boolean = false,  // true until the first-run profile is saved
    val todayForged: WorkoutRepository.TodayForged? = null, // non-null → Now Card leads with it
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = WorkoutRepository.get(app)
    private val _ui = MutableStateFlow(HomeUiState())
    val ui = _ui.asStateFlow()

    init {
        viewModelScope.launch { repo.workoutCount.collect { reload() } }
    }

    /** Re-check the active program when the screen comes back into view. */
    fun refresh() {
        viewModelScope.launch { reload() }
    }

    fun saveProfile(name: String, weightKg: Double?, heightCm: Int?, weeklyGoal: Int) {
        repo.saveProfile(name, weightKg, heightCm, weeklyGoal)
        refresh()
    }

    private suspend fun reload() {
        val stats = repo.homeStats()
        val profile = repo.profile()
        // Identity v5: today's plan shows each exercise's readiness as a heat dot
        val freshness =
            if (stats.hasData) repo.muscleFreshness().associate { it.muscle to it.freshnessPercent }
            else emptyMap()
        // UX v6 Now Card: a session finished today outranks the plan as Home's hero
        val forged = if (stats.hasData && stats.lastWorkoutDaysAgo == 0) repo.todayForged() else null
        fun primaryFreshness(muscles: String?): Int? = muscles
            ?.split("·")?.map(String::trim)
            ?.firstNotNullOfOrNull { m -> ProgressionImporter.canonicalMuscle(m)?.let(freshness::get) }
        fun HomeUiState.withProfile() = copy(
            userName = profile.name,
            weeklyGoal = profile.weeklyGoal,
            needsProfile = !repo.isProfileSet(),
        )

        // 1st priority: next day of the active program
        val next = repo.nextProgramDay()
        val programDetail = next?.let { repo.dayDetail(it.day.id) }
        if (next != null && programDetail != null && programDetail.exercises.isNotEmpty()) {
            _ui.value = statsPart(stats).copy(
                planLabel = "PROGRAM · ${next.programName.uppercase()}",
                planTitle = programDetail.day.name,
                planMuscles = programDetail.exercises
                    .mapNotNull { it.exercise?.muscles }
                    .flatMap { it.split("·").map(String::trim) }
                    .mapNotNull(ProgressionImporter::canonicalMuscle)
                    .distinct()
                    .joinToString(", "),
                planRows = programDetail.exercises.map { pe ->
                    PlanRow(
                        name = pe.exercise?.name ?: "Unknown exercise",
                        muscle = pe.exercise?.muscles.orEmpty(),
                        detail = "${pe.row.targetSets} × ${Formats.repRange(pe.row.repMin, pe.row.repMax)}",
                        freshness = primaryFreshness(pe.exercise?.muscles),
                    )
                },
                programDayId = programDetail.day.id,
                todayForged = forged,
            ).withProfile()
            return
        }

        // 2nd: repeat the latest logged workout
        if (stats.hasData) {
            val template = repo.latestWorkoutTemplate()
            if (template != null) {
                _ui.value = statsPart(stats).copy(
                    planLabel = "REPEAT LAST",
                    planTitle = template.name.ifBlank { "Workout" },
                    planMuscles = template.exercises
                        .flatMap { it.muscleGroup.split("·").map(String::trim) }
                        .mapNotNull(ProgressionImporter::canonicalMuscle)
                        .distinct()
                        .joinToString(", ")
                        .ifEmpty { "Based on your last session" },
                    planRows = template.exercises.map { ex ->
                        val top = ex.sets.maxByOrNull { it.weightKg ?: 0.0 }
                        val weight = top?.weightKg
                        val reps = top?.reps
                        PlanRow(
                            name = ex.name,
                            muscle = ex.muscleGroup,
                            detail = when {
                                weight != null -> "${ex.sets.size}×${reps ?: "–"} · ${PlateCalculator.fmt(weight)} kg"
                                reps != null -> "${ex.sets.size}×$reps"
                                else -> "${ex.sets.size} sets"
                            },
                            freshness = primaryFreshness(ex.muscleGroup),
                        )
                    },
                    todayForged = forged,
                ).withProfile()
                return
            }
        }

        // fallback: sample
        _ui.value = HomeUiState().withProfile()
    }

    private fun statsPart(stats: WorkoutRepository.HomeStats): HomeUiState =
        if (stats.hasData) {
            HomeUiState(
                hasData = true,
                workoutsThisWeek = stats.workoutsThisWeek,
                weeklyGoal = SampleData.weeklyGoal,
                streakWeeks = stats.streakWeeks,
                lastWorkoutDaysAgo = stats.lastWorkoutDaysAgo,
                doneWeekdays = stats.doneWeekdays,
            )
        } else {
            HomeUiState()
        }
}
