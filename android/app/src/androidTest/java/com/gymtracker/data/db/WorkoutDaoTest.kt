// Purpose: Instrumented tests for WorkoutDao's hand-written @Query strings against real
//          in-memory Room/SQLite — ordering, filters, and date-range cutoffs Room itself
//          can't be trusted to get right just because "insert then select by id" works
// Inputs: Room.inMemoryDatabaseBuilder, real SQLite on the test device/emulator
// Outputs: pass/fail signal for WorkoutDao's query correctness
package com.gymtracker.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutDaoTest {

    private lateinit var db: GymDb
    private lateinit var dao: WorkoutDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), GymDb::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.workoutDao()
    }

    @After
    fun tearDown() = db.close()

    private fun workout(id: String, name: String, startedAt: Long, endedAt: Long? = startedAt + 1_000L) =
        WorkoutEntity(id, name, startedAt, endedAt)

    @Test
    fun latestNamed_ordersNewestFirstAndRespectsTheLimit() = runTest {
        dao.upsertAll(
            listOf(
                workout("a", "Push", startedAt = 1_000L),
                workout("b", "Push", startedAt = 3_000L),
                workout("c", "Push", startedAt = 2_000L),
                workout("d", "Pull", startedAt = 5_000L), // different name, must not appear
            ),
        )

        val result = dao.latestNamed("Push", limit = 2)

        assertEquals(listOf("b", "c"), result.map { it.id })
    }

    @Test
    fun latestNamed_excludesWorkoutsThatWereNeverFinished() = runTest {
        dao.upsertAll(
            listOf(
                workout("finished", "Push", startedAt = 1_000L, endedAt = 2_000L),
                workout("abandoned", "Push", startedAt = 3_000L, endedAt = null),
            ),
        )

        val result = dao.latestNamed("Push", limit = 5)

        assertEquals(listOf("finished"), result.map { it.id })
    }

    @Test
    fun between_isStartInclusiveAndEndExclusive() = runTest {
        dao.upsertAll(
            listOf(
                workout("before", "W", startedAt = 999L),
                workout("atStart", "W", startedAt = 1_000L),
                workout("inside", "W", startedAt = 1_500L),
                workout("atEnd", "W", startedAt = 2_000L),
                workout("after", "W", startedAt = 2_001L),
            ),
        )

        val result = dao.between(start = 1_000L, end = 2_000L).map { it.id }.toSet()

        assertEquals(setOf("atStart", "inside"), result)
    }

    @Test
    fun since_returnsOnlyWorkoutsAtOrAfterTheCutoff() = runTest {
        dao.upsertAll(
            listOf(
                workout("old", "W", startedAt = 1_000L),
                workout("cutoff", "W", startedAt = 2_000L),
                workout("new", "W", startedAt = 3_000L),
            ),
        )

        val result = dao.since(2_000L).map { it.id }.toSet()

        assertEquals(setOf("cutoff", "new"), result)
    }

    @Test
    fun earliestStart_isNullOnAnEmptyTable() = runTest {
        assertNull(dao.earliestStart())
    }

    @Test
    fun earliestStart_findsTheMinimumAcrossAllWorkouts() = runTest {
        dao.upsertAll(
            listOf(
                workout("mid", "W", startedAt = 2_000L),
                workout("earliest", "W", startedAt = 500L),
                workout("late", "W", startedAt = 3_000L),
            ),
        )

        assertEquals(500L, dao.earliestStart())
    }
}
