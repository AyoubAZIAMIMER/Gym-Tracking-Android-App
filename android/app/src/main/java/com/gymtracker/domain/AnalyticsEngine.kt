// Purpose: Pure-Kotlin analytics over the training history — formulas mirror backend/SKILL.md
//          (e1RM series, volume, PRs, plateau, progressive overload, weekly volume, calendar)
// Inputs: raw entity lists from WorkoutRepository.analyticsSnapshot()
// Outputs: chart-ready series + badges consumed by Stats/ExerciseStats ViewModels
package com.gymtracker.domain

import com.gymtracker.data.db.ExerciseEntity
import com.gymtracker.data.db.SetEntity
import com.gymtracker.data.db.WorkoutEntity
import com.gymtracker.utils.OneRM
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object AnalyticsEngine {

    data class Point(val time: Long, val value: Double)

    enum class PrKind { WEIGHT, E1RM }

    data class PrEvent(
        val time: Long,
        val exerciseId: String,
        val exerciseName: String,
        val kind: PrKind,
        val value: Double,
        val reps: Int?,
    )

    data class SessionSummary(
        val time: Long,
        val workoutName: String,
        val sets: Int,
        val topWeightKg: Double?,
        val topReps: Int?,
        val bestE1rm: Double?,
        val volumeKg: Double,
    )

    data class ExerciseAnalytics(
        val e1rmSeries: List<Point>,
        val volumeSeries: List<Point>,
        val bestWeightKg: Double?,
        val bestE1rm: Double?,
        val totalSets: Int,
        val totalVolumeKg: Double,
        val plateaued: Boolean,
        val trendKgPerWeek: Double?,        // e1RM regression slope over the last 90 days
        val overloadSuggestionKg: Double?,  // ready to add weight (SKILL.md rule)
        val recentSessions: List<SessionSummary>,
    )

    data class TopExercise(val exerciseId: String, val name: String, val volumeKg: Double, val sets: Int)

    private val zone: ZoneId get() = ZoneId.systemDefault()
    private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

    // --- per-exercise ------------------------------------------------------------

    fun forExercise(
        exerciseId: String,
        allSets: List<SetEntity>,
        workouts: List<WorkoutEntity>,
    ): ExerciseAnalytics {
        val workoutById = workouts.associateBy { it.id }
        val sessions = allSets.asSequence()
            .filter { it.exerciseId == exerciseId }
            .groupBy { it.workoutId }
            .toList()
            .sortedBy { (workoutId, sets) -> workoutById[workoutId]?.startedAt ?: sets.minOf { it.completedAt } }

        val e1rmSeries = mutableListOf<Point>()
        val volumeSeries = mutableListOf<Point>()
        val summaries = mutableListOf<SessionSummary>()

        sessions.forEach { (workoutId, sets) ->
            val time = workoutById[workoutId]?.startedAt ?: sets.minOf { it.completedAt }
            // warm-ups don't count toward strength/volume numbers
            val working = sets.filter { it.tag != "W" }.ifEmpty { sets }
            val volume = working.sumOf { (it.weightKg ?: 0.0) * (it.reps ?: 0) }
            val bestE1rm = working.mapNotNull { s ->
                val w = s.weightKg ?: return@mapNotNull null
                val r = s.reps ?: return@mapNotNull null
                if (r > 0) OneRM.estimate(w, r) else null
            }.maxOrNull()
            val top = working.filter { it.weightKg != null }.maxByOrNull { it.weightKg!! }
            if (bestE1rm != null) e1rmSeries += Point(time, bestE1rm)
            if (volume > 0) volumeSeries += Point(time, volume)
            summaries += SessionSummary(
                time = time,
                workoutName = workoutById[workoutId]?.name.orEmpty(),
                sets = working.size,
                topWeightKg = top?.weightKg,
                topReps = top?.reps,
                bestE1rm = bestE1rm,
                volumeKg = volume,
            )
        }

        return ExerciseAnalytics(
            e1rmSeries = e1rmSeries,
            volumeSeries = volumeSeries,
            bestWeightKg = summaries.mapNotNull { it.topWeightKg }.maxOrNull(),
            bestE1rm = e1rmSeries.maxOfOrNull { it.value },
            totalSets = sessions.sumOf { it.second.size },
            totalVolumeKg = volumeSeries.sumOf { it.value },
            plateaued = detectPlateau(e1rmSeries),
            trendKgPerWeek = trendPerWeek(e1rmSeries),
            overloadSuggestionKg = overloadSuggestion(summaries),
            recentSessions = summaries.sortedByDescending { it.time }.take(8),
        )
    }

    /** SKILL.md: no 1RM improvement over the last 4 sessions of the same exercise. */
    fun detectPlateau(e1rmSeries: List<Point>): Boolean {
        if (e1rmSeries.size < 5) return false
        val lastFour = e1rmSeries.takeLast(4).maxOf { it.value }
        val before = e1rmSeries.dropLast(4).maxOf { it.value }
        return lastFour <= before
    }

    /** Least-squares slope of e1RM over the last 90 days, in kg/week. */
    fun trendPerWeek(e1rmSeries: List<Point>): Double? {
        val cutoff = System.currentTimeMillis() - 90L * 24 * 3_600_000
        val recent = e1rmSeries.filter { it.time >= cutoff }
        if (recent.size < 3) return null
        val n = recent.size.toDouble()
        val meanX = recent.sumOf { it.time.toDouble() } / n
        val meanY = recent.sumOf { it.value } / n
        val denom = recent.sumOf { (it.time - meanX) * (it.time - meanX) }
        if (denom == 0.0) return null
        val slopePerMs = recent.sumOf { (it.time - meanX) * (it.value - meanY) } / denom
        return slopePerMs * 7L * 24 * 3_600_000
    }

    /**
     * SKILL.md progressive overload: the last 2 sessions held the same top weight and reps
     * did not drop → suggest +2.5 kg (compound default; isolation classification comes later).
     */
    fun overloadSuggestion(summaries: List<SessionSummary>): Double? {
        val recent = summaries.sortedBy { it.time }.takeLast(2)
        if (recent.size < 2) return null
        val (prev, last) = recent
        val w1 = prev.topWeightKg ?: return null
        val w2 = last.topWeightKg ?: return null
        val r1 = prev.topReps ?: return null
        val r2 = last.topReps ?: return null
        return if (w1 == w2 && r2 >= r1) 2.5 else null
    }

    // --- global -------------------------------------------------------------------

    /** Volume per week (Mondays), zero-filled so bars are continuous. */
    fun weeklyVolume(sets: List<SetEntity>, weeks: Int = 12): List<Pair<LocalDate, Double>> {
        val byWeek = sets.asSequence()
            .filter { it.tag != "W" }
            .groupBy { it.completedAt.toLocalDate().with(DayOfWeek.MONDAY) }
            .mapValues { (_, s) -> s.sumOf { (it.weightKg ?: 0.0) * (it.reps ?: 0) } }
        val thisWeek = LocalDate.now(zone).with(DayOfWeek.MONDAY)
        return (weeks - 1 downTo 0).map { back ->
            val week = thisWeek.minusWeeks(back.toLong())
            week to (byWeek[week] ?: 0.0)
        }
    }

    /** Weight PRs in chronological order (baseline excluded), newest first. */
    fun prTimeline(
        sets: List<SetEntity>,
        exercises: List<ExerciseEntity>,
        limit: Int = 10,
    ): List<PrEvent> {
        val names = exercises.associate { it.id to it.name }
        val events = mutableListOf<PrEvent>()
        sets.asSequence()
            .filter { it.tag != "W" && it.weightKg != null }
            .sortedBy { it.completedAt }
            .groupBy { it.exerciseId }
            .forEach { (exId, exSets) ->
                var maxWeight = Double.MIN_VALUE
                var first = true
                exSets.forEach { s ->
                    val w = s.weightKg!!
                    if (w > maxWeight) {
                        if (!first) {
                            events += PrEvent(
                                time = s.completedAt,
                                exerciseId = exId,
                                exerciseName = names[exId] ?: "Unknown",
                                kind = PrKind.WEIGHT,
                                value = w,
                                reps = s.reps,
                            )
                        }
                        maxWeight = w
                        first = false
                    }
                }
            }
        return events.sortedByDescending { it.time }.take(limit)
    }

    /** Session durations in minutes (implausible outliers dropped). */
    fun durationSeries(workouts: List<WorkoutEntity>, last: Int = 30): List<Point> =
        workouts.asSequence()
            .filter { it.endedAt != null }
            .map { Point(it.startedAt, (it.endedAt!! - it.startedAt) / 60_000.0) }
            .filter { it.value in 5.0..300.0 }
            .sortedBy { it.time }
            .toList()
            .takeLast(last)

    /** Day → volume for the calendar heatmap (last [days]). */
    fun calendarVolume(sets: List<SetEntity>, days: Int = 140): Map<LocalDate, Double> {
        val cutoff = System.currentTimeMillis() - days.toLong() * 24 * 3_600_000
        return sets.asSequence()
            .filter { it.completedAt >= cutoff }
            .groupBy { it.completedAt.toLocalDate() }
            .mapValues { (_, s) -> s.sumOf { (it.weightKg ?: 0.0) * (it.reps ?: 0) } }
    }

    fun topExercises(
        sets: List<SetEntity>,
        exercises: List<ExerciseEntity>,
        sinceDays: Int = 30,
        limit: Int = 5,
    ): List<TopExercise> {
        val cutoff = System.currentTimeMillis() - sinceDays.toLong() * 24 * 3_600_000
        val names = exercises.associate { it.id to it.name }
        return sets.asSequence()
            .filter { it.completedAt >= cutoff && it.tag != "W" }
            .groupBy { it.exerciseId }
            .map { (exId, s) ->
                TopExercise(
                    exerciseId = exId,
                    name = names[exId] ?: "Unknown",
                    volumeKg = s.sumOf { (it.weightKg ?: 0.0) * (it.reps ?: 0) },
                    sets = s.size,
                )
            }
            .sortedByDescending { it.volumeKg }
            .take(limit)
    }
}
