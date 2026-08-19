// Purpose: Double-progression engine — decides next session's load from the last two
//          sessions' working sets against the program's rep range (Identity: the app
//          stops being a logbook and starts making the loading call)
// Inputs: rep range + working sets (weight, reps) of the last and second-to-last session
// Outputs: Plan(kind, suggested weight/reps, one forge-voiced line) or null (no basis)
package com.gymtracker.domain

import com.gymtracker.utils.PlateCalculator
import kotlin.math.floor
import kotlin.math.roundToInt

object Progression {

    /** Smallest standard plate pair — same step the drag fields use. */
    const val STEP_KG = 2.5

    enum class Kind { INCREASE, HOLD, DELOAD }

    data class Plan(
        val kind: Kind,
        val weightKg: Double?,   // suggested prefill; null when HOLD (keep last actuals)
        val reps: Int?,          // suggested rep target at the new weight
        val line: String,        // one human line shown on the exercise card
    )

    /**
     * The rule, deliberately boring (v1):
     * - every working set reached repMax           → add 2.5 kg, aim for repMin
     * - a set fell below repMin two sessions
     *   running at the same-or-higher top weight   → deload 5% (rounded down to 2.5)
     * - anything else                              → hold and say why
     * Bodyweight work (no weights logged) has no basis for a load call → null.
     */
    fun plan(
        repMin: Int,
        repMax: Int,
        last: List<Pair<Double?, Int?>>,
        previous: List<Pair<Double?, Int?>>,
    ): Plan? {
        if (repMin <= 0 || repMax < repMin || last.isEmpty()) return null
        val top = last.mapNotNull { it.first }.maxOrNull() ?: return null
        if (last.any { it.second == null }) return null

        val allCleared = last.all { (it.second ?: 0) >= repMax }
        val stalledNow = last.any { (it.second ?: 0) < repMin }
        val stalledBefore = previous.isNotEmpty() &&
            previous.any { (it.second ?: 0) < repMin } &&
            (previous.mapNotNull { it.first }.maxOrNull() ?: 0.0) >= top

        return when {
            allCleared -> {
                val next = roundToStep(top + STEP_KG)
                Plan(
                    Kind.INCREASE, next, repMin,
                    "+${PlateCalculator.fmt(STEP_KG)} kg — you cleared ${repMax}s across the board",
                )
            }
            stalledNow && stalledBefore -> {
                val next = (floor(top * 0.95 / STEP_KG) * STEP_KG).coerceAtLeast(STEP_KG)
                Plan(
                    Kind.DELOAD, next, repMin,
                    "deload to ${PlateCalculator.fmt(next)} kg — two stalls at ${PlateCalculator.fmt(top)}",
                )
            }
            stalledNow -> Plan(
                Kind.HOLD, null, null,
                "hold ${PlateCalculator.fmt(top)} kg — get every set past $repMin first",
            )
            else -> Plan(
                Kind.HOLD, null, null,
                "hold ${PlateCalculator.fmt(top)} kg — build the sets toward ${repMax}s",
            )
        }
    }

    private fun roundToStep(kg: Double): Double = (kg / STEP_KG).roundToInt() * STEP_KG
}
