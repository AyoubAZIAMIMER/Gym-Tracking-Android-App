// Purpose: Unit tests for WorkoutSessionViewModel — set completion/auto-advance, live-PR
//          flagging, superset grouping, warm-up ramp math, session-JSON restore, finish
// Inputs: MockK-mocked WorkoutRepository + RestTimerService (both via mockkObject seams)
// Outputs: pass/fail signal for the live workout session's core interaction logic
package com.gymtracker.ui.screens.session

import android.app.Application
import com.google.gson.Gson
import com.gymtracker.MainDispatcherRule
import com.gymtracker.data.WorkoutRepository
import com.gymtracker.service.RestTimerService
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class WorkoutSessionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repo: WorkoutRepository

    @Before
    fun setUp() {
        repo = mockk()
        mockkObject(WorkoutRepository)
        every { WorkoutRepository.get(any()) } returns repo
        mockkObject(RestTimerService.Companion)
        every { RestTimerService.start(any(), any(), any(), any()) } just Runs
        every { RestTimerService.stop(any()) } just Runs

        every { repo.loadActiveSessionJson() } returns null
        every { repo.saveActiveSessionJson(any()) } just Runs
        every { repo.clearActiveSession() } just Runs
        every { repo.barWeightKg() } returns 20.0
        every { repo.restSeconds() } returns 120
        coEvery { repo.bestE1rmByExercise() } returns emptyMap()
        coEvery { repo.latestWorkoutTemplate() } returns null
        coEvery { repo.exercisesSnapshot() } returns emptyList()
    }

    @After
    fun tearDown() {
        unmockkObject(WorkoutRepository)
        unmockkObject(RestTimerService.Companion)
    }

    private fun vm() = WorkoutSessionViewModel(mockk<Application>(relaxed = true))

    /** A two-exercise template with real dbExerciseIds, so live-PR baselines can apply
     *  ([bestE1rm] is keyed by exercise id — sampleSession()'s exercises carry none). */
    private fun vmWithPrTemplate(): WorkoutSessionViewModel {
        val template = WorkoutRepository.SessionTemplate(
            name = "Push Day",
            exercises = listOf(
                WorkoutRepository.TemplateExercise(
                    exerciseId = "e1", name = "Bench Press", muscleGroup = "Chest",
                    sets = listOf(WorkoutRepository.TemplateSet(100.0, 5)),
                ),
                WorkoutRepository.TemplateExercise(
                    exerciseId = "e2", name = "Squat", muscleGroup = "Quads",
                    sets = listOf(WorkoutRepository.TemplateSet(120.0, 5)),
                ),
            ),
        )
        coEvery { repo.latestWorkoutTemplate() } returns template
        coEvery { repo.bestE1rmByExercise() } returns mapOf("e1" to 90.0) // e2 has no baseline
        return vm()
    }

    @Test
    fun `toggleCompleted advances the active set to the next incomplete one`() = runTest {
        val template = WorkoutRepository.SessionTemplate(
            name = "Push Day",
            exercises = listOf(
                WorkoutRepository.TemplateExercise(
                    exerciseId = "e1", name = "Bench Press", muscleGroup = "Chest",
                    sets = listOf(WorkoutRepository.TemplateSet(100.0, 5), WorkoutRepository.TemplateSet(100.0, 5)),
                ),
            ),
        )
        coEvery { repo.latestWorkoutTemplate() } returns template
        val model = vm()
        val ex = model.ui.value.exercises.single()
        val (first, second) = ex.sets

        model.toggleCompleted(ex.id, first.id)

        assertEquals(second.id, model.ui.value.activeSetId)
        assertTrue(model.ui.value.exercises.single().sets[0].completed)
    }

    @Test
    fun `toggleCompleted carries the just-logged weight into the next blank set`() = runTest {
        // Neither set has any history/plan hint — the exact first-ever-session case where the
        // stepper used to start at 0 for every set instead of picking up what was just lifted.
        val template = WorkoutRepository.SessionTemplate(
            name = "Push Day",
            exercises = listOf(
                WorkoutRepository.TemplateExercise(
                    exerciseId = "e1", name = "Bench Press", muscleGroup = "Chest",
                    sets = listOf(WorkoutRepository.TemplateSet(null, null), WorkoutRepository.TemplateSet(null, null)),
                ),
            ),
        )
        coEvery { repo.latestWorkoutTemplate() } returns template
        val model = vm()
        val (first, second) = model.ui.value.exercises.single().sets
        assertNull(second.effectiveWeightKg)

        model.setWeightText(model.ui.value.exercises.single().id, first.id, "60")
        model.toggleCompleted(model.ui.value.exercises.single().id, first.id)

        val updatedSecond = model.ui.value.exercises.single().sets[1]
        assertEquals(60.0, updatedSecond.effectiveWeightKg)
    }

    @Test
    fun `toggleCompleted does not override a set's own history hint with the carried weight`() = runTest {
        // set 2 already has a real prior-session weight (80) — today's set 1 (60) must not
        // clobber it, only a genuinely blank next set should pick up the carried value.
        val template = WorkoutRepository.SessionTemplate(
            name = "Push Day",
            exercises = listOf(
                WorkoutRepository.TemplateExercise(
                    exerciseId = "e1", name = "Bench Press", muscleGroup = "Chest",
                    sets = listOf(WorkoutRepository.TemplateSet(null, null), WorkoutRepository.TemplateSet(80.0, 8)),
                ),
            ),
        )
        coEvery { repo.latestWorkoutTemplate() } returns template
        val model = vm()
        val (first, _) = model.ui.value.exercises.single().sets

        model.setWeightText(model.ui.value.exercises.single().id, first.id, "60")
        model.toggleCompleted(model.ui.value.exercises.single().id, first.id)

        assertEquals(80.0, model.ui.value.exercises.single().sets[1].effectiveWeightKg)
    }

    @Test
    fun `toggleCompleted flags a set beating the all-time e1RM as a PR`() = runTest {
        val model = vmWithPrTemplate()
        val bench = model.ui.value.exercises.single { it.name == "Bench Press" }

        model.toggleCompleted(bench.id, bench.sets.single().id)

        val logged = model.ui.value.exercises.single { it.name == "Bench Press" }.sets.single()
        assertTrue(logged.isPr)
        assertTrue(logged.intensity!! >= 1f)
    }

    @Test
    fun `toggleCompleted does not flag a PR when there is no baseline for that exercise`() = runTest {
        val model = vmWithPrTemplate()
        val squat = model.ui.value.exercises.single { it.name == "Squat" }

        model.toggleCompleted(squat.id, squat.sets.single().id)

        val logged = model.ui.value.exercises.single { it.name == "Squat" }.sets.single()
        assertTrue(!logged.isPr)
    }

    @Test
    fun `toggleSupersetWithNext joins then dissolves an adjacent pair`() = runTest {
        val model = vm()
        val (ex0, ex1, ex2) = model.ui.value.exercises

        model.toggleSupersetWithNext(ex0.id)
        val afterJoin = model.ui.value.exercises.associateBy { it.id }
        assertEquals(afterJoin[ex0.id]!!.supersetGroup, afterJoin[ex1.id]!!.supersetGroup)
        assertTrue(afterJoin[ex0.id]!!.supersetGroup != null)
        assertNull(afterJoin[ex2.id]!!.supersetGroup)

        model.toggleSupersetWithNext(ex0.id)
        val afterLeave = model.ui.value.exercises.associateBy { it.id }
        assertNull(afterLeave[ex0.id]!!.supersetGroup)
        assertNull(afterLeave[ex1.id]!!.supersetGroup)
    }

    @Test
    fun `generateWarmupSets ramps bar to 40-60-80 percent of the target, rounded to 2point5kg`() = runTest {
        val model = vm()
        // sampleSession()'s first exercise (Bench Press) has an effective top weight of 60kg
        val bench = model.ui.value.exercises.first()

        model.generateWarmupSets(bench.id)

        val warmups = model.ui.value.exercises.first().sets.filter { it.tag == SetTag.WARMUP }
        assertEquals(
            listOf("20" to "10", "25" to "8", "35" to "5", "47.5" to "3"),
            warmups.map { it.weightText to it.repsText },
        )
    }

    @Test
    fun `restoreActiveSession round-trips a persisted session on construction`() = runTest {
        val persisted = WorkoutSessionUiState(
            workoutName = "Leg Day",
            exercises = listOf(
                SessionExercise(id = 1L, name = "Squat", muscleGroup = "Quads", sets = listOf(SessionSet(id = 2L, completed = true))),
            ),
            activeSetId = 2L,
            sessionActive = true,
            showExercisePicker = true,
            replacingExerciseId = 5L,
            finished = true,
            savedWorkoutId = "stale-id",
        )
        every { repo.loadActiveSessionJson() } returns Gson().toJson(persisted)

        val state = vm().ui.value

        assertEquals("Leg Day", state.workoutName)
        assertTrue(state.sessionActive)
        assertEquals(1, state.exercises.size)
        // restoreActiveSession() explicitly clears transient/one-shot UI flags on restore
        assertTrue(!state.showExercisePicker)
        assertNull(state.replacingExerciseId)
        assertTrue(!state.finished)
        assertNull(state.savedWorkoutId)
    }

    @Test
    fun `finishWorkout with zero completed sets saves nothing`() = runTest {
        val model = vm()

        model.finishWorkout()

        assertTrue(model.ui.value.finished)
        assertNull(model.ui.value.savedWorkoutId)
        coVerify(exactly = 0) { repo.saveSession(any(), any(), any(), any()) }
    }

    @Test
    fun `finishWorkout persists only the completed sets`() = runTest {
        val model = vmWithPrTemplate()
        val bench = model.ui.value.exercises.single { it.name == "Bench Press" }
        model.toggleCompleted(bench.id, bench.sets.single().id)
        val savedSetsSlot = slot<List<WorkoutRepository.SaveExercise>>()
        coEvery { repo.saveSession(any(), any(), any(), capture(savedSetsSlot)) } returns "new-workout-id"

        model.finishWorkout()

        assertEquals("new-workout-id", model.ui.value.savedWorkoutId)
        assertTrue(!model.ui.value.sessionActive)
        // only Bench Press had a completed set; Squat's uncompleted set is dropped entirely
        assertEquals(listOf("Bench Press"), savedSetsSlot.captured.map { it.name })
        assertEquals(1, savedSetsSlot.captured.single().sets.size)
    }
}
