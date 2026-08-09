// Purpose: Holds and mutates the in-progress workout session state
// Inputs: UI events from WorkoutSessionScreen; WorkoutRepository (template, prefs, persistence)
// Outputs: WorkoutSessionUiState via StateFlow; finished sessions saved to Room
package com.gymtracker.ui.screens.session

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gymtracker.data.WorkoutRepository
import com.gymtracker.service.RestTimerService
import com.gymtracker.utils.PlateCalculator
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WorkoutSessionViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = WorkoutRepository.get(app)
    private var nextId = 1_000L

    // live-PR baseline (exerciseId → all-time best e1RM), loaded once per session build
    private var bestE1rm: MutableMap<String, Double> = mutableMapOf()

    private val _ui = MutableStateFlow(sampleSession())
    val ui = _ui.asStateFlow()

    init {
        viewModelScope.launch { loadFreshSession(keepIfActive = true) }
    }

    private suspend fun refreshPrBaseline() {
        bestE1rm = repo.bestE1rmByExercise().toMutableMap()
    }

    /**
     * Called right before navigating to the session screen. An active session always
     * resumes untouched; otherwise the session is built from the given program day
     * (hints = that exercise's last logged sets) or from the latest workout.
     */
    fun prepareStart(programDayId: String? = null) {
        viewModelScope.launch {
            if (_ui.value.sessionActive) return@launch
            refreshPrBaseline()
            val template = programDayId?.let { repo.sessionTemplateFromDay(it) }
            if (template != null) {
                val pickerItems = repo.exercisesSnapshot()
                    .map { PickerItem(it.id, it.name, it.muscles) }
                    .ifEmpty { StarterExercises.map { PickerItem(null, it.name, it.muscleGroup) } }
                _ui.update {
                    fromTemplate(template).copy(
                        barKg = repo.barWeightKg(),
                        pickerItems = pickerItems,
                        programDayId = programDayId,
                        // template build races the screen's markSessionActive() — never
                        // let the async replace wipe a flag the screen already set
                        sessionActive = it.sessionActive,
                    )
                }
            } else {
                loadFreshSession(keepIfActive = false)
            }
        }
    }

    /** "Repeat this workout" from History — same rules as prepareStart. */
    fun prepareRepeat(workoutId: String) {
        viewModelScope.launch {
            if (_ui.value.sessionActive) return@launch
            val template = repo.templateFromWorkout(workoutId)
            if (template != null) {
                val pickerItems = repo.exercisesSnapshot()
                    .map { PickerItem(it.id, it.name, it.muscles) }
                    .ifEmpty { StarterExercises.map { PickerItem(null, it.name, it.muscleGroup) } }
                _ui.update {
                    fromTemplate(template).copy(
                        barKg = repo.barWeightKg(),
                        pickerItems = pickerItems,
                        sessionActive = it.sessionActive,   // same race as prepareStart
                    )
                }
            } else {
                loadFreshSession(keepIfActive = false)
            }
        }
    }

    // Rebuild the pre-filled session from the most recent workout in the database
    // ("repeat last workout" — hints come from what you actually lifted last time).
    private suspend fun loadFreshSession(keepIfActive: Boolean) {
        refreshPrBaseline()
        val template = repo.latestWorkoutTemplate()
        val pickerItems = repo.exercisesSnapshot()
            .map { PickerItem(it.id, it.name, it.muscles) }
            .ifEmpty { StarterExercises.map { PickerItem(null, it.name, it.muscleGroup) } }
        _ui.update { current ->
            if (keepIfActive && current.sessionActive) {
                current.copy(barKg = repo.barWeightKg(), pickerItems = pickerItems)
            } else {
                (template?.let(::fromTemplate) ?: sampleSession())
                    .copy(
                        barKg = repo.barWeightKg(),
                        pickerItems = pickerItems,
                        sessionActive = current.sessionActive,   // same race as prepareStart
                    )
            }
        }
    }

    private fun fromTemplate(template: WorkoutRepository.SessionTemplate): WorkoutSessionUiState {
        val exercises = template.exercises.map { ex ->
            // INCREASE/DELOAD plans repoint every working set's hint at the new load;
            // HOLD keeps last session's actuals as the hint (plan carries only the why)
            val suggestion = ex.plan?.takeIf { it.weightKg != null }
            SessionExercise(
                id = nextId++,
                name = ex.name,
                muscleGroup = ex.muscleGroup,
                dbExerciseId = ex.exerciseId,
                sets = ex.sets.map { s ->
                    SessionSet(
                        id = nextId++,
                        prevWeightKg = s.weightKg,
                        prevReps = s.reps,
                        suggestedWeightKg = suggestion?.weightKg,
                        suggestedReps = suggestion?.reps,
                    )
                }.ifEmpty { List(3) { SessionSet(id = nextId++) } },
                note = ex.note,
                plan = ex.plan,
            )
        }
        return WorkoutSessionUiState(
            workoutName = template.name,
            exercises = exercises,
            activeSetId = exercises.firstOrNull()?.sets?.firstOrNull()?.id,
        )
    }

    // --- set edits ------------------------------------------------------------

    /** Leave the forge with nothing logged — one-gesture entry made accidental sessions
     *  easy to start, so they must be as easy to abandon (nothing is saved). */
    fun discardSession() {
        _ui.update { it.copy(sessionActive = false, finished = true) }
    }

    /** Save the sticky machine note — state now, Room when the exercise exists there. */
    fun setExerciseNote(exerciseId: Long, note: String) {
        var dbId: String? = null
        _ui.update { st ->
            st.copy(exercises = st.exercises.map { ex ->
                if (ex.id != exerciseId) ex else ex.copy(note = note.trim()).also { dbId = it.dbExerciseId }
            })
        }
        dbId?.let { id -> viewModelScope.launch { repo.setExerciseNote(id, note) } }
    }

    fun setWeightText(exerciseId: Long, setId: Long, text: String) =
        updateSet(exerciseId, setId) { it.copy(weightText = sanitizeDecimal(text)) }

    fun setRepsText(exerciseId: Long, setId: Long, text: String) =
        updateSet(exerciseId, setId) { it.copy(repsText = text.filter(Char::isDigit).take(3)) }

    fun dragWeight(exerciseId: Long, setId: Long, steps: Int) =
        updateSet(exerciseId, setId) {
            // ±2.5 kg per step: smallest standard plate pair (owner-approved deviation from ±1)
            val base = it.weightText.toDoubleOrNull() ?: it.suggestedWeightKg ?: it.prevWeightKg ?: 0.0
            it.copy(weightText = PlateCalculator.fmt((base + steps * 2.5).coerceAtLeast(0.0)))
        }

    fun dragReps(exerciseId: Long, setId: Long, steps: Int) =
        updateSet(exerciseId, setId) {
            val base = it.repsText.toIntOrNull() ?: it.suggestedReps ?: it.prevReps ?: 0
            it.copy(repsText = (base + steps).coerceAtLeast(0).toString())
        }

    fun cycleTag(exerciseId: Long, setId: Long) =
        updateSet(exerciseId, setId) {
            val current = it.tag
            it.copy(tag = if (current == null) SetTag.WARMUP else current.next())
        }

    /** Direct pick from the 5-bar EFFORT selector (null clears). */
    fun setRpe(exerciseId: Long, setId: Long, rpe: Int?) =
        updateSet(exerciseId, setId) { it.copy(rpe = rpe) }

    fun cycleRpe(exerciseId: Long, setId: Long) =
        updateSet(exerciseId, setId) { it.copy(rpe = nextRpe(it.rpe)) }

    fun toggleCompleted(exerciseId: Long, setId: Long) {
        var completedNow = false
        _ui.update { st ->
            val exercises = st.exercises.map { ex ->
                if (ex.id != exerciseId) ex else ex.copy(sets = ex.sets.map { s ->
                    if (s.id != setId) s else if (!s.completed) {
                        completedNow = true
                        // materialize accepted hints so the logged values are explicit
                        val done = s.copy(
                            completed = true,
                            weightText = s.weightText.ifEmpty {
                                (s.suggestedWeightKg ?: s.prevWeightKg)?.let(PlateCalculator::fmt) ?: ""
                            },
                            repsText = s.repsText.ifEmpty {
                                (s.suggestedReps ?: s.prevReps)?.toString() ?: ""
                            },
                        )
                        // Forged Moment (rung 4): a working set that beats the all-time e1RM.
                        // First-ever lifts have no baseline and are no PR (same rule as analytics).
                        // isPr first: a PR raises the baseline, so its own intensity reads 1.0
                        val pr = isPr(ex.dbExerciseId, done)
                        done.copy(isPr = pr, intensity = intensityOf(ex.dbExerciseId, done))
                    } else {
                        s.copy(completed = false, isPr = false, intensity = null)
                    }
                })
            }
            st.copy(
                exercises = exercises,
                activeSetId = if (completedNow) nextIncompleteSetId(exercises, setId) else st.activeSetId,
            )
        }
        // completing a set auto-starts rest; duration comes from imported Progression prefs
        if (completedNow) RestTimerService.start(getApplication(), repo.restSeconds())
    }

    private fun isPr(dbExerciseId: String?, s: SessionSet): Boolean {
        if (dbExerciseId == null || s.tag == SetTag.WARMUP) return false
        val w = s.effectiveWeightKg ?: return false
        val r = s.effectiveReps ?: return false
        if (r <= 0) return false
        val best = bestE1rm[dbExerciseId] ?: return false
        val e1rm = com.gymtracker.utils.OneRM.estimate(w, r)
        if (e1rm > best) {
            bestE1rm[dbExerciseId] = e1rm   // the next PR must beat *this* set
            return true
        }
        return false
    }

    // Identity v5: how hot was this set relative to the all-time e1RM? Drives the
    // heat-tinted badge (steel warm-up → glowing top set); null when there's no baseline.
    private fun intensityOf(dbExerciseId: String?, s: SessionSet): Float? {
        if (dbExerciseId == null) return null
        val w = s.effectiveWeightKg ?: return null
        val r = s.effectiveReps ?: return null
        if (r <= 0) return null
        val best = bestE1rm[dbExerciseId] ?: return null
        return (com.gymtracker.utils.OneRM.estimate(w, r) / best).toFloat().coerceIn(0f, 1f)
    }

    private fun nextIncompleteSetId(exercises: List<SessionExercise>, afterSetId: Long): Long? {
        val flat = exercises.flatMap { it.sets }
        val index = flat.indexOfFirst { it.id == afterSetId }
        return flat.drop(index + 1).firstOrNull { !it.completed }?.id
            ?: flat.firstOrNull { !it.completed }?.id
    }

    // --- exercise list ----------------------------------------------------------

    fun addSet(exerciseId: Long) = _ui.update { st ->
        st.copy(exercises = st.exercises.map { ex ->
            if (ex.id != exerciseId) ex else {
                val last = ex.sets.lastOrNull()
                // the new set inherits the previous set's values as its hints
                ex.copy(
                    sets = ex.sets + SessionSet(
                        id = nextId++,
                        prevWeightKg = last?.effectiveWeightKg,
                        prevReps = last?.effectiveReps,
                    )
                )
            }
        })
    }

    fun addExercise(item: PickerItem) = _ui.update { st ->
        st.copy(
            showExercisePicker = false,
            exercises = st.exercises + SessionExercise(
                id = nextId++,
                name = item.name,
                muscleGroup = item.muscleGroup,
                dbExerciseId = item.dbExerciseId,
                sets = List(3) { SessionSet(id = nextId++) },
            ),
        )
    }

    fun removeExercise(exerciseId: Long) = _ui.update { st ->
        st.copy(exercises = st.exercises.filterNot { it.id == exerciseId })
    }

    // Progression-style warm-up ramp: bar×10 → 40%×8 → 60%×5 → 80%×3, rounded to 2.5 kg,
    // prepended as W-tagged sets. Target = first untagged working set's weight.
    fun generateWarmupSets(exerciseId: Long) = _ui.update { st ->
        st.copy(exercises = st.exercises.map { ex ->
            if (ex.id != exerciseId) return@map ex
            val bar = PlateCalculator.DEFAULT_BAR_KG
            val target = ex.sets.firstOrNull { it.tag == null }?.effectiveWeightKg
            if (target == null || target <= bar) return@map ex
            val ramp = listOf(bar to 10, roundTo2p5(target * 0.4) to 8, roundTo2p5(target * 0.6) to 5, roundTo2p5(target * 0.8) to 3)
                .map { (w, r) -> w.coerceAtLeast(bar) to r }
                .distinctBy { it.first }
                .filter { it.first < target }
            val warmups = ramp.map { (w, r) ->
                SessionSet(
                    id = nextId++,
                    weightText = PlateCalculator.fmt(w),
                    repsText = r.toString(),
                    tag = SetTag.WARMUP,
                )
            }
            ex.copy(sets = warmups + ex.sets)
        })
    }

    private fun roundTo2p5(x: Double): Double = (x / 2.5).roundToInt() * 2.5

    fun moveExercise(fromIndex: Int, toIndex: Int) = _ui.update { st ->
        if (fromIndex !in st.exercises.indices || toIndex !in st.exercises.indices) st
        else st.copy(
            exercises = st.exercises.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
        )
    }

    fun toggleSupersetWithNext(exerciseId: Long) = _ui.update { st ->
        val list = st.exercises
        val i = list.indexOfFirst { it.id == exerciseId }
        if (i == -1) return@update st
        val ex = list[i]
        val updated = when {
            ex.supersetGroup != null -> {
                // leaving a group; dissolve it entirely if only one member would remain
                val othersInGroup = list.count { it.id != exerciseId && it.supersetGroup == ex.supersetGroup }
                list.map {
                    when {
                        it.id == exerciseId -> it.copy(supersetGroup = null)
                        othersInGroup <= 1 && it.supersetGroup == ex.supersetGroup ->
                            it.copy(supersetGroup = null)
                        else -> it
                    }
                }
            }
            i < list.lastIndex -> {
                // join the next exercise's group, or open a fresh one for the pair
                val group = list[i + 1].supersetGroup
                    ?: (list.mapNotNull { it.supersetGroup }.maxOrNull() ?: 0) + 1
                list.mapIndexed { idx, e ->
                    if (idx == i || idx == i + 1) e.copy(supersetGroup = group) else e
                }
            }
            else -> list
        }
        st.copy(exercises = updated)
    }

    // --- sheets & finish --------------------------------------------------------

    fun showExercisePicker(show: Boolean) = _ui.update { it.copy(showExercisePicker = show) }

    fun showFinishSheet(show: Boolean) = _ui.update { it.copy(showFinishSheet = show) }

    // The VM is activity-scoped so an in-progress workout survives Back / app-switch;
    // Home shows "Resume workout" while sessionActive is true.
    fun markSessionActive() = _ui.update {
        if (it.sessionActive) it else it.copy(sessionActive = true, startedAtMillis = System.currentTimeMillis())
    }

    fun finishWorkout(comment: String) {
        RestTimerService.stop(getApplication())
        val state = _ui.value
        viewModelScope.launch {
            val toSave = state.exercises
                .map { ex ->
                    WorkoutRepository.SaveExercise(
                        dbExerciseId = ex.dbExerciseId,
                        name = ex.name,
                        muscleGroup = ex.muscleGroup,
                        sets = ex.sets.filter { it.completed }.map { s ->
                            WorkoutRepository.SaveSet(
                                weightKg = s.effectiveWeightKg,
                                reps = s.effectiveReps,
                                tagLetter = s.tag?.letter,
                                rpe = s.rpe?.toFloat(),
                            )
                        },
                    )
                }
                .filter { it.sets.isNotEmpty() }
            if (toSave.isNotEmpty()) {
                repo.saveSession(state.workoutName, state.startedAtMillis, comment, toSave)
                // program sessions rotate the active program to its next day
                if (state.programDayId != null) repo.advanceProgramPointer()
            }
            _ui.update { it.copy(showFinishSheet = false, finished = true) }
        }
    }

    /** Called after navigation leaves the finished session: reset for the next workout. */
    fun consumeFinished() {
        viewModelScope.launch { loadFreshSession(keepIfActive = false) }
    }

    // --- helpers ---------------------------------------------------------------

    private fun updateSet(exerciseId: Long, setId: Long, transform: (SessionSet) -> SessionSet) =
        _ui.update { st ->
            st.copy(exercises = st.exercises.map { ex ->
                if (ex.id != exerciseId) ex
                else ex.copy(sets = ex.sets.map { if (it.id == setId) transform(it) else it })
            })
        }

    private fun sanitizeDecimal(text: String): String {
        val filtered = text.filter { it.isDigit() || it == '.' }.take(6)
        val firstDot = filtered.indexOf('.')
        // keep at most one decimal point
        return if (firstDot == -1) filtered
        else filtered.substring(0, firstDot + 1) + filtered.substring(firstDot + 1).replace(".", "")
    }

    // Sample "Push Day" with fake last-session hints until the Room data layer exists
    private fun sampleSession(): WorkoutSessionUiState {
        fun set(w: Double?, r: Int?) = SessionSet(id = nextId++, prevWeightKg = w, prevReps = r)
        val exercises = listOf(
            SessionExercise(
                id = nextId++, name = "Bench Press (Barbell)", muscleGroup = "Chest · Triceps",
                sets = listOf(set(60.0, 8), set(60.0, 8), set(57.5, 10)),
            ),
            SessionExercise(
                id = nextId++, name = "Incline Press (Dumbbell)", muscleGroup = "Chest · Front Delts",
                sets = listOf(set(22.5, 10), set(22.5, 10), set(20.0, 12)),
            ),
            SessionExercise(
                id = nextId++, name = "Triceps Pushdown (Cable)", muscleGroup = "Triceps",
                sets = listOf(set(25.0, 12), set(25.0, 12), set(25.0, 12)),
            ),
        )
        return WorkoutSessionUiState(
            workoutName = "Push Day",
            exercises = exercises,
            activeSetId = exercises.first().sets.first().id,
        )
    }
}
