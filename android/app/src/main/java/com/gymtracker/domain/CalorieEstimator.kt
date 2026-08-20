// Purpose: Session calorie estimate — MET-based, weighted by how the sets were tagged
// Inputs: each completed set's tag letter (or null), session duration, optional bodyweight
// Outputs: an Int calorie estimate — same-order-of-magnitude, not a lab measurement
package com.gymtracker.domain

object CalorieEstimator {
    private const val BASE_MET = 5.0
    private const val DEFAULT_BODY_WEIGHT_KG = 75.0

    // Tag letters as persisted on SetEntity (see SetTag.letter). A Failure/Dropset-heavy
    // session reads hotter than a Warmup-heavy one; untagged sets sit at the resistance-
    // training baseline.
    private val MULTIPLIER = mapOf(
        "W" to 0.6,   // warmup
        "L" to 0.85,  // partial
        "P" to 1.05,  // paused
        "R" to 1.15,  // forced
        "N" to 1.1,   // negative
        "X" to 1.2,   // explosive
        "D" to 1.2,   // dropset
        "F" to 1.3,   // failure
    )

    fun estimate(tagLetters: List<String?>, durationSeconds: Long, bodyWeightKg: Double?): Int {
        if (durationSeconds <= 0) return 0
        val avgMultiplier = if (tagLetters.isEmpty()) {
            1.0
        } else {
            tagLetters.map { MULTIPLIER[it] ?: 1.0 }.average()
        }
        val met = (BASE_MET * avgMultiplier).coerceIn(2.5, 8.0)
        val hours = durationSeconds / 3600.0
        val weight = bodyWeightKg?.takeIf { it > 0.0 } ?: DEFAULT_BODY_WEIGHT_KG
        return (met * weight * hours * 1.05).toInt()
    }
}
