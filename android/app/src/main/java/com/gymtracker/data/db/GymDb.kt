// Purpose: Room database singleton (v2: exercise descriptions + program tables)
// Inputs: application context
// Outputs: DAO access for the repository layer (never used directly from ViewModels)
package com.gymtracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutEntity::class,
        SetEntity::class,
        ProgramEntity::class,
        ProgramDayEntity::class,
        ProgramExerciseEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class GymDb : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun setDao(): SetDao
    abstract fun programDao(): ProgramDao

    companion object {
        // v1 → v2: description column + program tables (user data must survive)
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercises ADD COLUMN description TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `programs` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `program_days` (`id` TEXT NOT NULL, `programId` TEXT NOT NULL, " +
                        "`name` TEXT NOT NULL, `orderIdx` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_program_days_programId` ON `program_days` (`programId`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `program_exercises` (`id` TEXT NOT NULL, `dayId` TEXT NOT NULL, " +
                        "`exerciseId` TEXT NOT NULL, `orderIdx` INTEGER NOT NULL, `targetSets` INTEGER NOT NULL, " +
                        "`repMin` INTEGER NOT NULL, `repMax` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_program_exercises_dayId` ON `program_exercises` (`dayId`)")
            }
        }

        // v2 → v3: per-exercise sticky note (machine settings live at the exercise, not
        // the workout — they're the same every session)
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercises ADD COLUMN note TEXT NOT NULL DEFAULT ''")
            }
        }

        @Volatile private var instance: GymDb? = null

        fun get(context: Context): GymDb =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    GymDb::class.java,
                    "repforge.db",
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }
    }
}
