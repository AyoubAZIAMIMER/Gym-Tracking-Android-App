// Purpose: Instrumented tests for SetDao's hand-written @Query strings — the PR-baseline
//          cutoff and the placeholder-merge rewrite that WorkoutRepository depends on
// Inputs: Room.inMemoryDatabaseBuilder, real SQLite on the test device/emulator
// Outputs: pass/fail signal for SetDao's query correctness
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
class SetDaoTest {

    private lateinit var db: GymDb
    private lateinit var dao: SetDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), GymDb::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.setDao()
    }

    @After
    fun tearDown() = db.close()

    private fun set(id: String, exerciseId: String, completedAt: Long, workoutId: String = "w1") =
        SetEntity(id, workoutId, exerciseId, completedAt, weightKg = 60.0, reps = 8)

    @Test
    fun forExercisesBefore_excludesSetsAtOrAfterTheCutoff() = runTest {
        dao.upsertAll(
            listOf(
                set("before", "e1", completedAt = 1_000L),
                set("atCutoff", "e1", completedAt = 2_000L),
                set("after", "e1", completedAt = 3_000L),
            ),
        )

        val result = dao.forExercisesBefore(listOf("e1"), before = 2_000L).map { it.id }

        assertEquals(listOf("before"), result)
    }

    @Test
    fun forExercisesBefore_onlyMatchesTheRequestedExerciseIds() = runTest {
        dao.upsertAll(
            listOf(
                set("e1-set", "e1", completedAt = 1_000L),
                set("e2-set", "e2", completedAt = 1_000L),
            ),
        )

        val result = dao.forExercisesBefore(listOf("e1"), before = 5_000L).map { it.id }

        assertEquals(listOf("e1-set"), result)
    }

    @Test
    fun since_isInclusiveOfTheCutoffTimestamp() = runTest {
        dao.upsertAll(
            listOf(
                set("old", "e1", completedAt = 999L),
                set("atCutoff", "e1", completedAt = 1_000L),
                set("new", "e1", completedAt = 1_001L),
            ),
        )

        val result = dao.since(1_000L).map { it.id }.toSet()

        assertEquals(setOf("atCutoff", "new"), result)
    }

    @Test
    fun reassignExercise_movesEverySetFromThePlaceholderOntoTheTarget() = runTest {
        dao.upsertAll(
            listOf(
                set("s1", "placeholder", completedAt = 1_000L),
                set("s2", "placeholder", completedAt = 2_000L),
                set("s3", "unrelated", completedAt = 3_000L),
            ),
        )

        val moved = dao.reassignExercise("placeholder", "named")

        assertEquals(2, moved)
        assertEquals(0, dao.countForExercise("placeholder"))
        assertEquals(2, dao.countForExercise("named"))
        // the untouched set must not have been swept up by the rewrite
        assertEquals("unrelated", dao.getAll().single { it.id == "s3" }.exerciseId)
    }
}
