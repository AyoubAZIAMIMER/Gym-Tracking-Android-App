// Purpose: Session calorie estimate — MET values from the peer-reviewed Compendium of
//          Physical Activities, per set's actual equipment and tag
// Inputs: each completed set's equipment + tag letter, session duration, optional bodyweight
// Outputs: an Int calorie estimate — same-order-of-magnitude, not a lab measurement
package com.gymtracker.domain

object CalorieEstimator {
    // Source: Ainsworth BE, Haskell WL, Herrmann SD, et al. "2011 Compendium of Physical
    // Activities: A Second Update of Codes and MET Values." Medicine & Science in Sports &
    // Exercise, 43(8):1575-1581, 2011 — the standard reference NIH/CDC/ACSM calorie
    // calculators are built on. Two codes cover a gym session's own two modalities:
    //   02050 "resistance training (weight lifting - free weight, nautilus or universal),
    //          power lifting or body building, vigorous effort" = 6.0 METs
    //   02020 "calisthenics (e.g. push ups, sit ups, pull-ups, jumping jacks), vigorous
    //          effort" = 8.0 METs
    // Both are the Compendium's *measured* (not estimated) vigorous-effort values — the
    // right anchor for a working set, since a set is a burst of vigorous effort even inside
    // an easier session. The set's tag then scales that anchor toward how the set actually
    // went, rather than only a session-wide average.
    private const val MET_WEIGHTED = 6.0     // Code 02050
    private const val MET_BODYWEIGHT = 8.0   // Code 02020
    private const val DEFAULT_BODY_WEIGHT_KG = 75.0

    // Tag letters as persisted on SetEntity (see SetTag.letter) — relative to the vigorous-
    // effort anchor above, not an independent MET of their own.
    private val TAG_MULTIPLIER = mapOf(
        "W" to 0.6,   // warmup
        "L" to 0.85,  // partial
        "P" to 1.05,  // paused
        "R" to 1.15,  // forced
        "N" to 1.1,   // negative
        "X" to 1.2,   // explosive
        "D" to 1.2,   // dropset
        "F" to 1.3,   // failure
    )

    data class SetSample(val equipment: String, val tagLetter: String?)

    /** Equal time-share per set is the honest choice here: sets don't carry their own
     *  completion timestamps (SetEntity.completedAt is stamped once, at save time, for the
     *  whole batch), so a per-set duration split would be fabricated precision. */
    fun estimate(sets: List<SetSample>, durationSeconds: Long, bodyWeightKg: Double?): Int {
        if (sets.isEmpty() || durationSeconds <= 0) return 0
        val weight = bodyWeightKg?.takeIf { it > 0.0 } ?: DEFAULT_BODY_WEIGHT_KG
        val hoursPerSet = (durationSeconds.toDouble() / sets.size) / 3600.0
        return sets.sumOf { s ->
            val met = metFor(s.equipment) * (TAG_MULTIPLIER[s.tagLetter] ?: 1.0)
            met * weight * hoursPerSet * 1.05
        }.toInt()
    }

    private fun metFor(equipment: String): Double =
        if (equipment.equals("Bodyweight", ignoreCase = true)) MET_BODYWEIGHT else MET_WEIGHTED
}
