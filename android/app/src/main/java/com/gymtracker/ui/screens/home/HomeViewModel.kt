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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// freshness = primary muscle's recovery % (heat-tints the row dot, Identity v5)
data class PlanRow(val name: String, val muscle: String, val detail: String, val freshness: Int? = null)

// Home's "NEXT UP" row: an upcoming program day, not yet started
data class UpcomingRow(val name: String, val muscles: String, val dayLabel: String)

// Home's "RECENT" row: the most recently logged workout
data class RecentRow(
    val id: String,
    val name: String,
    val dayLabel: String,
    val durationMin: Int?,
    val volumeKg: Double,
)

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
    val readinessLabel: String? = null, // "{muscle} {ready|worn|hot|spent}" — today's least-fresh target
    val readinessFreshness: Float? = null, // 0f (spent) .. 1f (ready) — colors the label via heat.at()
    val estimatedMinutes: Int? = null,  // "· about 58 min" — null until this day has history
    val upcoming: List<UpcomingRow> = emptyList(),  // NEXT UP
    val recent: List<RecentRow> = emptyList(),      // RECENT (prototype shows two)
    // Dynamic Hub's "Ready to train" rail — most-recovered muscle groups first
    val readyToTrain: List<WorkoutRepository.MuscleFreshness> = emptyList(),
)

/** muscleFreshness() only looks this far back; beyond it a group counts as fully cooled. */
private const val FRESHNESS_LOOKBACK_DAYS = 14

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

    /** Same as [refresh], but awaitable — callers that must read [ui]'s value immediately
     *  afterward (the onboarding redirect) need the reload to have actually landed first. */
    suspend fun awaitRefresh() = reload()

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
        // Today block's readiness tag: the least-fresh of today's target muscles names the
        // limiting factor ("QUADS WORN"), bucketed onto the same ready/worn/hot/spent
        // vocabulary HeatScale.at() already uses (Color.kt), so no new copy is invented.
        fun readinessTag(canonicalMuscles: List<String>): Pair<String, Float>? {
            val (muscle, pct) = canonicalMuscles.mapNotNull { m -> freshness[m]?.let { m to it } }
                .minByOrNull { it.second } ?: return null
            val word = when {
                pct >= 90 -> "ready"
                pct >= 68 -> "worn"
                pct >= 45 -> "hot"
                else -> "spent"
            }
            return "${muscle.uppercase()} ${word.uppercase()}" to (pct / 100f)
        }
        // NEXT UP / RECENT are independent of which plan branch resolves below
        val upcomingRows = if (stats.hasData) {
            repo.upcomingProgramDays().mapIndexed { i, u ->
                UpcomingRow(
                    name = u.day.name,
                    muscles = u.detail?.exercises
                        ?.mapNotNull { it.exercise?.muscles }
                        ?.flatMap { it.split("·").map(String::trim) }
                        ?.mapNotNull(ProgressionImporter::canonicalMuscle)
                        ?.distinct()
                        ?.joinToString(", ")
                        .orEmpty(),
                    dayLabel = if (i == 0) "Next" else "Then",
                )
            }
        } else emptyList()
        // "Ready to train" must answer "what can I hit today?", so it spans ALL canonical
        // groups — one absent from muscleFreshness() hasn't been trained inside the lookback
        // window, which means recovered, not unknown. Without this the rail showed only the
        // muscles you just destroyed, at 0%, under a heading that said they were ready.
        val ready = if (stats.hasData) {
            val trained = repo.muscleFreshness().associateBy { it.muscle }
            ProgressionImporter.CANONICAL_MUSCLES
                .map { group ->
                    trained[group] ?: WorkoutRepository.MuscleFreshness(
                        muscle = group,
                        lastTrainedDaysAgo = FRESHNESS_LOOKBACK_DAYS,
                        freshnessPercent = 100,
                    )
                }
                .sortedByDescending { it.freshnessPercent }
                .take(6)
        } else emptyList()
        val recentRows = if (stats.hasData) {
            repo.recentWorkouts().map { r ->
                RecentRow(
                    id = r.id,
                    name = r.name,
                    dayLabel = LocalDate.ofInstant(Instant.ofEpochMilli(r.startedAtMillis), ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)),
                    durationMin = r.durationMin,
                    volumeKg = r.volumeKg,
                )
            }
        } else emptyList()

        // 1st priority: next day of the active program
        val next = repo.nextProgramDay()
        val programDetail = next?.let { repo.dayDetail(it.day.id) }
        if (next != null && programDetail != null && programDetail.exercises.isNotEmpty()) {
            val muscles = programDetail.exercises
                .mapNotNull { it.exercise?.muscles }
                .flatMap { it.split("·").map(String::trim) }
                .mapNotNull(ProgressionImporter::canonicalMuscle)
                .distinct()
            val readiness = readinessTag(muscles)
            _ui.value = statsPart(stats).copy(
                planLabel = "PROGRAM · ${next.programName.uppercase()}",
                planTitle = programDetail.day.name,
                planMuscles = muscles.joinToString(", "),
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
                readinessLabel = readiness?.first,
                readinessFreshness = readiness?.second,
                estimatedMinutes = repo.estimatedMinutesFor(programDetail.day.name),
                upcoming = upcomingRows,
                recent = recentRows,
                readyToTrain = ready,
            ).withProfile()
            return
        }

        // 2nd: repeat the latest logged workout
        if (stats.hasData) {
            val template = repo.latestWorkoutTemplate()
            if (template != null) {
                val muscles = template.exercises
                    .flatMap { it.muscleGroup.split("·").map(String::trim) }
                    .mapNotNull(ProgressionImporter::canonicalMuscle)
                    .distinct()
                val readiness = readinessTag(muscles)
                _ui.value = statsPart(stats).copy(
                    planLabel = "REPEAT LAST",
                    planTitle = template.name.ifBlank { "Workout" },
                    planMuscles = muscles.joinToString(", ").ifEmpty { "Based on your last session" },
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
                    readinessLabel = readiness?.first,
                    readinessFreshness = readiness?.second,
                    estimatedMinutes = repo.estimatedMinutesFor(template.name),
                    upcoming = upcomingRows,
                    recent = recentRows,
                    readyToTrain = ready,
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
            // A confirmed "nothing logged yet" is real information, not a loading placeholder —
            // it must not read as the sample week (Wed done, 1/3) to a genuinely fresh account.
            HomeUiState(workoutsThisWeek = 0, doneWeekdays = emptySet())
        }
}
