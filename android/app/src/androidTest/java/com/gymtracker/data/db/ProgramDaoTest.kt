// Purpose: Instrumented tests for ProgramDao's hand-written @Query strings — the
//          catalog-merge rewrite and day/exercise ordering the editor depends on
// Inputs: Room.inMemoryDatabaseBuilder, real SQLite on the test device/emulator
// Outputs: pass/fail signal for ProgramDao's query correctness
package com.gymtracker.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProgramDaoTest {

    private lateinit var db: GymDb
    private lateinit var dao: ProgramDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), GymDb::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.programDao()
    }

    @After
    fun tearDown() = db.close()

    private fun row(id: String, dayId: String, exerciseId: String, orderIdx: Int) =
        ProgramExerciseEntity(id, dayId, exerciseId, orderIdx, targetSets = 3, repMin = 8, repMax = 12)

    @Test
    fun reassignExercise_rewritesEveryRowSharingTheOldExerciseId() = runTest {
        dao.upsertDays(listOf(ProgramDayEntity("d1", "p1", "Day 1", 0)))
        dao.upsertExercises(
            listOf(
                row("r1", "d1", "placeholder", 0),
                row("r2", "d1", "placeholder", 1),
                row("r3", "d1", "unrelated", 2),
            ),
        )

        dao.reassignExercise("placeholder", "named")

        val exercises = dao.dayExercises("d1").associateBy { it.id }
        assertEquals("named", exercises["r1"]!!.exerciseId)
        assertEquals("named", exercises["r2"]!!.exerciseId)
        assertEquals("unrelated", exercises["r3"]!!.exerciseId)
    }

    @Test
    fun dayExercises_isOrderedByOrderIdx() = runTest {
        dao.upsertDays(listOf(ProgramDayEntity("d1", "p1", "Day 1", 0)))
        dao.upsertExercises(
            listOf(row("last", "d1", "e3", 2), row("first", "d1", "e1", 0), row("middle", "d1", "e2", 1)),
        )

        val ids = dao.dayExercises("d1").map { it.id }

        assertEquals(listOf("first", "middle", "last"), ids)
    }

    @Test
    fun days_isOrderedByOrderIdxAndScopedToItsProgram() = runTest {
        dao.upsertDays(
            listOf(
                ProgramDayEntity("p1-b", "p1", "Day B", 1),
                ProgramDayEntity("p1-a", "p1", "Day A", 0),
                ProgramDayEntity("other", "p2", "Other Program's Day", 0),
            ),
        )

        val ids = dao.days("p1").map { it.id }

        assertEquals(listOf("p1-a", "p1-b"), ids)
    }

    @Test
    fun replaceProgramExercise_changesOnlyTheTargetedRow() = runTest {
        dao.upsertDays(listOf(ProgramDayEntity("d1", "p1", "Day 1", 0)))
        dao.upsertExercises(listOf(row("r1", "d1", "e1", 0), row("r2", "d1", "e1", 1)))

        dao.replaceProgramExercise("r1", "e2")

        val exercises = dao.dayExercises("d1").associateBy { it.id }
        assertEquals("e2", exercises["r1"]!!.exerciseId)
        assertEquals("e1", exercises["r2"]!!.exerciseId) // sharing the old id must be untouched
    }
}
