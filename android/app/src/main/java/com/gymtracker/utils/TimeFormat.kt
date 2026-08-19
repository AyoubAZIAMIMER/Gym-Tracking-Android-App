// Purpose: Shared duration formatting for the elapsed clock, rest timer, and summary
// Inputs: seconds or milliseconds
// Outputs: "m:ss" / "h:mm:ss" strings
package com.gymtracker.utils

import kotlin.math.abs

object TimeFormat {

    fun mmss(totalSeconds: Int): String =
        "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)

    /** Same as [mmss], but negative means over — "+0:15" for 15 seconds past the target. */
    fun signedMmss(totalSeconds: Int): String {
        val sign = if (totalSeconds < 0) "+" else ""
        return "$sign${mmss(abs(totalSeconds))}"
    }

    fun clock(elapsedMillis: Long): String {
        val s = (elapsedMillis / 1000).coerceAtLeast(0)
        return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)
        else mmss(s.toInt())
    }
}
