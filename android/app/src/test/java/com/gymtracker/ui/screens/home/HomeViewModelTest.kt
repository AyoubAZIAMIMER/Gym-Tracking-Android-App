// Purpose: Unit tests for HomeViewModel.reload()'s three-branch plan resolution and its
//          hasData=false guard (the exact bug the 2026-08-22 redesign session fixed live)
// Inputs: MockK-mocked WorkoutRepository via mockkObject(WorkoutRepository)
// Outputs: pass/fail signal for Home's plan-branch selection and readiness bucketing
package com.gymtracker.ui.screens.home

import android.app.Application
import com.gymtracker.MainDispatcherRule
import com.gymtracker.data.SampleData
import com.gymtracker.data.WorkoutRepository
import com.gymtracker.data.db.ExerciseEntity
import com.gymtracker.data.db.ProgramDayEntity
import com.gymtracker.data.db.ProgramExerciseEntity
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import java.time.DayOfWeek
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repo: WorkoutRepository

    @Before
    fun setUp() {
        repo = mockk()
        mockkObject(WorkoutRepository)
        every { WorkoutRepository.get(any()) } returns repo
        every { repo.workoutCount } returns flowOf(1) // one emission -> one reload(), then completes
        coEvery { repo.profile() } returns WorkoutRepository.Profile("Ayoub", null, null, 3)
        every { repo.isProfileSet() } returns true
    }

    @After
    fun tearDown() = unmockkObject(WorkoutRepository)

    private fun vm() = HomeViewModel(mockk<Application>(relaxed = true))

    private fun programDay(muscle: String) = WorkoutRepository.NextProgramDay(
        programName = "PPL",
        day = ProgramDayEntity("d1", "p1", "Push", 0),
    ) to WorkoutRepository.ProgramDayDetail(
        day = ProgramDayEntity("d1", "p1", "Push", 0),
        exercises = listOf(
            WorkoutRepository.ProgramExerciseDetail(
                row = ProgramExerciseEntity("pe1", "d1", "e1", 0, targetSets = 3, repMin = 8, repMax = 12),
                exercise = ExerciseEntity("e1", "Bench Press", muscles = muscle),
            ),
        ),
    )

    @Test
    fun `active program's next day becomes the plan card`() = runTest {
        val (next, detail) = programDay("Chest")
        coEvery { repo.homeStats() } returns WorkoutRepository.HomeStats(
            true, workoutsThisWeek = 2, doneWeekdays = setOf(DayOfWeek.MONDAY), lastWorkoutDaysAgo = 1, streakWeeks = 1,
        )
        coEvery { repo.muscleFreshness() } returns listOf(WorkoutRepository.MuscleFreshness("Chest", 3, 80))
        coEvery { repo.upcomingProgramDays(any()) } returns emptyList()
        coEvery { repo.recentWorkouts(any()) } returns emptyList()
        coEvery { repo.nextProgramDay() } returns next
        coEvery { repo.dayDetail("d1") } returns detail
        coEvery { repo.estimatedMinutesFor("Push") } returns 45

        val state = vm().ui.value

        assertEquals("PROGRAM · PPL", state.planLabel)
        assertEquals("Push", state.planTitle)
        assertEquals("d1", state.programDayId)
        assertEquals(45, state.estimatedMinutes)
        assertEquals(1, state.planRows.size)
    }

    @Test
    fun `no active program repeats the latest logged workout`() = runTest {
        coEvery { repo.homeStats() } returns WorkoutRepository.HomeStats(
            true, workoutsThisWeek = 2, doneWeekdays = setOf(DayOfWeek.MONDAY), lastWorkoutDaysAgo = 1, streakWeeks = 1,
        )
        coEvery { repo.muscleFreshness() } returns emptyList()
        coEvery { repo.upcomingProgramDays(any()) } returns emptyList()
        coEvery { repo.recentWorkouts(any()) } returns emptyList()
        coEvery { repo.nextProgramDay() } returns null
        val template = WorkoutRepository.SessionTemplate(
            name = "Push Day",
            exercises = listOf(
                WorkoutRepository.TemplateExercise(
                    exerciseId = "e1", name = "Bench Press", muscleGroup = "Chest",
                    sets = listOf(WorkoutRepository.TemplateSet(100.0, 5)),
                ),
            ),
        )
        coEvery { repo.latestWorkoutTemplate() } returns template
        coEvery { repo.estimatedMinutesFor("Push Day") } returns 30

        val state = vm().ui.value

        assertEquals("REPEAT LAST", state.planLabel)
        assertEquals("Push Day", state.planTitle)
        assertEquals(30, state.estimatedMinutes)
    }

    @Test
    fun `no program and no history falls back to the sample plan`() = runTest {
        coEvery { repo.homeStats() } returns WorkoutRepository.HomeStats(false, 0, emptySet(), null, 0)
        coEvery { repo.nextProgramDay() } returns null

        val state = vm().ui.value

        assertTrue(!state.hasData)
        assertEquals(SampleData.todaysPlanName, state.planTitle)
    }

    @Test
    fun `regression - a fresh account with an active program never leaks sample stats`() = runTest {
        // Exactly the bug fixed 2026-08-22: onboarding activates a program before the first
        // workout is ever logged, so homeStats().hasData is false while nextProgramDay() is
        // already non-null. statsPart()'s hasData=false branch must return real zeros, not
        // HomeUiState()'s SampleData defaults (workoutsThisWeek=1, doneWeekdays={WEDNESDAY}).
        val (next, detail) = programDay("Chest")
        coEvery { repo.homeStats() } returns WorkoutRepository.HomeStats(false, 0, emptySet(), null, 0)
        coEvery { repo.muscleFreshness() } returns emptyList()
        coEvery { repo.nextProgramDay() } returns next
        coEvery { repo.dayDetail("d1") } returns detail
        coEvery { repo.estimatedMinutesFor("Push") } returns null

        val state = vm().ui.value

        assertEquals(0, state.workoutsThisWeek)
        assertTrue(state.doneWeekdays.isEmpty())
    }

    private fun stubReadinessScenario(freshnessPercent: Int) {
        val (next, detail) = programDay("Chest")
        coEvery { repo.homeStats() } returns WorkoutRepository.HomeStats(
            true, workoutsThisWeek = 1, doneWeekdays = setOf(DayOfWeek.MONDAY), lastWorkoutDaysAgo = 1, streakWeeks = 1,
        )
        coEvery { repo.muscleFreshness() } returns listOf(WorkoutRepository.MuscleFreshness("Chest", 1, freshnessPercent))
        coEvery { repo.upcomingProgramDays(any()) } returns emptyList()
        coEvery { repo.recentWorkouts(any()) } returns emptyList()
        coEvery { repo.nextProgramDay() } returns next
        coEvery { repo.dayDetail("d1") } returns detail
        coEvery { repo.estimatedMinutesFor("Push") } returns null
    }

    @Test
    fun `readiness bucket - 95 percent reads READY`() = runTest {
        stubReadinessScenario(95)
        assertEquals("CHEST READY", vm().ui.value.readinessLabel)
    }

    @Test
    fun `readiness bucket - 75 percent reads WORN`() = runTest {
        stubReadinessScenario(75)
        assertEquals("CHEST WORN", vm().ui.value.readinessLabel)
    }

    @Test
    fun `readiness bucket - 50 percent reads HOT`() = runTest {
        stubReadinessScenario(50)
        assertEquals("CHEST HOT", vm().ui.value.readinessLabel)
    }

    @Test
    fun `readiness bucket - 20 percent reads SPENT`() = runTest {
        stubReadinessScenario(20)
        assertEquals("CHEST SPENT", vm().ui.value.readinessLabel)
    }

    @Test
    fun `needsProfile stays true until a profile is saved`() = runTest {
        every { repo.isProfileSet() } returns false
        coEvery { repo.homeStats() } returns WorkoutRepository.HomeStats(false, 0, emptySet(), null, 0)
        coEvery { repo.nextProgramDay() } returns null

        assertTrue(vm().ui.value.needsProfile)
    }
}
