// Small (JVM) test — the three pure decision functions inside AnalyticsEngine: plateau
// detection, trend slope, and the progressive-overload suggestion. `forExercise()` itself
// (the aggregation over raw Room entities) is deliberately left for a later medium/instrumented
// pass — these three are the part a wrong answer actually misleads the user about.
package com.gymtracker.domain

import com.gymtracker.domain.AnalyticsEngine.Point
import com.gymtracker.domain.AnalyticsEngine.SessionSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsEngineTest {

    // ---- detectPlateau ---------------------------------------------------------------------

    @Test fun `fewer than five sessions is never a plateau`() {
        val series = listOf(10.0, 20.0, 30.0, 25.0).mapIndexed { i, v -> Point(i.toLong(), v) }
        assertFalse(AnalyticsEngine.detectPlateau(series))
    }

    @Test fun `the last four sessions failing to beat the prior best is a plateau`() {
        // dropLast(4) -> [10, 20, 30], max 30. takeLast(4) -> [25, 25, 20, 15], max 25 <= 30.
        val series = listOf(10.0, 20.0, 30.0, 25.0, 25.0, 20.0, 15.0).mapIndexed { i, v -> Point(i.toLong(), v) }
        assertTrue(AnalyticsEngine.detectPlateau(series))
    }

    @Test fun `a new best inside the last four sessions is not a plateau`() {
        // dropLast(4) -> [10, 20, 30], max 30. takeLast(4) -> [20, 20, 20, 40], max 40 > 30.
        val series = listOf(10.0, 20.0, 30.0, 20.0, 20.0, 20.0, 40.0).mapIndexed { i, v -> Point(i.toLong(), v) }
        assertFalse(AnalyticsEngine.detectPlateau(series))
    }

    @Test fun `exactly five sessions is enough to call a plateau`() {
        // dropLast(4) -> [50], max 50. takeLast(4) -> [10, 20, 30, 25], max 30 <= 50.
        val series = listOf(50.0, 10.0, 20.0, 30.0, 25.0).mapIndexed { i, v -> Point(i.toLong(), v) }
        assertTrue(AnalyticsEngine.detectPlateau(series))
    }

    // ---- trendPerWeek ----------------------------------------------------------------------

    @Test fun `fewer than three recent points has no trend`() {
        val now = System.currentTimeMillis()
        val series = listOf(Point(now, 100.0), Point(now - 1, 105.0))
        assertNull(AnalyticsEngine.trendPerWeek(series))
    }

    @Test fun `points outside the ninety day window do not count toward the minimum`() {
        val now = System.currentTimeMillis()
        val ancientDay = 200L * 24 * 3_600_000
        val series = listOf(
            Point(now - ancientDay, 40.0),
            Point(now - ancientDay - 1, 45.0),
            Point(now, 100.0),
        )
        // only one point is inside the 90-day window -> below the minimum of three
        assertNull(AnalyticsEngine.trendPerWeek(series))
    }

    @Test fun `identical timestamps have no slope to fit`() {
        val now = System.currentTimeMillis()
        val series = listOf(Point(now, 100.0), Point(now, 105.0), Point(now, 110.0))
        assertNull(AnalyticsEngine.trendPerWeek(series))
    }

    @Test fun `a clean upward line reports the exact weekly slope`() {
        val day = 24L * 3_600_000
        val now = System.currentTimeMillis()
        // +5 kg per day, evenly spaced -> a perfect line, so the least-squares slope equals
        // the analytic slope exactly: 5 kg/day * 7 = 35 kg/week.
        val series = listOf(
            Point(now - 2 * day, 100.0),
            Point(now - day, 105.0),
            Point(now, 110.0),
        )
        assertEquals(35.0, AnalyticsEngine.trendPerWeek(series)!!, 0.01)
    }

    @Test fun `a flat line has zero trend`() {
        val day = 24L * 3_600_000
        val now = System.currentTimeMillis()
        val series = listOf(
            Point(now - 2 * day, 100.0),
            Point(now - day, 100.0),
            Point(now, 100.0),
        )
        assertEquals(0.0, AnalyticsEngine.trendPerWeek(series)!!, 0.01)
    }

    // ---- overloadSuggestion ------------------------------------------------------------------

    private fun summary(time: Long, weight: Double?, reps: Int?) = SessionSummary(
        time = time,
        workoutName = "",
        sets = 1,
        topWeightKg = weight,
        topReps = reps,
        bestE1rm = null,
        volumeKg = 0.0,
    )

    @Test fun `fewer than two sessions has no suggestion`() {
        assertNull(AnalyticsEngine.overloadSuggestion(listOf(summary(0, 100.0, 8))))
        assertNull(AnalyticsEngine.overloadSuggestion(emptyList()))
    }

    @Test fun `same weight with reps held or improved suggests the standard step`() {
        val summaries = listOf(summary(0, 100.0, 8), summary(1, 100.0, 9))
        assertEquals(2.5, AnalyticsEngine.overloadSuggestion(summaries))
    }

    @Test fun `same weight with exactly the same reps still suggests a step`() {
        val summaries = listOf(summary(0, 100.0, 8), summary(1, 100.0, 8))
        assertEquals(2.5, AnalyticsEngine.overloadSuggestion(summaries))
    }

    @Test fun `a weight change between sessions has no clear basis`() {
        val summaries = listOf(summary(0, 95.0, 8), summary(1, 100.0, 8))
        assertNull(AnalyticsEngine.overloadSuggestion(summaries))
    }

    @Test fun `reps dropping at the same weight has no suggestion`() {
        val summaries = listOf(summary(0, 100.0, 9), summary(1, 100.0, 8))
        assertNull(AnalyticsEngine.overloadSuggestion(summaries))
    }

    @Test fun `a missing weight or rep count blocks the suggestion`() {
        assertNull(AnalyticsEngine.overloadSuggestion(listOf(summary(0, null, 8), summary(1, 100.0, 8))))
        assertNull(AnalyticsEngine.overloadSuggestion(listOf(summary(0, 100.0, null), summary(1, 100.0, 8))))
    }

    @Test fun `only the two most recent sessions are considered`() {
        // An old stall at a heavier weight must not poison a fresh pair that's holding steady.
        val summaries = listOf(
            summary(0, 120.0, 4),
            summary(1, 100.0, 8),
            summary(2, 100.0, 9),
        )
        assertEquals(2.5, AnalyticsEngine.overloadSuggestion(summaries))
    }
}
