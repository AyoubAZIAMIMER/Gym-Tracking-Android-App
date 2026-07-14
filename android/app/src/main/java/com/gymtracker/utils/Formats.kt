// Purpose: Display formatting for accumulated numbers (volumes) — avoids FP noise like
//          "14851.200000000004 kg" that Double.toString produces after summation
// Inputs: raw doubles
// Outputs: grouped, rounded strings ("14,851")
package com.gymtracker.utils

import java.util.Locale

object Formats {

    /** Whole-kg volume with thousands grouping: 14851.2000004 → "14,851". */
    fun volumeKg(v: Double): String = String.format(Locale.US, "%,.0f", v)

    /** Weight rounded to 1 decimal, trailing ".0" trimmed: 22.16666 → "22.2". */
    fun kg1(v: Double): String {
        val r = Math.round(v * 10.0) / 10.0
        return if (r % 1.0 == 0.0) r.toInt().toString() else r.toString()
    }

    /** Program rep-range label; (0,0) means AMRAP / no fixed target. */
    fun repRange(min: Int, max: Int): String = when {
        max <= 0 -> "AMRAP"
        min == max -> "$min"
        else -> "$min-$max"
    }
}
