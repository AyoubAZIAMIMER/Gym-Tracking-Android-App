// Purpose: Estimated one-rep-max — MUST stay formula-identical to backend/analytics (see backend/SKILL.md)
// Inputs: weight (kg) and reps of a completed set
// Outputs: estimated 1RM in kg (0.0 for invalid input)
package com.gymtracker.utils

object OneRM {

    // Brzycki is more accurate at low reps, Epley at higher reps — per SKILL.md:
    //   Brzycki (≤5 reps): weight * 36 / (37 - reps)
    //   Epley   (>5 reps): weight * (1 + reps / 30)
    fun estimate(weightKg: Double, reps: Int): Double = when {
        weightKg <= 0.0 || reps <= 0 -> 0.0
        reps == 1 -> weightKg
        reps <= 5 -> weightKg * 36.0 / (37.0 - reps)
        else -> weightKg * (1.0 + reps / 30.0)
    }
}
