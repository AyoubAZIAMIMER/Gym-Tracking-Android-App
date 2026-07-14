// Purpose: Hardcoded stand-in for workout history + today's plan until the Room data layer lands
// Inputs: none (constants)
// Outputs: sample stats consumed by Home / Recovery / plan preview — replace with repositories later
package com.gymtracker.data

import java.time.DayOfWeek

object SampleData {

    const val workoutsThisWeek = 1
    const val weeklyGoal = 3
    const val streakWeeks = 3
    const val lastWorkoutDaysAgo = 2
    val doneWeekdays = setOf(DayOfWeek.WEDNESDAY)

    // --- today's plan (Fitbod-style preview on Home) --------------------------
    data class PlannedExercise(
        val name: String,
        val muscleGroup: String,
        val sets: Int,
        val reps: Int,
        val weightKg: Double,
    )

    const val todaysPlanName = "Push Day"
    const val todaysPlanMuscles = "Chest, Shoulders, Triceps"
    val todaysPlan = listOf(
        PlannedExercise("Bench Press (Barbell)", "Chest", 3, 8, 60.0),
        PlannedExercise("Incline Press (Dumbbell)", "Chest", 3, 10, 22.5),
        PlannedExercise("Lateral Raise (Dumbbell)", "Side Delts", 3, 12, 10.0),
        PlannedExercise("Triceps Pushdown (Cable)", "Triceps", 3, 12, 25.0),
    )

    // --- muscle recovery (Fitbod-style freshness) ------------------------------
    data class MuscleStatus(val muscle: String, val lastTrainedDaysAgo: Int) {
        // simple linear model: fully fresh 72h after training — replaced by real
        // volume-weighted recovery once history exists
        val freshnessPercent: Int
            get() = (lastTrainedDaysAgo * 24 * 100 / 72).coerceIn(0, 100)
    }

    val muscles = listOf(
        MuscleStatus("Chest", 4),
        MuscleStatus("Back", 2),
        MuscleStatus("Shoulders", 4),
        MuscleStatus("Biceps", 2),
        MuscleStatus("Triceps", 4),
        MuscleStatus("Quads", 1),
        MuscleStatus("Hamstrings", 1),
        MuscleStatus("Glutes", 1),
        MuscleStatus("Abs", 6),
        MuscleStatus("Calves", 6),
    )

    val freshMuscleCount: Int get() = muscles.count { it.freshnessPercent >= 80 }
}
