// Small (JVM) test — the double-progression rule that decides next session's load. This is
// business logic, not glue: the app "makes the loading call" here, so wrong output is a wrong
// weight suggestion shown to a real person at the rack.
package com.gymtracker.domain

import com.gymtracker.domain.Progression.Kind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionTest {

    private val delta = 1e-9

    // ---- invalid input has no basis for a call --------------------------------------------

    @Test fun `non-positive repMin has no plan`() {
        assertNull(Progression.plan(repMin = 0, repMax = 12, last = listOf(100.0 to 10), previous = emptyList()))
    }

    @Test fun `repMax below repMin has no plan`() {
        assertNull(Progression.plan(repMin = 12, repMax = 8, last = listOf(100.0 to 10), previous = emptyList()))
    }

    @Test fun `no sets logged last session has no plan`() {
        assertNull(Progression.plan(repMin = 8, repMax = 12, last = emptyList(), previous = emptyList()))
    }

    @Test fun `a set with no rep count logged blocks the whole call`() {
        assertNull(
            Progression.plan(
                repMin = 8, repMax = 12,
                last = listOf(100.0 to 10, 100.0 to null),
                previous = emptyList(),
            )
        )
    }

    @Test fun `bodyweight work with no weight logged has no basis for a load call`() {
        assertNull(
            Progression.plan(repMin = 8, repMax = 12, last = listOf(null to 10), previous = emptyList())
        )
    }

    // ---- INCREASE: every working set reached repMax ---------------------------------------

    @Test fun `clearing every set at repMax suggests one step up`() {
        val plan = Progression.plan(
            repMin = 8, repMax = 12,
            last = listOf(100.0 to 12, 100.0 to 13),
            previous = emptyList(),
        )
        assertEquals(Kind.INCREASE, plan?.kind)
        assertEquals(102.5, plan?.weightKg!!, delta)
        assertEquals(8, plan.reps)
        assertTrue(plan.line.contains("+2.5 kg"))
    }

    // ---- DELOAD: stalled below repMin two sessions running, at the same or higher top -----

    @Test fun `stalling twice at the same top weight suggests a deload`() {
        val plan = Progression.plan(
            repMin = 8, repMax = 12,
            last = listOf(100.0 to 6),
            previous = listOf(100.0 to 7),
        )
        assertEquals(Kind.DELOAD, plan?.kind)
        // floor(100 * 0.95 / 2.5) * 2.5 = floor(38.0) * 2.5 = 95.0
        assertEquals(95.0, plan?.weightKg!!, delta)
        assertEquals(8, plan.reps)
    }

    @Test fun `deload weight never drops below one step`() {
        // A very light top weight would round below STEP_KG without the floor.
        val plan = Progression.plan(
            repMin = 8, repMax = 12,
            last = listOf(2.5 to 6),
            previous = listOf(2.5 to 7),
        )
        assertEquals(Kind.DELOAD, plan?.kind)
        assertTrue((plan?.weightKg ?: 0.0) >= 2.5)
    }

    // ---- HOLD: stalled now, but no second stall at as-heavy-or-heavier weight -------------

    @Test fun `a first stall holds instead of deloading`() {
        val plan = Progression.plan(
            repMin = 8, repMax = 12,
            last = listOf(100.0 to 6),
            previous = emptyList(),
        )
        assertEquals(Kind.HOLD, plan?.kind)
        assertNull(plan?.weightKg)
        assertTrue(plan!!.line.contains("get every set past"))
    }

    @Test fun `stalling now after a lighter previous stall still just holds`() {
        // previous top (90) is below the current top (100), so stalledBefore is false —
        // the prior stall doesn't count against a weight you hadn't reached yet.
        val plan = Progression.plan(
            repMin = 8, repMax = 12,
            last = listOf(100.0 to 6),
            previous = listOf(90.0 to 6),
        )
        assertEquals(Kind.HOLD, plan?.kind)
    }

    // ---- HOLD: neither cleared nor stalled — still building toward repMax -----------------

    @Test fun `in-range but short of repMax holds and says so`() {
        val plan = Progression.plan(
            repMin = 8, repMax = 12,
            last = listOf(100.0 to 10),
            previous = emptyList(),
        )
        assertEquals(Kind.HOLD, plan?.kind)
        assertNull(plan?.weightKg)
        assertTrue(plan!!.line.contains("build the sets toward"))
    }
}
