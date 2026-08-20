// Small (JVM) test — the Compendium-sourced MET math behind the Calories stat. Wrong output
// here is a wrong number shown as fact on the results screen.
package com.gymtracker.domain

import com.gymtracker.domain.CalorieEstimator.SetSample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalorieEstimatorTest {

    // ---- edge cases have no basis for an estimate ------------------------------------------

    @Test fun `no sets is zero`() {
        assertEquals(0, CalorieEstimator.estimate(emptyList(), durationSeconds = 600, bodyWeightKg = 80.0))
    }

    @Test fun `zero duration is zero`() {
        val sets = listOf(SetSample("Barbell", null))
        assertEquals(0, CalorieEstimator.estimate(sets, durationSeconds = 0, bodyWeightKg = 80.0))
    }

    // ---- tag weighting reflects actual effort ------------------------------------------------

    @Test fun `a warmup set burns less than an untagged working set of the same exercise`() {
        val warmup = CalorieEstimator.estimate(
            listOf(SetSample("Barbell", "W")), durationSeconds = 60, bodyWeightKg = 80.0,
        )
        val working = CalorieEstimator.estimate(
            listOf(SetSample("Barbell", null)), durationSeconds = 60, bodyWeightKg = 80.0,
        )
        assertTrue("warmup ($warmup) should burn less than working ($working)", warmup < working)
    }

    @Test fun `a failure set burns more than an untagged working set`() {
        val failure = CalorieEstimator.estimate(
            listOf(SetSample("Barbell", "F")), durationSeconds = 60, bodyWeightKg = 80.0,
        )
        val working = CalorieEstimator.estimate(
            listOf(SetSample("Barbell", null)), durationSeconds = 60, bodyWeightKg = 80.0,
        )
        assertTrue("failure ($failure) should burn more than working ($working)", failure > working)
    }

    // ---- equipment picks the right Compendium anchor -----------------------------------------

    @Test fun `bodyweight sets burn more than weighted sets, same duration and tag`() {
        val bodyweight = CalorieEstimator.estimate(
            listOf(SetSample("Bodyweight", null)), durationSeconds = 60, bodyWeightKg = 80.0,
        )
        val weighted = CalorieEstimator.estimate(
            listOf(SetSample("Barbell", null)), durationSeconds = 60, bodyWeightKg = 80.0,
        )
        assertTrue("bodyweight ($bodyweight) should burn more than weighted ($weighted)", bodyweight > weighted)
    }

    @Test fun `machine and cable share the weighted anchor, not a separate one`() {
        val machine = CalorieEstimator.estimate(
            listOf(SetSample("Machine", null)), durationSeconds = 60, bodyWeightKg = 80.0,
        )
        val cable = CalorieEstimator.estimate(
            listOf(SetSample("Cable Machine", null)), durationSeconds = 60, bodyWeightKg = 80.0,
        )
        val barbell = CalorieEstimator.estimate(
            listOf(SetSample("Barbell", null)), durationSeconds = 60, bodyWeightKg = 80.0,
        )
        assertEquals(barbell, machine)
        assertEquals(barbell, cable)
    }

    // ---- missing bodyweight falls back, doesn't crash or zero out ----------------------------

    @Test fun `null bodyweight still produces a positive estimate`() {
        val calories = CalorieEstimator.estimate(
            listOf(SetSample("Barbell", null)), durationSeconds = 600, bodyWeightKg = null,
        )
        assertTrue(calories > 0)
    }

    // ---- scales with more sets at the same per-set duration -----------------------------------

    @Test fun `more sets at the same pace burns more total`() {
        val threeSets = CalorieEstimator.estimate(
            List(3) { SetSample("Barbell", null) }, durationSeconds = 180, bodyWeightKg = 80.0,
        )
        val sixSets = CalorieEstimator.estimate(
            List(6) { SetSample("Barbell", null) }, durationSeconds = 360, bodyWeightKg = 80.0,
        )
        assertTrue(sixSets > threeSets)
    }
}
