// Purpose: UI-layer models for an in-progress workout session
// Inputs: none (pure data) — hydrated from Room via a repository once the data layer lands
// Outputs: state consumed by WorkoutSessionViewModel/Screen
package com.gymtracker.ui.screens.session

import com.gymtracker.domain.Progression

enum class SetTag(val letter: String, val label: String) {
    WARMUP("W", "Warmup"),
    FAILURE("F", "Failure"),
    FORCED("R", "Forced"),
    NEGATIVE("N", "Negative"),
    PAUSED("P", "Paused"),
    PARTIAL("L", "Partial"),
    EXPLOSIVE("X", "Explosive"),
    DROPSET("D", "Dropset");

    // Legacy table view cycles W → F → R → ... → D → (none); null re-enters at WARMUP.
    // The live session's Comment sheet sets a tag directly instead (tap to select/clear).
    fun next(): SetTag? = entries.getOrNull(ordinal + 1)

    companion object {
        fun fromLetter(letter: String?): SetTag? = entries.firstOrNull { it.letter == letter }
    }
}

// Optional effort rating, RPE 6-10 scale. Tap cycles (none) → 6 → 7 → 8 → 9 → 10 → (none).
fun nextRpe(current: Int?): Int? = when (current) {
    null -> 6
    10 -> null
    else -> current + 1
}

data class SessionSet(
    val id: Long,
    val prevWeightKg: Double? = null,
    val prevReps: Int? = null,
    // Double-progression prefill: when the plan calls a new load, accepting the hint
    // logs the suggestion, not last session's numbers
    val suggestedWeightKg: Double? = null,
    val suggestedReps: Int? = null,
    val weightText: String = "",
    val repsText: String = "",
    val tag: SetTag? = null,
    val rpe: Int? = null,            // optional effort rating, RPE 6-10
    val comment: String = "",        // free-text note on this one set
    val completed: Boolean = false,
    val isPr: Boolean = false,      // beat the all-time e1RM at the moment of logging
    val intensity: Float? = null,   // e1RM ÷ all-time best at logging (heat badge, Identity v5)
) {
    // Empty fields fall back to the suggestion, then to last session — "accept the hint"
    val effectiveWeightKg: Double? get() = weightText.toDoubleOrNull() ?: suggestedWeightKg ?: prevWeightKg
    val effectiveReps: Int? get() = repsText.toIntOrNull() ?: suggestedReps ?: prevReps
}

data class SessionExercise(
    val id: Long,
    val name: String,
    val muscleGroup: String,
    val dbExerciseId: String? = null, // Room id once the exercise exists in the database
    val supersetGroup: Int? = null, // exercises sharing a non-null id render as one superset
    val sets: List<SessionSet> = emptyList(),
    val note: String = "",                        // sticky machine note (seat, pin, grip)
    val plan: Progression.Plan? = null,           // loading call for this session
)

// Entry in the add-exercise picker: database exercises when available, starters otherwise
data class PickerItem(val dbExerciseId: String?, val name: String, val muscleGroup: String)

data class WorkoutSessionUiState(
    val workoutName: String = "Workout",
    val startedAtMillis: Long = System.currentTimeMillis(),
    val exercises: List<SessionExercise> = emptyList(),
    val activeSetId: Long? = null,          // auto-advance highlight
    val showExercisePicker: Boolean = false,
    val finished: Boolean = false,
    val savedWorkoutId: String? = null,     // set once finishWorkout() persists — null means
                                             // nothing was actually saved (e.g. zero sets)
    val sessionActive: Boolean = false,     // true once the session screen was opened; survives Back
    val barKg: Double = 20.0,               // from imported Progression prefs (plate calculator)
    val pickerItems: List<PickerItem> = emptyList(),
    val programDayId: String? = null,       // set when the session came from a program day
    // non-null while the picker is open in "swap this one" mode from the exercise menu
    val replacingExerciseId: Long? = null,
) {
    val completedSets: Int get() = exercises.sumOf { ex -> ex.sets.count { it.completed } }
    val totalSets: Int get() = exercises.sumOf { it.sets.size }
    val totalVolumeKg: Double
        get() = exercises.sumOf { ex ->
            ex.sets.filter { it.completed }
                .sumOf { (it.effectiveWeightKg ?: 0.0) * (it.effectiveReps ?: 0) }
        }
}

data class StarterExercise(val name: String, val muscleGroup: String)

// Hardcoded picker list until the Room data layer seeds the 300+ exercise library
val StarterExercises = listOf(
    StarterExercise("Bench Press (Barbell)", "Chest · Triceps"),
    StarterExercise("Incline Press (Dumbbell)", "Chest · Front Delts"),
    StarterExercise("Overhead Press (Barbell)", "Shoulders · Triceps"),
    StarterExercise("Lateral Raise (Dumbbell)", "Side Delts"),
    StarterExercise("Squat (Barbell)", "Quads · Glutes"),
    StarterExercise("Romanian Deadlift (Barbell)", "Hamstrings · Glutes"),
    StarterExercise("Deadlift (Barbell)", "Back · Glutes · Hamstrings"),
    StarterExercise("Leg Press", "Quads · Glutes"),
    StarterExercise("Pull-Up", "Lats · Biceps"),
    StarterExercise("Lat Pulldown (Cable)", "Lats · Biceps"),
    StarterExercise("Seated Row (Cable)", "Back · Biceps"),
    StarterExercise("Bicep Curl (Dumbbell)", "Biceps"),
    StarterExercise("Triceps Pushdown (Cable)", "Triceps"),
    StarterExercise("Skullcrusher (EZ-Bar)", "Triceps"),
    StarterExercise("Hip Thrust (Barbell)", "Glutes"),
    StarterExercise("Calf Raise (Machine)", "Calves"),
    StarterExercise("Ab Crunch (Machine)", "Abs"),
    StarterExercise("Face Pull (Cable)", "Rear Delts · Traps"),
)
