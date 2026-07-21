// Purpose: Subtle rank ladder (Identity v8 "Blue Hour" gamification — SUBTLE only). One
//          overall training tier earned by lifetime working-set tonnage: Wood → Olympian,
//          surfaced as a single quiet "N to next" cue on Stats. No badges, no level-up
//          theatre. UI-free: the screen maps a Rank to its medal colour.
// Inputs: lifetime working volume in kg
// Outputs: current Standing (tier, next tier, fraction filled, kg to go)
package com.gymtracker.domain

enum class Rank(val label: String, val minVolumeKg: Double) {
    WOOD("Wood", 0.0),
    BRONZE("Bronze", 100_000.0),
    SILVER("Silver", 400_000.0),
    GOLD("Gold", 1_000_000.0),
    OLYMPIAN("Olympian", 2_500_000.0);

    val next: Rank? get() = entries.getOrNull(ordinal + 1)

    /** A lifter's standing at a given lifetime tonnage. */
    data class Standing(
        val rank: Rank,
        val next: Rank?,
        val progress: Float?,   // fraction of the way to `next` (0..1); null at top tier
        val toNextKg: Double?,  // kg of working volume still to earn; null at top tier
    )

    companion object {
        // Thresholds are lifetime working-set tonnage, tuned for long-term progression.
        fun of(totalVolumeKg: Double): Rank = entries.last { totalVolumeKg >= it.minVolumeKg }

        fun standing(totalVolumeKg: Double): Standing {
            val cur = of(totalVolumeKg)
            val nxt = cur.next
            val progress = nxt?.let {
                ((totalVolumeKg - cur.minVolumeKg) / (it.minVolumeKg - cur.minVolumeKg))
                    .toFloat().coerceIn(0f, 1f)
            }
            val toGo = nxt?.let { (it.minVolumeKg - totalVolumeKg).coerceAtLeast(0.0) }
            return Standing(cur, nxt, progress, toGo)
        }
    }
}
