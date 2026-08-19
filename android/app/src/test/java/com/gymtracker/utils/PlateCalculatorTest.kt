// Small (JVM) test — greedy plate-loading algorithm, pure math over the default plate set.
package com.gymtracker.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlateCalculatorTest {

    // ---- forTarget -----------------------------------------------------------------------

    @Test fun `target below the bar has no loadout`() {
        assertNull(PlateCalculator.forTarget(targetKg = 15.0, barKg = 20.0))
    }

    @Test fun `target equal to the bar loads nothing`() {
        val loadout = PlateCalculator.forTarget(targetKg = 20.0, barKg = 20.0)
        assertEquals(emptyList<Double>(), loadout?.platesPerSide)
        assertEquals(0.0, loadout?.leftoverKg)
    }

    @Test fun `sixty kg loads one 20 per side`() {
        val loadout = PlateCalculator.forTarget(targetKg = 60.0, barKg = 20.0)
        assertEquals(listOf(20.0), loadout?.platesPerSide)
        assertEquals(0.0, loadout?.leftoverKg)
    }

    @Test fun `greedy picks the largest plates first`() {
        // perSide = (100 - 20) / 2 = 40 -> 25, then 15 (20 doesn't fit the remaining 15)
        val loadout = PlateCalculator.forTarget(targetKg = 100.0, barKg = 20.0)
        assertEquals(listOf(25.0, 15.0), loadout?.platesPerSide)
        assertEquals(0.0, loadout?.leftoverKg)
    }

    @Test fun `weight the plate set cannot hit exactly reports a leftover`() {
        // perSide = (61 - 20) / 2 = 20.5 -> one 20 plate, 0.5 kg per side left unloadable
        // (smallest plate is 1.25 kg, so 0.5 kg has no plate small enough).
        val loadout = PlateCalculator.forTarget(targetKg = 61.0, barKg = 20.0)
        assertEquals(listOf(20.0), loadout?.platesPerSide)
        assertEquals(1.0, loadout?.leftoverKg!!, 1e-9)
    }

    @Test fun `epsilon guard admits a plate that floating point would otherwise just miss`() {
        // perSide lands on 2.4999999999999996 for some target math elsewhere in the app;
        // reproduce that directly and confirm the 2.5 plate still gets used.
        val loadout = PlateCalculator.forTarget(targetKg = 24.999_999_999_998, barKg = 20.0)
        assertEquals(listOf(2.5), loadout?.platesPerSide)
    }

    // ---- describe ------------------------------------------------------------------------

    @Test fun `describe with no plates is just the bar`() {
        val loadout = PlateCalculator.Loadout(barKg = 20.0, platesPerSide = emptyList(), leftoverKg = 0.0)
        assertEquals("Just the bar (20 kg)", PlateCalculator.describe(loadout))
    }

    @Test fun `describe groups repeated plates by count, heaviest first`() {
        val loadout = PlateCalculator.Loadout(
            barKg = 20.0,
            platesPerSide = listOf(20.0, 20.0, 2.5, 2.5),
            leftoverKg = 0.0,
        )
        assertEquals("Bar 20 + 2×20 + 2×2.5 per side", PlateCalculator.describe(loadout))
    }

    // ---- fmt -----------------------------------------------------------------------------

    @Test fun `fmt trims a whole-number trailing dot-zero`() {
        assertEquals("60", PlateCalculator.fmt(60.0))
    }

    @Test fun `fmt keeps a real fractional value`() {
        assertEquals("57.5", PlateCalculator.fmt(57.5))
    }

    @Test fun `fmt handles zero`() {
        assertEquals("0", PlateCalculator.fmt(0.0))
    }
}
