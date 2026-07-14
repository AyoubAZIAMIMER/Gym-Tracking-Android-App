// Purpose: Which plates to load per side for a target weight — same greedy algorithm as backend/SKILL.md
// Inputs: target weight, bar weight, available plate sizes (defaults until the Settings screen exists)
// Outputs: Loadout (plates per side + unloadable remainder) and display strings
package com.gymtracker.utils

object PlateCalculator {

    const val DEFAULT_BAR_KG = 20.0
    val DEFAULT_PLATES = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)

    // Guards float artifacts like per_side = 2.4999999 missing a 2.5 plate
    private const val EPSILON = 1e-6

    data class Loadout(
        val barKg: Double,
        val platesPerSide: List<Double>,
        val leftoverKg: Double,
    )

    /** Greedy largest-first, mirroring plates_for_weight() in backend/SKILL.md. Null if target < bar. */
    fun forTarget(
        targetKg: Double,
        barKg: Double = DEFAULT_BAR_KG,
        availablePlates: List<Double> = DEFAULT_PLATES,
    ): Loadout? {
        if (targetKg < barKg) return null
        var perSide = (targetKg - barKg) / 2.0
        val used = mutableListOf<Double>()
        for (plate in availablePlates.sortedDescending()) {
            while (perSide >= plate - EPSILON) {
                used += plate
                perSide -= plate
            }
        }
        return Loadout(barKg, used, perSide * 2.0)
    }

    /** e.g. "Bar 20 + 2×20 + 2×2.5 per side" */
    fun describe(loadout: Loadout): String {
        if (loadout.platesPerSide.isEmpty()) return "Just the bar (${fmt(loadout.barKg)} kg)"
        val groups = loadout.platesPerSide
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.key }
            .joinToString(" + ") { (plate, count) -> "${count}×${fmt(plate)}" }
        return "Bar ${fmt(loadout.barKg)} + $groups per side"
    }

    /** Trims the pointless ".0" — gym numbers read as "60", "57.5" */
    fun fmt(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
}
