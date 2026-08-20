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
    version = 5,
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

        // v3 → v4: optional per-set RPE (effort rating) — genuinely optional, no default
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sets ADD COLUMN rpe REAL")
            }
        }

        // v4 → v5: persisted program-level superset pairing, so a program remembers
        // grouping instead of it being redone live every session
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE program_exercises ADD COLUMN supersetGroup INTEGER")
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                    .also { instance = it }
            }
    }
}
