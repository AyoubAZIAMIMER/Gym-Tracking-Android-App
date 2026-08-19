// Purpose: Plan tab state — programs list, active program's next day, prebuilt templates
// Inputs: WorkoutRepository programs + ProgramTemplates
// Outputs: PlanUiState via StateFlow
package com.gymtracker.ui.screens.plan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gymtracker.data.ProgramTemplates
import com.gymtracker.data.WorkoutRepository
import com.gymtracker.data.db.ProgramEntity
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One row of the Plan tab's SESSIONS list, carrying its own state label. */
enum class SessionState { Logged, Today, Upcoming }

data class PlanSessionRow(
    val dayId: String,
    val name: String,
    val muscles: String,
    val eyebrow: String,        // "LOGGED · WED" / "TODAY" / "IN 2 DAYS"
    val exerciseCount: Int,
    val estimatedMinutes: Int?,
    val state: SessionState,
    /** Chronological position: negative = already logged, 0 = today, positive = upcoming. */
    val sortKey: Int = 0,
)

data class PlanUiState(
    val programs: List<ProgramEntity> = emptyList(),
    val activeProgramId: String? = null,
    val next: WorkoutRepository.NextProgramDay? = null,
    val templates: List<ProgramTemplates.Template> = ProgramTemplates.all,
    // the prototype's Plan leads with the same THIS WEEK strip Home shows
    val workoutsThisWeek: Int = 0,
    val weeklyGoal: Int = 0,
    val doneWeekdays: Set<DayOfWeek> = emptySet(),
    val activeProgramName: String? = null,
    // the active program's day list — the SESSIONS section
    val sessions: List<PlanSessionRow> = emptyList(),
)

class PlanViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = WorkoutRepository.get(app)
    private val _ui = MutableStateFlow(PlanUiState())
    val ui = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            repo.observePrograms().collect { programs ->
                val activeId = repo.activeProgramId()
                _ui.update {
                    it.copy(
                        programs = programs,
                        activeProgramId = activeId,
                        activeProgramName = programs.firstOrNull { p -> p.id == activeId }?.name,
                        next = repo.nextProgramDay(),
                    )
                }
                loadWeek()
            }
        }
    }

    fun refresh() = viewModelScope.launch {
        val activeId = repo.activeProgramId()
        _ui.update {
            it.copy(
                activeProgramId = activeId,
                activeProgramName = it.programs.firstOrNull { p -> p.id == activeId }?.name,
                next = repo.nextProgramDay(),
            )
        }
        loadWeek()
    }

    /** Week strip + the active program's full day list, both shown above the program directory. */
    private suspend fun loadWeek() {
        val stats = repo.homeStats()
        val profile = repo.profile()
        _ui.update {
            it.copy(
                workoutsThisWeek = stats.workoutsThisWeek,
                weeklyGoal = profile.weeklyGoal,
                doneWeekdays = stats.doneWeekdays,
                sessions = buildSessionRows(profile.weeklyGoal),
            )
        }
    }

    /**
     * The design labels each session with its distance from now: "LOGGED · WED", "TODAY",
     * "IN 2 DAYS". Programs here are a rotation pointer, not a weekday calendar, so the gap
     * between sessions is inferred from the weekly goal (a 4/week plan trains every ~2 days).
     */
    private suspend fun buildSessionRows(weeklyGoal: Int): List<PlanSessionRow> {
        val days = repo.programDays()
        if (days.isEmpty()) return emptyList()
        val nextId = repo.nextProgramDay()?.day?.id
        val pointer = days.indexOfFirst { it.day.id == nextId }.coerceAtLeast(0)
        val cadence = if (weeklyGoal > 0) (7f / weeklyGoal).roundToInt().coerceAtLeast(1) else 2
        val loggedRecently = repo.recentWorkouts(6)
            .filter { it.startedAtMillis >= System.currentTimeMillis() - 7L * 86_400_000L }

        return days.mapIndexed { index, session ->
            // rotation order != reading order: the list reads chronologically, logged → today →
            // upcoming, so a day before the pointer wraps to the far side of the cycle
            val raw = index - pointer
            val offset = raw
            val logged = loggedRecently.firstOrNull { it.name == session.day.name }
            val state = when {
                offset < 0 && logged != null -> SessionState.Logged
                offset == 0 -> SessionState.Today
                else -> SessionState.Upcoming
            }
            val eyebrow = when (state) {
                SessionState.Logged -> {
                    val day = LocalDate
                        .ofInstant(Instant.ofEpochMilli(logged!!.startedAtMillis), ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH))
                    "LOGGED · ${day.uppercase()}"
                }
                SessionState.Today -> "TODAY"
                SessionState.Upcoming -> {
                    val inDays = (if (offset > 0) offset else offset + days.size) * cadence
                    if (inDays == 1) "TOMORROW" else "IN $inDays DAYS"
                }
            }
            PlanSessionRow(
                dayId = session.day.id,
                name = session.day.name,
                eyebrow = eyebrow,
                muscles = session.detail?.exercises
                    ?.mapNotNull { it.exercise?.muscles }
                    ?.flatMap { it.split("·").map(String::trim) }
                    ?.distinct()
                    ?.take(3)
                    ?.joinToString(" · ")
                    .orEmpty(),
                exerciseCount = session.detail?.exercises?.size ?: 0,
                estimatedMinutes = repo.estimatedMinutesFor(session.day.name),
                state = state,
                sortKey = when (state) {
                    SessionState.Logged -> raw            // stays negative: sits above TODAY
                    SessionState.Today -> 0
                    SessionState.Upcoming -> if (raw > 0) raw else raw + days.size
                },
            )
        }.sortedBy { it.sortKey }
    }

    fun createProgram(name: String, onCreated: (String) -> Unit) = viewModelScope.launch {
        val id = repo.createProgram(name.ifBlank { "My program" })
        onCreated(id)
    }

    fun addTemplate(template: ProgramTemplates.Template) = viewModelScope.launch {
        val id = repo.createFromTemplate(template)
        // first program added becomes active automatically
        if (repo.activeProgramId() == null) repo.setActiveProgram(id)
        refresh()
    }

    fun setActive(programId: String?) {
        repo.setActiveProgram(programId)
        refresh()
    }
}
