// Purpose: Instrumented tests for ExerciseDao's hand-written @Query strings — the
//          archived filter and the IGNORE-on-conflict import guarantee
// Inputs: Room.inMemoryDatabaseBuilder, real SQLite on the test device/emulator
// Outputs: pass/fail signal for ExerciseDao's query correctness
package com.gymtracker.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExerciseDaoTest {

    private lateinit var db: GymDb
    private lateinit var dao: ExerciseDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), GymDb::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.exerciseDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun observeAll_excludesArchivedExercises() = runTest {
        dao.upsertAll(
            listOf(
                ExerciseEntity("e1", "Bench Press", archived = false),
                ExerciseEntity("e2", "Retired Move", archived = true),
            ),
        )

        val active = dao.observeAll().first().map { it.id }

        assertEquals(listOf("e1"), active)
    }

    @Test
    fun insertAllIgnore_neverOverwritesAnExistingRow() = runTest {
        dao.upsertAll(listOf(ExerciseEntity("e1", "Bench Press", note = "seat height 4")))

        // the import path must not clobber a note the user already set on re-import
        dao.insertAllIgnore(listOf(ExerciseEntity("e1", "Bench Press (renamed by import)", note = "")))

        val stored = dao.getAll().single { it.id == "e1" }
        assertEquals("Bench Press", stored.name)
        assertEquals("seat height 4", stored.note)
    }

    @Test
    fun updateNote_changesOnlyTheTargetedExercise() = runTest {
        dao.upsertAll(listOf(ExerciseEntity("e1", "Bench Press"), ExerciseEntity("e2", "Squat")))

        dao.updateNote("e1", "pin 4, seat 2")

        val all = dao.getAll().associateBy { it.id }
        assertEquals("pin 4, seat 2", all["e1"]!!.note)
        assertEquals("", all["e2"]!!.note)
    }

    @Test
    fun delete_removesOnlyThatExercise() = runTest {
        dao.upsertAll(listOf(ExerciseEntity("e1", "Bench Press"), ExerciseEntity("e2", "Squat")))

        dao.delete("e1")

        assertEquals(listOf("e2"), dao.getAll().map { it.id })
    }
}
