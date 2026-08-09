// Purpose: Room entities — the persistent core of Phase 1 (exercises / workouts / sets)
// Inputs: written by ProgressionImporter and session saves
// Outputs: read by repositories for Home stats, Recovery freshness, prefill and export
package com.gymtracker.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: String,      // UUID for customs, "pgn-<n>" for imported built-ins
    val name: String,
    val muscles: String = "",        // display string, e.g. "Chest · Triceps"
    val equipment: String = "",
    val isCustom: Boolean = false,
    val archived: Boolean = false,
    val description: String = "",    // short how-to; shown in the detail sheet (v2)
    val note: String = "",           // owner's sticky note: seat height, pin, grip (v3)
)

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey val id: String,
    val name: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val comment: String = "",
)

@Entity(
    tableName = "sets",
    indices = [Index("workoutId"), Index("exerciseId"), Index("completedAt")],
)
data class SetEntity(
    @PrimaryKey val id: String,
    val workoutId: String,
    val exerciseId: String,
    val completedAt: Long,
    val weightKg: Double? = null,    // null = bodyweight
    val reps: Int? = null,
    val tag: String? = null,         // SetTag letter (W/D/N/T/F)
    val orderInWorkout: Int = 0,
    val rpe: Float? = null,          // optional effort rating, 6-10
)

// --- programs (v2): program → ordered days → ordered exercises with targets -----

@Entity(tableName = "programs")
data class ProgramEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
)

@Entity(tableName = "program_days", indices = [Index("programId")])
data class ProgramDayEntity(
    @PrimaryKey val id: String,
    val programId: String,
    val name: String,
    val orderIdx: Int,
)

@Entity(tableName = "program_exercises", indices = [Index("dayId")])
data class ProgramExerciseEntity(
    @PrimaryKey val id: String,
    val dayId: String,
    val exerciseId: String,
    val orderIdx: Int,
    val targetSets: Int,
    val repMin: Int,
    val repMax: Int,
)
