// Small (JVM) test — display formatting for accumulated volume numbers and rep ranges.
package com.gymtracker.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatsTest {

    // ---- volumeKg ------------------------------------------------------------------------

    @Test fun `volumeKg trims floating point summation noise`() {
        assertEquals("14,851", Formats.volumeKg(14_851.200_000_000_4))
    }

    @Test fun `volumeKg groups large numbers with commas`() {
        assertEquals("1,234,567", Formats.volumeKg(1_234_567.0))
    }

    @Test fun `volumeKg handles zero`() {
        assertEquals("0", Formats.volumeKg(0.0))
    }

    @Test fun `volumeKg rounds to the nearest whole kg`() {
        assertEquals("1,000", Formats.volumeKg(999.6))
    }

    // ---- kg1 -----------------------------------------------------------------------------

    @Test fun `kg1 rounds to one decimal`() {
        assertEquals("22.2", Formats.kg1(22.166_66))
    }

    @Test fun `kg1 trims a whole-number trailing dot-zero`() {
        assertEquals("22", Formats.kg1(22.0))
    }

    @Test fun `kg1 rounds a value that is already close to a whole number down`() {
        assertEquals("22", Formats.kg1(22.04))
    }

    @Test fun `kg1 rounds a value clearly past the tenth up`() {
        assertEquals("22.2", Formats.kg1(22.16))
    }

    // ---- repRange --------------------------------------------------------------------------

    @Test fun `repRange with no max is AMRAP`() {
        assertEquals("AMRAP", Formats.repRange(0, 0))
    }

    @Test fun `repRange treats any non-positive max as AMRAP even with a nonzero min`() {
        // Documents the real (if slightly surprising) behavior: min is ignored once max <= 0.
        assertEquals("AMRAP", Formats.repRange(8, 0))
    }

    @Test fun `repRange with equal min and max shows a single number`() {
        assertEquals("5", Formats.repRange(5, 5))
    }

    @Test fun `repRange with a real range shows both bounds`() {
        assertEquals("3-5", Formats.repRange(3, 5))
    }
}
