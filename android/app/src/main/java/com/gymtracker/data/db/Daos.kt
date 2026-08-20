// Purpose: Room DAOs for exercises / workouts / sets
// Inputs: SQL over the entities
// Outputs: Flows for UI + suspend calls for import/export/save
package com.gymtracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ExerciseEntity>)

    // import path: never clobber rows the user has renamed/merged since
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(items: List<ExerciseEntity>)

    @Query("SELECT * FROM exercises WHERE archived = 0 ORDER BY name")
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises")
    suspend fun getAll(): List<ExerciseEntity>

    @Query("DELETE FROM exercises WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE exercises SET note = :note WHERE id = :id")
    suspend fun updateNote(id: String, note: String)

    @Query("SELECT COUNT(*) FROM exercises")
    fun observeCount(): Flow<Int>
}

@Dao
interface ProgramDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPrograms(items: List<ProgramEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDays(items: List<ProgramDayEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExercises(items: List<ProgramExerciseEntity>)

    @Query("SELECT * FROM programs ORDER BY createdAt")
    fun observePrograms(): Flow<List<ProgramEntity>>

    @Query("SELECT * FROM programs WHERE id = :id")
    suspend fun program(id: String): ProgramEntity?

    @Query("SELECT * FROM program_days WHERE programId = :programId ORDER BY orderIdx")
    suspend fun days(programId: String): List<ProgramDayEntity>

    @Query("SELECT * FROM program_days WHERE id = :dayId")
    suspend fun day(dayId: String): ProgramDayEntity?

    @Query("SELECT * FROM program_exercises WHERE dayId = :dayId ORDER BY orderIdx")
    suspend fun dayExercises(dayId: String): List<ProgramExerciseEntity>

    @Query("DELETE FROM programs WHERE id = :id")
    suspend fun deleteProgram(id: String)

    @Query("DELETE FROM program_days WHERE programId = :programId")
    suspend fun deleteDaysOf(programId: String)

    @Query("DELETE FROM program_days WHERE id = :dayId")
    suspend fun deleteDay(dayId: String)

    @Query("DELETE FROM program_exercises WHERE dayId = :dayId")
    suspend fun deleteExercisesOf(dayId: String)

    @Query("DELETE FROM program_exercises WHERE id = :id")
    suspend fun deleteProgramExercise(id: String)

    // keeps program day lists intact when a placeholder is merged into a named exercise
    @Query("UPDATE program_exercises SET exerciseId = :toId WHERE exerciseId = :fromId")
    suspend fun reassignExercise(fromId: String, toId: String)

    // one row's exercise, from the editor's own Replace action — distinct from
    // reassignExercise above, which rewrites every row sharing the old exercise
    @Query("UPDATE program_exercises SET exerciseId = :toId WHERE id = :id")
    suspend fun replaceProgramExercise(id: String, toId: String)

    @Query("UPDATE program_exercises SET targetSets = :sets, repMin = :repMin, repMax = :repMax WHERE id = :id")
    suspend fun updateProgramExerciseTarget(id: String, sets: Int, repMin: Int, repMax: Int)

    @Query("UPDATE program_exercises SET supersetGroup = :group WHERE id = :id")
    suspend fun setProgramSupersetGroup(id: String, group: Int?)
}

@Dao
interface WorkoutDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<WorkoutEntity>)

    @Query("SELECT * FROM workouts ORDER BY startedAt DESC LIMIT 1")
    suspend fun latest(): WorkoutEntity?

    @Query("SELECT * FROM workouts ORDER BY startedAt DESC LIMIT :limit")
    suspend fun latestN(limit: Int): List<WorkoutEntity>

    /** Past runs of the same day, used to estimate "about N min" on Home. */
    @Query(
        "SELECT * FROM workouts WHERE name = :name AND endedAt IS NOT NULL " +
            "ORDER BY startedAt DESC LIMIT :limit"
    )
    suspend fun latestNamed(name: String, limit: Int): List<WorkoutEntity>

    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun byId(id: String): WorkoutEntity?

    @Query("SELECT * FROM workouts WHERE startedAt >= :since ORDER BY startedAt DESC")
    suspend fun since(since: Long): List<WorkoutEntity>

    @Query("SELECT * FROM workouts WHERE startedAt >= :start AND startedAt < :end ORDER BY startedAt DESC")
    suspend fun between(start: Long, end: Long): List<WorkoutEntity>

    @Query("SELECT MIN(startedAt) FROM workouts")
    suspend fun earliestStart(): Long?

    @Query("SELECT startedAt FROM workouts ORDER BY startedAt DESC")
    suspend fun allStartTimes(): List<Long>

    @Query("SELECT * FROM workouts")
    suspend fun getAll(): List<WorkoutEntity>

    @Query("SELECT COUNT(*) FROM workouts")
    fun observeCount(): Flow<Int>

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteWorkout(id: String)
}

@Dao
interface SetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<SetEntity>)

    // import path: re-importing a backup must not undo CSV-naming merges
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(items: List<SetEntity>)

    // merge an imported placeholder's history onto a named exercise
    @Query("UPDATE sets SET exerciseId = :toId WHERE exerciseId = :fromId")
    suspend fun reassignExercise(fromId: String, toId: String): Int

    @Query("SELECT COUNT(*) FROM sets WHERE exerciseId = :exerciseId")
    suspend fun countForExercise(exerciseId: String): Int

    @Query("SELECT * FROM sets WHERE workoutId = :workoutId ORDER BY orderInWorkout")
    suspend fun forWorkout(workoutId: String): List<SetEntity>

    @Query("SELECT * FROM sets WHERE workoutId IN (:workoutIds)")
    suspend fun forWorkouts(workoutIds: List<String>): List<SetEntity>

    // PR baseline for the workout-detail view: everything lifted before that session
    @Query("SELECT * FROM sets WHERE exerciseId IN (:exerciseIds) AND completedAt < :before")
    suspend fun forExercisesBefore(exerciseIds: List<String>, before: Long): List<SetEntity>

    @Query("SELECT * FROM sets WHERE completedAt >= :since")
    suspend fun since(since: Long): List<SetEntity>

    @Query("SELECT * FROM sets WHERE exerciseId = :exerciseId ORDER BY completedAt DESC LIMIT 60")
    suspend fun recentForExercise(exerciseId: String): List<SetEntity>

    @Query("SELECT * FROM sets")
    suspend fun getAll(): List<SetEntity>

    @Query("SELECT COUNT(*) FROM sets")
    fun observeCount(): Flow<Int>

    @Query("DELETE FROM sets WHERE workoutId = :workoutId")
    suspend fun deleteSetsOf(workoutId: String)

    @Query("DELETE FROM sets WHERE id = :id")
    suspend fun deleteSet(id: String)

    @Query("DELETE FROM sets WHERE workoutId = :workoutId AND exerciseId = :exerciseId")
    suspend fun deleteSetsOfExercise(workoutId: String, exerciseId: String)

    @Query("UPDATE sets SET weightKg = :weightKg, reps = :reps WHERE id = :id")
    suspend fun updateSet(id: String, weightKg: Double?, reps: Int?)
}
