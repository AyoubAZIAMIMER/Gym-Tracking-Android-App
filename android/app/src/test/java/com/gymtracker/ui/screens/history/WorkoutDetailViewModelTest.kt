// Purpose: Unit tests for WorkoutDetailViewModel's load() — PR row / volume-delta wiring,
//          the muscle-split zero-division guard, and the celebrate-flag passthrough
// Inputs: MockK-mocked WorkoutRepository via mockkObject(WorkoutRepository)
// Outputs: pass/fail signal for the workout-detail screen's presentation logic
package com.gymtracker.ui.screens.history

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.gymtracker.MainDispatcherRule
import com.gymtracker.data.WorkoutRepository
import com.gymtracker.data.db.WorkoutEntity
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class WorkoutDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repo: WorkoutRepository

    @Before
    fun setUp() {
        repo = mockk()
        mockkObject(WorkoutRepository)
        every { WorkoutRepository.get(any()) } returns repo
        coEvery { repo.profile() } returns WorkoutRepository.Profile("Ayoub", 80.0, 180, 3)
    }

    @After
    fun tearDown() = unmockkObject(WorkoutRepository)

    private fun vm(workoutId: String = "w1", celebrate: Boolean = false) = WorkoutDetailViewModel(
        mockk<Application>(relaxed = true),
        SavedStateHandle(mapOf("workoutId" to workoutId, "celebrate" to celebrate)),
    )

    private fun detailWith(
        totalSets: Int,
        exercises: List<WorkoutRepository.DetailExercise>,
        prRows: List<WorkoutRepository.PrRow> = emptyList(),
    ) = WorkoutRepository.WorkoutDetail(
        workout = WorkoutEntity("w1", "Push Day", startedAt = 1_000L, endedAt = 2_000L),
        exercises = exercises,
        totalSets = totalSets,
        totalVolumeKg = 1_000.0,
        prRows = prRows,
    )

    @Test
    fun `load populates PR rows and the volume delta from the repository`() = runTest {
        val exercises = listOf(
            WorkoutRepository.DetailExercise(
                exerciseId = "e1", name = "Bench Press", muscles = "Chest", equipment = "Barbell",
                sets = listOf(WorkoutRepository.DetailSet("s1", 100.0, 5, null, isPr = true, e1rm = 112.5)),
            ),
        )
        coEvery { repo.workoutDetail("w1") } returns detailWith(
            totalSets = 1, exercises = exercises,
            prRows = listOf(WorkoutRepository.PrRow("Bench Press", oldE1rm = 90.0, newE1rm = 112.5)),
        )
        coEvery { repo.volumeDeltaVsLastSameNamed("w1", "Push Day", 1_000.0) } returns 15

        val state = vm().ui.value

        assertEquals(1, state.prRows.size)
        assertEquals("Bench Press", state.prRows[0].exerciseName)
        assertEquals(15, state.volumeDeltaPercent)
        assertEquals(1, state.prCount)
    }

    @Test
    fun `musclesSplit guards against a zero totalSets denominator`() = runTest {
        // an all-warmup workout: totalSets (working sets) is 0, but the exercise still has
        // sets whose muscle share must be computed without dividing by zero
        val exercises = listOf(
            WorkoutRepository.DetailExercise(
                exerciseId = "e1", name = "Bench Press", muscles = "Chest", equipment = "Barbell",
                sets = listOf(
                    WorkoutRepository.DetailSet("s1", 40.0, 10, "W", isPr = false),
                    WorkoutRepository.DetailSet("s2", 40.0, 10, "W", isPr = false),
                ),
            ),
        )
        coEvery { repo.workoutDetail("w1") } returns detailWith(totalSets = 0, exercises = exercises)
        coEvery { repo.volumeDeltaVsLastSameNamed(any(), any(), any()) } returns null

        val state = vm().ui.value

        val chest = state.musclesSplit.single { it.muscle == "Chest" }
        assertEquals(2, chest.sets)
        assertTrue(chest.fraction.isFinite())
        assertEquals(2.0f, chest.fraction, 0.001f)
    }

    @Test
    fun `load flags deleted when the workout no longer exists`() = runTest {
        coEvery { repo.workoutDetail("gone") } returns null

        val state = vm(workoutId = "gone").ui.value

        assertTrue(state.deleted)
    }

    @Test
    fun `justFinished mirrors the celebrate saved-state flag`() = runTest {
        coEvery { repo.workoutDetail("w1") } returns detailWith(totalSets = 0, exercises = emptyList())
        coEvery { repo.volumeDeltaVsLastSameNamed(any(), any(), any()) } returns null

        assertTrue(vm(celebrate = true).ui.value.justFinished)
        assertTrue(!vm(celebrate = false).ui.value.justFinished)
    }

    @Test
    fun `prCount sums PR sets across every exercise`() = runTest {
        val exercises = listOf(
            WorkoutRepository.DetailExercise(
                exerciseId = "e1", name = "Bench Press", muscles = "Chest", equipment = "Barbell",
                sets = listOf(
                    WorkoutRepository.DetailSet("s1", 100.0, 5, null, isPr = true, e1rm = 112.5),
                    WorkoutRepository.DetailSet("s2", 100.0, 5, null, isPr = false, e1rm = 112.5),
                ),
            ),
            WorkoutRepository.DetailExercise(
                exerciseId = "e2", name = "Squat", muscles = "Quads", equipment = "Barbell",
                sets = listOf(WorkoutRepository.DetailSet("s3", 150.0, 5, null, isPr = true, e1rm = 168.75)),
            ),
        )
        coEvery { repo.workoutDetail("w1") } returns detailWith(totalSets = 3, exercises = exercises)
        coEvery { repo.volumeDeltaVsLastSameNamed(any(), any(), any()) } returns null

        assertEquals(2, vm().ui.value.prCount)
    }
}
