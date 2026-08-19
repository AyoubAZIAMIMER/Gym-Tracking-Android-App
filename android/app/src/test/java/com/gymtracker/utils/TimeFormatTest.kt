// Small (JVM) test — pure formatting, no Android framework involved.
package com.gymtracker.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatTest {

    // ---- mmss --------------------------------------------------------------------------

    @Test fun `mmss zero`() {
        assertEquals("0:00", TimeFormat.mmss(0))
    }

    @Test fun `mmss pads seconds under ten`() {
        assertEquals("0:05", TimeFormat.mmss(5))
    }

    @Test fun `mmss rolls a minute at sixty seconds`() {
        assertEquals("1:00", TimeFormat.mmss(60))
    }

    @Test fun `mmss does not roll into hours`() {
        // 3661s is 1h 1m 1s, but mmss has no hour branch — minutes just keep climbing.
        // This is the reason clock() exists as a separate function; document the boundary.
        assertEquals("61:01", TimeFormat.mmss(3661))
    }

    @Test fun `mmss on a negative input is not sign-safe by design`() {
        // -5 / 60 == 0 and -5 % 60 == -5 in Kotlin, so this reads "0:-5" — exactly why
        // signedMmss() exists as the caller-facing function for anything that can go negative.
        assertEquals("0:-5", TimeFormat.mmss(-5))
    }

    // ---- signedMmss ----------------------------------------------------------------------

    @Test fun `signedMmss zero has no sign`() {
        assertEquals("0:00", TimeFormat.signedMmss(0))
    }

    @Test fun `signedMmss positive has no sign`() {
        assertEquals("1:30", TimeFormat.signedMmss(90))
    }

    @Test fun `signedMmss negative gets a plus prefix and the absolute value`() {
        assertEquals("+1:30", TimeFormat.signedMmss(-90))
    }

    @Test fun `signedMmss negative one second`() {
        assertEquals("+0:01", TimeFormat.signedMmss(-1))
    }

    // ---- clock -------------------------------------------------------------------------

    @Test fun `clock zero millis`() {
        assertEquals("0:00", TimeFormat.clock(0L))
    }

    @Test fun `clock floors sub-second millis`() {
        assertEquals("0:59", TimeFormat.clock(59_999L))
    }

    @Test fun `clock negative millis is coerced to zero, not a crash`() {
        assertEquals("0:00", TimeFormat.clock(-5_000L))
    }

    @Test fun `clock just under an hour stays in mm-ss form`() {
        assertEquals("59:59", TimeFormat.clock(3_599_000L))
    }

    @Test fun `clock rolls into h-mm-ss at exactly one hour`() {
        assertEquals("1:00:00", TimeFormat.clock(3_600_000L))
    }

    @Test fun `clock formats hours minutes and seconds together`() {
        assertEquals("1:01:01", TimeFormat.clock(3_661_000L))
    }
}
