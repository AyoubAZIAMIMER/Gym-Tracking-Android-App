// Small (JVM) test — Brzycki (<=5 reps) / Epley (>5 reps) split. MUST stay formula-identical
// to backend/SKILL.md per the file's own header comment; this test is the tripwire for that.
package com.gymtracker.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class OneRMTest {

    private val delta = 1e-9

    @Test fun `zero weight is invalid`() {
        assertEquals(0.0, OneRM.estimate(0.0, 5), delta)
    }

    @Test fun `negative weight is invalid`() {
        assertEquals(0.0, OneRM.estimate(-10.0, 5), delta)
    }

    @Test fun `zero reps is invalid`() {
        assertEquals(0.0, OneRM.estimate(100.0, 0), delta)
    }

    @Test fun `negative reps is invalid`() {
        assertEquals(0.0, OneRM.estimate(100.0, -1), delta)
    }

    @Test fun `a single rep is its own one-rep max`() {
        assertEquals(100.0, OneRM.estimate(100.0, 1), delta)
    }

    @Test fun `two reps uses Brzycki`() {
        // 100 * 36 / (37 - 2) = 3600 / 35
        assertEquals(3600.0 / 35.0, OneRM.estimate(100.0, 2), delta)
    }

    @Test fun `five reps is the last Brzycki step`() {
        // 100 * 36 / (37 - 5) = 3600 / 32 = 112.5
        assertEquals(112.5, OneRM.estimate(100.0, 5), delta)
    }

    @Test fun `six reps switches to Epley`() {
        // 100 * (1 + 6 / 30) = 120.0
        assertEquals(120.0, OneRM.estimate(100.0, 6), delta)
    }

    @Test fun `Brzycki and Epley do not have a discontinuity worth worrying about at the boundary`() {
        // Not exact equality by design (two different formulas either side of the line) — this
        // just pins down that the jump from 5 to 6 reps is small, not a cliff.
        val atFive = OneRM.estimate(100.0, 5)
        val atSix = OneRM.estimate(100.0, 6)
        assertEquals(112.5, atFive, delta)
        assertEquals(120.0, atSix, delta)
    }

    @Test fun `high reps keep climbing under Epley with no cap`() {
        // 100 * (1 + 30 / 30) = 200.0
        assertEquals(200.0, OneRM.estimate(100.0, 30), delta)
    }
}
