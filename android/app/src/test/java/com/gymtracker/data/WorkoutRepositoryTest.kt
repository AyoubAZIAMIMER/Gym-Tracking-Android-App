// Purpose: Host-JVM unit tests for WorkoutRepository's business logic (PR/e1RM math,
//          volume deltas, template previews, home stats, program creation, session save)
// Inputs: MockK-mocked GymDb/DAOs, FakeSharedPreferences, mocked Context
// Outputs: pass/fail signal for the repository's own logic, independent of Room/SQLite
package com.gymtracker.data

import android.content.Context
import com.gymtracker.MainDispatcherRule
import com.gymtracker.data.db.ExerciseDao
import com.gymtracker.data.db.ExerciseEntity
import com.gymtracker.data.db.GymDb
import com.gymtracker.data.db.ProgramDao
import com.gymtracker.data.db.ProgramDayEntity
import com.gymtracker.data.db.ProgramEntity
import com.gymtracker.data.db.ProgramExerciseEntity
import com.gymtracker.data.db.SetDao
import com.gymtracker.data.db.SetEntity
import com.gymtracker.data.db.WorkoutDao
import com.gymtracker.data.db.WorkoutEntity
import com.gymtracker.widget.ForgeWidgetProvider
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class WorkoutRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var exerciseDao: ExerciseDao
    private lateinit var workoutDao: WorkoutDao
    private lateinit var setDao: SetDao
    private lateinit var programDao: ProgramDao
    private lateinit var prefs: FakeSharedPreferences
    private lateinit var repo: WorkoutRepository

    @Before
    fun setUp() {
        exerciseDao = mockk()
        workoutDao = mockk()
        setDao = mockk()
        programDao = mockk()
        val db = mockk<GymDb> {
            every { exerciseDao() } returns exerciseDao
            every { workoutDao() } returns workoutDao
            every { setDao() } returns setDao
            every { programDao() } returns programDao
        }
        prefs = FakeSharedPreferences()
        val appContext = mockk<Context>(relaxed = true)
        repo = WorkoutRepository(db, prefs, appContext)
    }

    @After
    fun tearDown() {
        if (isForgeWidgetMocked) unmockkObject(ForgeWidgetProvider.Companion)
    }

    private var isForgeWidgetMocked = false
    private fun stubWidgetUpdate() {
        mockkObject(ForgeWidgetProvider.Companion)
        every { ForgeWidgetProvider.requestUpdate(any()) } just Runs
        isForgeWidgetMocked = true
    }

    // --- workoutDetail(): PR / e1RM detection ------------------------------------

    @Test
    fun `workoutDetail - a first-ever lift is never a PR`() = runTest {
        val workout = WorkoutEntity("w1", "Push", startedAt = 1_000L, endedAt = 2_000L)
        coEvery { workoutDao.byId("w1") } returns workout
        coEvery { setDao.forWorkout("w1") } returns listOf(
            SetEntity("s1", "w1", "e1", completedAt = 1_500L, weightKg = 100.0, reps = 5, orderInWorkout = 0),
        )
        coEvery { exerciseDao.getAll() } returns listOf(ExerciseEntity("e1", "Bench Press"))
        coEvery { setDao.forExercisesBefore(listOf("e1"), 1_000L) } returns emptyList()

        val detail = repo.workoutDetail("w1")!!

        val set = detail.exercises.single().sets.single()
        assertEquals(112.5, set.e1rm!!, 0.001)
        assertTrue(!set.isPr)
        assertTrue(detail.prRows.isEmpty())
    }

    @Test
    fun `workoutDetail - beating the prior best is a PR`() = runTest {
        val workout = WorkoutEntity("w1", "Push", startedAt = 2_000L, endedAt = 3_000L)
        coEvery { workoutDao.byId("w1") } returns workout
        coEvery { setDao.forWorkout("w1") } returns listOf(
            SetEntity("s1", "w1", "e1", completedAt = 2_500L, weightKg = 100.0, reps = 5, orderInWorkout = 0),
        )
        coEvery { exerciseDao.getAll() } returns listOf(ExerciseEntity("e1", "Bench Press"))
        // baseline: 80kg x 5 -> e1rm 90.0
        coEvery { setDao.forExercisesBefore(listOf("e1"), 2_000L) } returns listOf(
            SetEntity("s0", "w0", "e1", completedAt = 1_000L, weightKg = 80.0, reps = 5, orderInWorkout = 0),
        )

        val detail = repo.workoutDetail("w1")!!

        val set = detail.exercises.single().sets.single()
        assertTrue(set.isPr)
        assertEquals(1, detail.prRows.size)
        assertEquals("Bench Press", detail.prRows[0].exerciseName)
        assertEquals(90.0, detail.prRows[0].oldE1rm, 0.001)
        assertEquals(112.5, detail.prRows[0].newE1rm, 0.001)
    }

    @Test
    fun `workoutDetail - matching the prior best exactly is not a PR`() = runTest {
        val workout = WorkoutEntity("w1", "Push", startedAt = 2_000L, endedAt = 3_000L)
        coEvery { workoutDao.byId("w1") } returns workout
        coEvery { setDao.forWorkout("w1") } returns listOf(
            SetEntity("s1", "w1", "e1", completedAt = 2_500L, weightKg = 100.0, reps = 5, orderInWorkout = 0),
        )
        coEvery { exerciseDao.getAll() } returns listOf(ExerciseEntity("e1", "Bench Press"))
        // baseline: same weight/reps -> same e1rm (112.5) as today's set
        coEvery { setDao.forExercisesBefore(listOf("e1"), 2_000L) } returns listOf(
            SetEntity("s0", "w0", "e1", completedAt = 1_000L, weightKg = 100.0, reps = 5, orderInWorkout = 0),
        )

        val detail = repo.workoutDetail("w1")!!

        assertTrue(!detail.exercises.single().sets.single().isPr)
        assertTrue(detail.prRows.isEmpty())
    }

    @Test
    fun `workoutDetail - an all-warmup session has zero working sets and volume`() = runTest {
        val workout = WorkoutEntity("w1", "Push", startedAt = 1_000L, endedAt = 2_000L)
        coEvery { workoutDao.byId("w1") } returns workout
        coEvery { setDao.forWorkout("w1") } returns listOf(
            SetEntity("s1", "w1", "e1", completedAt = 1_500L, weightKg = 60.0, reps = 10, tag = "W", orderInWorkout = 0),
        )
        coEvery { exerciseDao.getAll() } returns listOf(ExerciseEntity("e1", "Bench Press"))
        coEvery { setDao.forExercisesBefore(any(), any()) } returns emptyList()

        val detail = repo.workoutDetail("w1")!!

        assertEquals(0, detail.totalSets)
        assertEquals(0.0, detail.totalVolumeKg, 0.001)
        assertNull(detail.exercises.single().sets.single().e1rm)
        assertTrue(detail.prRows.isEmpty())
    }

    // --- volumeDeltaVsLastSameNamed() ---------------------------------------------

    @Test
    fun `volumeDelta - null when no earlier same-named workout exists`() = runTest {
        coEvery { workoutDao.latestNamed("Push", 2) } returns emptyList()

        assertNull(repo.volumeDeltaVsLastSameNamed("w1", "Push", 1_000.0))
    }

    @Test
    fun `volumeDelta - ignores the current workout when it is its own latest match`() = runTest {
        coEvery { workoutDao.latestNamed("Push", 2) } returns listOf(WorkoutEntity("w1", "Push", 1_000L))

        assertNull(repo.volumeDeltaVsLastSameNamed("w1", "Push", 1_000.0))
    }

    @Test
    fun `volumeDelta - null when the prior workout had zero working volume`() = runTest {
        coEvery { workoutDao.latestNamed("Push", 2) } returns listOf(WorkoutEntity("prev", "Push", 500L))
        coEvery { setDao.forWorkout("prev") } returns listOf(
            SetEntity("s0", "prev", "e1", completedAt = 500L, weightKg = 40.0, reps = 10, tag = "W", orderInWorkout = 0),
        )

        assertNull(repo.volumeDeltaVsLastSameNamed("w1", "Push", 1_000.0))
    }

    @Test
    fun `volumeDelta - computes a signed percentage against working volume only`() = runTest {
        coEvery { workoutDao.latestNamed("Push", 2) } returns listOf(WorkoutEntity("prev", "Push", 500L))
        coEvery { setDao.forWorkout("prev") } returns listOf(
            SetEntity("s0", "prev", "e1", completedAt = 500L, weightKg = 100.0, reps = 10, orderInWorkout = 0), // 1000
            SetEntity("s1", "prev", "e1", completedAt = 500L, weightKg = 50.0, reps = 10, tag = "W", orderInWorkout = 1), // excluded
        )

        assertEquals(10, repo.volumeDeltaVsLastSameNamed("w1", "Push", 1_100.0))
    }

    // --- previewTemplate() ---------------------------------------------------------

    @Test
    fun `previewTemplate - splits muscle load and estimates time from rest plus work seconds`() = runTest {
        coEvery { exerciseDao.getAll() } returns listOf(
            ExerciseEntity("e1", "Bench Press", muscles = "Chest"),
            ExerciseEntity("e2", "Squat", muscles = "Quads"),
        )
        val template = ProgramTemplates.Template(
            name = "T", description = "",
            days = listOf(
                ProgramTemplates.TemplateDay(
                    "Day 1",
                    listOf(
                        ProgramTemplates.TemplateExercise("Bench Press", sets = 4, repMin = 8, repMax = 12),
                        ProgramTemplates.TemplateExercise("Squat", sets = 4, repMin = 8, repMax = 12),
                    ),
                ),
            ),
        )

        val preview = repo.previewTemplate(template)

        // restSeconds defaults to 120s; 4 sets * (120 + 40) * 2 exercises / 1 day / 60
        assertEquals(21, preview.estimatedMinutes)
        assertEquals(1, preview.dayCount)
        assertEquals(
            listOf("Chest" to 50, "Quads" to 50),
            preview.muscleLoad.map { it.muscle to it.percent },
        )
    }

    @Test
    fun `previewTemplate - an exercise with no catalog match still costs time but no muscle share`() = runTest {
        coEvery { exerciseDao.getAll() } returns listOf(ExerciseEntity("e1", "Bench Press", muscles = "Chest"))
        val template = ProgramTemplates.Template(
            name = "T", description = "",
            days = listOf(
                ProgramTemplates.TemplateDay(
                    "Day 1",
                    listOf(
                        ProgramTemplates.TemplateExercise("Bench Press", sets = 4, repMin = 8, repMax = 12),
                        ProgramTemplates.TemplateExercise("Made Up Move", sets = 4, repMin = 8, repMax = 12),
                    ),
                ),
            ),
        )

        val preview = repo.previewTemplate(template)

        assertEquals(listOf("Chest"), preview.muscleLoad.map { it.muscle })
        assertEquals(100, preview.muscleLoad.single().percent)
        // both exercises still contribute to the time estimate: 2 * 4 * 160 / 60 = 21
        assertEquals(21, preview.estimatedMinutes)
    }

    // --- homeStats() -----------------------------------------------------------------
    // Dates are anchored off LocalDate.now() rather than hardcoded, so the test is not
    // flaky depending on which weekday it happens to run on.

    private val zone = ZoneId.systemDefault()
    private fun millisAt(date: java.time.LocalDate) = date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `homeStats - no workouts means no data, not the sample week`() = runTest {
        coEvery { workoutDao.allStartTimes() } returns emptyList()

        val stats = repo.homeStats()

        assertTrue(!stats.hasData)
        assertEquals(0, stats.workoutsThisWeek)
        assertTrue(stats.doneWeekdays.isEmpty())
        assertNull(stats.lastWorkoutDaysAgo)
        assertEquals(0, stats.streakWeeks)
    }

    @Test
    fun `homeStats - a single workout today counts this week with a one-week streak`() = runTest {
        val today = java.time.LocalDate.now(zone)
        coEvery { workoutDao.allStartTimes() } returns listOf(millisAt(today))

        val stats = repo.homeStats()

        assertTrue(stats.hasData)
        assertEquals(1, stats.workoutsThisWeek)
        assertEquals(setOf(today.dayOfWeek), stats.doneWeekdays)
        assertEquals(0, stats.lastWorkoutDaysAgo)
        assertEquals(1, stats.streakWeeks)
    }

    @Test
    fun `homeStats - two consecutive weeks logged extends the streak`() = runTest {
        val weekStart = java.time.LocalDate.now(zone).with(DayOfWeek.MONDAY)
        coEvery { workoutDao.allStartTimes() } returns listOf(
            millisAt(weekStart),
            millisAt(weekStart.minusWeeks(1)),
        )

        val stats = repo.homeStats()

        assertEquals(2, stats.streakWeeks)
        assertEquals(1, stats.workoutsThisWeek)
    }

    @Test
    fun `homeStats - a gap before the current week breaks the streak`() = runTest {
        val weekStart = java.time.LocalDate.now(zone).with(DayOfWeek.MONDAY)
        // nothing this week or last week; only two weeks further back
        coEvery { workoutDao.allStartTimes() } returns listOf(
            millisAt(weekStart.minusWeeks(2)),
            millisAt(weekStart.minusWeeks(3)),
        )

        val stats = repo.homeStats()

        assertEquals(0, stats.streakWeeks)
        assertEquals(0, stats.workoutsThisWeek)
    }

    // --- createFromTemplate() / setActiveProgram() ------------------------------------

    @Test
    fun `createFromTemplate - unmatched exercise names are silently skipped`() = runTest {
        coEvery { exerciseDao.getAll() } returns listOf(ExerciseEntity("e-bench", "Bench Press"))
        coEvery { programDao.upsertPrograms(any()) } just Runs
        val daysSlot = slot<List<ProgramDayEntity>>()
        val rowsSlot = slot<List<ProgramExerciseEntity>>()
        coEvery { programDao.upsertDays(capture(daysSlot)) } just Runs
        coEvery { programDao.upsertExercises(capture(rowsSlot)) } just Runs

        val template = ProgramTemplates.Template(
            name = "T", description = "",
            days = listOf(
                ProgramTemplates.TemplateDay(
                    "Day 1",
                    listOf(
                        ProgramTemplates.TemplateExercise("Bench Press", sets = 4, repMin = 8, repMax = 12),
                        ProgramTemplates.TemplateExercise("Unknown Move", sets = 3, repMin = 8, repMax = 12),
                    ),
                ),
            ),
        )

        repo.createFromTemplate(template)

        assertEquals(1, daysSlot.captured.size)
        assertEquals(1, rowsSlot.captured.size)
        assertEquals("e-bench", rowsSlot.captured.single().exerciseId)
    }

    @Test
    fun `setActiveProgram resets the day pointer back to the first day`() = runTest {
        coEvery { programDao.program("p1") } returns ProgramEntity("p1", "Test", 0L)
        coEvery { programDao.days("p1") } returns listOf(
            ProgramDayEntity("d0", "p1", "Day A", 0),
            ProgramDayEntity("d1", "p1", "Day B", 1),
        )

        repo.setActiveProgram("p1")
        repo.advanceProgramPointer() // pointer -> index 1 ("Day B")
        repo.setActiveProgram("p1")  // re-activating must reset the pointer

        assertEquals("d0", repo.nextProgramDay()?.day?.id)
    }

    // --- saveSession() ----------------------------------------------------------------

    @Test
    fun `saveSession - creates a new exercise when no id or name match exists`() = runTest {
        stubWidgetUpdate()
        coEvery { exerciseDao.getAll() } returns emptyList()
        val newExercisesSlot = slot<List<ExerciseEntity>>()
        coEvery { exerciseDao.upsertAll(capture(newExercisesSlot)) } just Runs
        coEvery { workoutDao.upsertAll(any()) } just Runs
        val setsSlot = slot<List<SetEntity>>()
        coEvery { setDao.upsertAll(capture(setsSlot)) } just Runs

        repo.saveSession(
            name = "Push Day", startedAt = 1_000L, comment = "",
            exercises = listOf(
                WorkoutRepository.SaveExercise(
                    dbExerciseId = null, name = "New Move", muscleGroup = "Chest",
                    sets = listOf(WorkoutRepository.SaveSet(weightKg = 50.0, reps = 10, tagLetter = null)),
                ),
            ),
        )

        assertEquals(1, newExercisesSlot.captured.size)
        assertEquals("New Move", newExercisesSlot.captured[0].name)
        assertEquals(newExercisesSlot.captured[0].id, setsSlot.captured.single().exerciseId)
    }

    @Test
    fun `saveSession - reuses an existing exercise matched by name, case-insensitively`() = runTest {
        stubWidgetUpdate()
        coEvery { exerciseDao.getAll() } returns listOf(ExerciseEntity("e1", "Bench Press", muscles = "Chest"))
        coEvery { workoutDao.upsertAll(any()) } just Runs
        val setsSlot = slot<List<SetEntity>>()
        coEvery { setDao.upsertAll(capture(setsSlot)) } just Runs

        repo.saveSession(
            name = "Push", startedAt = 1_000L, comment = "",
            exercises = listOf(
                WorkoutRepository.SaveExercise(
                    dbExerciseId = null, name = "bench press", muscleGroup = "Chest",
                    sets = listOf(WorkoutRepository.SaveSet(weightKg = 100.0, reps = 5, tagLetter = null)),
                ),
            ),
        )

        coVerify(exactly = 0) { exerciseDao.upsertAll(any()) }
        assertEquals("e1", setsSlot.captured.single().exerciseId)
    }

    @Test
    fun `saveSession - an empty exercise list still persists a workout with no sets`() = runTest {
        stubWidgetUpdate()
        coEvery { exerciseDao.getAll() } returns emptyList()
        coEvery { workoutDao.upsertAll(any()) } just Runs
        val setsSlot = slot<List<SetEntity>>()
        coEvery { setDao.upsertAll(capture(setsSlot)) } just Runs

        repo.saveSession(name = "Empty", startedAt = 1_000L, comment = "", exercises = emptyList())

        assertTrue(setsSlot.captured.isEmpty())
        coVerify(exactly = 0) { exerciseDao.upsertAll(any()) }
    }
}
