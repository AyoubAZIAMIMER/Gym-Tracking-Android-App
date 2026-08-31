// Purpose: Parse a Progression app backup (.pgnbkp JSON) into Room entities
// Inputs: raw backup JSON ({sessions, exercises, programs, profile, config})
// Outputs: Result with exercises/workouts/sets + imported preferences
package com.gymtracker.data

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.gymtracker.data.db.ExerciseEntity
import com.gymtracker.data.db.SetEntity
import com.gymtracker.data.db.WorkoutEntity

object ProgressionImporter {

    // programs come with stable ids so re-imports upsert instead of duplicating
    data class ImportedProgramExercise(
        val movementId: String,
        val exerciseKey: String,   // numeric built-in id or custom UUID
        val sets: Int,
        val repMin: Int,           // 0/0 = AMRAP or no target
        val repMax: Int,
    )

    data class ImportedProgramDay(
        val dayId: String,
        val name: String,
        val exercises: List<ImportedProgramExercise>,
    )

    data class ImportedProgram(
        val programId: String,
        val name: String,
        val days: List<ImportedProgramDay>,
    )

    data class Result(
        val exercises: List<ExerciseEntity>,
        val workouts: List<WorkoutEntity>,
        val sets: List<SetEntity>,
        val programs: List<ImportedProgram>,
        val restPeriodSeconds: Int?,
        val barWeightKg: Double?,
    )

    fun parse(json: String): Result {
        val root = JsonParser.parseString(json).asJsonObject

        // Custom exercises ship with names; built-ins are referenced by number only
        // (their names are not in the backup) → imported as renameable placeholders.
        val statuses = root.obj("config")?.obj("exerciseStatuses")
        val exercises = mutableMapOf<String, ExerciseEntity>()
        root.getAsJsonArray("exercises")?.forEach { el ->
            val o = el.asJsonObject
            val id = o.str("id") ?: return@forEach
            exercises[id] = ExerciseEntity(
                id = id,
                name = o.str("name") ?: "Unnamed exercise",
                muscles = o.getAsJsonArray("muscles")
                    ?.mapNotNull { m -> m.takeIf { it.isJsonPrimitive }?.asString }
                    ?.joinToString(" · ") { pretty(it) } ?: "",
                equipment = o.str("equipment")?.let(::pretty) ?: "",
                isCustom = true,
                archived = statuses?.str(id) == "ARCHIVED",
            )
        }

        val workouts = mutableListOf<WorkoutEntity>()
        val sets = mutableListOf<SetEntity>()
        root.getAsJsonArray("sessions")?.forEach { el ->
            val s = el.asJsonObject
            val sessionId = s.str("id") ?: return@forEach
            val startTime = s.long("startTime") ?: return@forEach
            workouts += WorkoutEntity(
                id = sessionId,
                name = s.obj("workout")?.str("name").orEmpty(),
                startedAt = startTime,
                endedAt = s.long("endTime"),
            )
            s.getAsJsonArray("performances")?.forEachIndexed { index, pe ->
                val p = pe.asJsonObject
                val rawExId = p.str("exerciseId") ?: return@forEachIndexed
                val exId = if (rawExId.all(Char::isDigit)) "pgn-$rawExId" else rawExId
                if (exId !in exercises) {
                    exercises[exId] = ExerciseEntity(
                        id = exId,
                        name = "Exercise #$rawExId",
                        isCustom = false,
                    )
                }
                val rawWeight = p.dbl("weight")
                val weightKg = when {
                    rawWeight == null -> null
                    p.str("weightUnit") == "POUNDS" -> rawWeight * 0.45359237
                    else -> rawWeight
                }
                sets += SetEntity(
                    id = p.str("id") ?: "$sessionId-$index",
                    workoutId = sessionId,
                    exerciseId = exId,
                    completedAt = p.long("completedAt") ?: startTime,
                    weightKg = weightKg,
                    reps = p.dbl("repetitions")?.toInt(),
                    tag = if (p.str("mark") == "WARMUP") "W" else null,
                    orderInWorkout = index,
                )
            }
        }

        // --- programs: weeks → routines → workout {name, movements[{exerciseId, plans}]} ---
        val programs = mutableListOf<ImportedProgram>()
        root.getAsJsonArray("programs")?.forEach { el ->
            val p = el.asJsonObject
            val programId = p.str("id") ?: return@forEach
            val days = mutableListOf<ImportedProgramDay>()
            p.getAsJsonArray("weeks")?.forEach { weekEl ->
                weekEl.asJsonObject.getAsJsonArray("routines")?.forEach { routineEl ->
                    val workout = routineEl.asJsonObject.obj("workout") ?: return@forEach
                    val dayId = workout.str("id") ?: return@forEach
                    val movements = mutableListOf<ImportedProgramExercise>()
                    workout.getAsJsonArray("movements")?.forEach { movEl ->
                        val m = movEl.asJsonObject
                        val movementId = m.str("id") ?: return@forEach
                        val exerciseKey = m.str("exerciseId") ?: return@forEach
                        val plans = m.getAsJsonArray("plans")
                        var repMin = 0
                        var repMax = 0
                        // explicit flag, not "repMax == 0", so a legitimately-zero first ranged
                        // plan (min present but max absent, min itself 0) can't be mistaken for
                        // "no plan picked yet" and get silently overwritten by a later plan —
                        // the rule is "take the first ranged plan," full stop.
                        var rangePicked = false
                        plans?.forEach planLoop@{ planEl ->
                            if (rangePicked) return@planLoop
                            val range = planEl.asJsonObject.obj("repetitionRange") ?: return@planLoop
                            val min = range.long("min")?.toInt()
                            if (min != null) {
                                repMin = min
                                repMax = range.long("max")?.toInt() ?: min
                                rangePicked = true
                            }
                        }
                        movements += ImportedProgramExercise(
                            movementId = movementId,
                            exerciseKey = exerciseKey,
                            sets = plans?.size() ?: 3,
                            repMin = repMin,
                            repMax = repMax,
                        )
                    }
                    days += ImportedProgramDay(dayId, workout.str("name") ?: "Day", movements)
                }
            }
            programs += ImportedProgram(programId, p.str("name") ?: "Program", days)
        }

        val prefs = root.obj("profile")?.obj("preferences")
        return Result(
            exercises = exercises.values.toList(),
            workouts = workouts,
            sets = sets,
            programs = programs,
            restPeriodSeconds = prefs?.long("restPeriod")?.let { (it / 1000L).toInt() },
            barWeightKg = prefs?.dbl("equipmentWeight"),
        )
    }

    /** The 10 canonical groups, in body order. A group absent from freshness data simply
     *  hasn't been trained inside the lookback window — which means it is fully recovered. */
    val CANONICAL_MUSCLES = listOf(
        "Chest", "Back", "Shoulders", "Biceps", "Triceps",
        "Abs", "Glutes", "Quads", "Hamstrings", "Calves",
    )

    /** Buckets any muscle label (ours or Progression's enums) into the 10 canonical groups. */
    fun canonicalMuscle(raw: String): String? {
        val u = raw.uppercase()
        return when {
            "PEC" in u || "CHEST" in u -> "Chest"
            "DELT" in u || "SHOULDER" in u -> "Shoulders"
            "BICEP" in u -> "Biceps"
            "TRICEP" in u -> "Triceps"
            "FOREARM" in u -> null
            "LAT" in u || "BACK" in u || "TRAP" in u || "RHOMBOID" in u -> "Back"
            "AB" in u || "CORE" in u || "OBLIQUE" in u -> "Abs"
            "GLUTE" in u -> "Glutes"
            "QUAD" in u -> "Quads"
            "HAMSTRING" in u -> "Hamstrings"
            "CALF" in u || "CALV" in u -> "Calves"
            else -> null
        }
    }

    // --- tolerant Gson accessors -------------------------------------------------
    private fun JsonObject.obj(key: String): JsonObject? =
        get(key)?.takeIf { it.isJsonObject }?.asJsonObject

    private fun JsonObject.str(key: String): String? =
        get(key)?.takeIf { it.isJsonPrimitive }?.asString

    private fun JsonObject.long(key: String): Long? =
        get(key)?.takeIf { it.isJsonPrimitive }?.asLong

    private fun JsonObject.dbl(key: String): Double? =
        get(key)?.takeIf { it.isJsonPrimitive }?.asDouble

    private fun pretty(enumName: String): String =
        enumName.lowercase().split('_', ' ').joinToString(" ") { w ->
            w.replaceFirstChar { it.uppercase() }
        }
}
