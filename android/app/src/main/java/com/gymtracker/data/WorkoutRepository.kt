// Purpose: Single data access point — import/export, session templates, stats, persistence
// Inputs: GymDb DAOs + SharedPreferences (imported gym prefs)
// Outputs: domain models consumed by ViewModels (no raw DB access outside this file)
package com.gymtracker.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.gymtracker.data.db.ExerciseEntity
import com.gymtracker.data.db.GymDb
import com.gymtracker.data.db.ProgramDayEntity
import com.gymtracker.data.db.ProgramEntity
import com.gymtracker.data.db.ProgramExerciseEntity
import com.gymtracker.data.db.SetEntity
import com.gymtracker.data.db.WorkoutEntity
import com.gymtracker.domain.Progression
import com.gymtracker.utils.OneRM
import com.gymtracker.widget.ForgeWidgetProvider
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlinx.coroutines.flow.Flow

class WorkoutRepository(
    private val db: GymDb,
    private val prefs: SharedPreferences,
    private val appContext: Context,
) {

    val workoutCount: Flow<Int> get() = db.workoutDao().observeCount()
    val setCount: Flow<Int> get() = db.setDao().observeCount()
    val exerciseCount: Flow<Int> get() = db.exerciseDao().observeCount()
    fun observeExercises(): Flow<List<ExerciseEntity>> = db.exerciseDao().observeAll()

    suspend fun exercisesSnapshot(): List<ExerciseEntity> =
        db.exerciseDao().getAll().filterNot { it.archived }.sortedBy { it.name.lowercase() }

    // Full copy of the DB for the analytics engine (5-10k rows — fine in memory,
    // and it keeps AnalyticsEngine pure/testable, mirroring the backend design)
    data class Snapshot(
        val exercises: List<ExerciseEntity>,
        val workouts: List<WorkoutEntity>,
        val sets: List<SetEntity>,
    )

    suspend fun analyticsSnapshot(): Snapshot =
        Snapshot(db.exerciseDao().getAll(), db.workoutDao().getAll(), db.setDao().getAll())

    fun restSeconds(): Int = prefs.getInt(KEY_REST_SECONDS, 120)
    fun barWeightKg(): Double = prefs.getFloat(KEY_BAR_KG, 20f).toDouble()

    // --- profile (first-run: name, body weight, height, weekly goal) --------------

    data class Profile(
        val name: String,
        val bodyWeightKg: Double?,
        val heightCm: Int?,
        val weeklyGoal: Int,
    )

    fun profile(): Profile = Profile(
        name = prefs.getString(KEY_PROFILE_NAME, "").orEmpty(),
        bodyWeightKg = prefs.getFloat(KEY_PROFILE_WEIGHT, -1f).takeIf { it > 0f }?.toDouble(),
        heightCm = prefs.getInt(KEY_PROFILE_HEIGHT, -1).takeIf { it > 0 },
        weeklyGoal = prefs.getInt(KEY_WEEKLY_GOAL, 3),
    )

    fun saveProfile(name: String, bodyWeightKg: Double?, heightCm: Int?, weeklyGoal: Int) {
        prefs.edit()
            .putString(KEY_PROFILE_NAME, name.trim())
            .putFloat(KEY_PROFILE_WEIGHT, bodyWeightKg?.toFloat() ?: -1f)
            .putInt(KEY_PROFILE_HEIGHT, heightCm ?: -1)
            .putInt(KEY_WEEKLY_GOAL, weeklyGoal.coerceIn(1, 7))
            .apply()
    }

    fun isProfileSet(): Boolean = !prefs.getString(KEY_PROFILE_NAME, null).isNullOrBlank()

    // --- import -----------------------------------------------------------------

    data class ImportSummary(
        val workouts: Int,
        val sets: Int,
        val exercises: Int,
        val programs: Int,
    )

    suspend fun importProgression(json: String): ImportSummary {
        val parsed = ProgressionImporter.parse(json)
        val aliases = loadAliases()
        // IGNORE inserts + alias skips: re-importing never undoes renames or merges
        db.exerciseDao().insertAllIgnore(parsed.exercises.filter { it.id !in aliases })
        db.workoutDao().upsertAll(parsed.workouts)
        db.setDao().insertAllIgnore(parsed.sets)

        // programs (ABAB, Upper/Lower, …) with stable ids so re-imports upsert cleanly
        var importedPrograms = 0
        val known = db.exerciseDao().getAll().map { it.id }.toSet()
        parsed.programs.forEach { program ->
            if (program.days.isEmpty()) return@forEach
            db.programDao().upsertPrograms(
                listOf(ProgramEntity(program.programId, program.name, System.currentTimeMillis()))
            )
            val days = mutableListOf<ProgramDayEntity>()
            val rows = mutableListOf<ProgramExerciseEntity>()
            program.days.forEachIndexed { dayIdx, day ->
                days += ProgramDayEntity(day.dayId, program.programId, day.name, dayIdx)
                day.exercises.forEachIndexed { exIdx, ex ->
                    val rawId = if (ex.exerciseKey.all(Char::isDigit)) "pgn-${ex.exerciseKey}" else ex.exerciseKey
                    val resolved = aliases[rawId] ?: rawId
                    if (resolved !in known) return@forEachIndexed
                    rows += ProgramExerciseEntity(
                        id = ex.movementId,
                        dayId = day.dayId,
                        exerciseId = resolved,
                        orderIdx = exIdx,
                        targetSets = ex.sets,
                        repMin = ex.repMin,
                        repMax = ex.repMax,
                    )
                }
            }
            db.programDao().upsertDays(days)
            db.programDao().upsertExercises(rows)
            importedPrograms++
        }

        parsed.restPeriodSeconds?.let { prefs.edit().putInt(KEY_REST_SECONDS, it).apply() }
        parsed.barWeightKg?.let { prefs.edit().putFloat(KEY_BAR_KG, it.toFloat()).apply() }
        // launcher widget shows streak/days-since — refresh the moment they change
        ForgeWidgetProvider.requestUpdate(appContext)
        return ImportSummary(
            parsed.workouts.size, parsed.sets.size, parsed.exercises.size, importedPrograms,
        )
    }

    // merged placeholders leave an alias behind so program re-imports resolve correctly
    private fun loadAliases(): Map<String, String> =
        prefs.getString(KEY_ALIASES, null)?.let { json ->
            runCatching {
                GsonBuilder().create()
                    .fromJson<Map<String, String>>(json, object : TypeToken<Map<String, String>>() {}.type)
            }.getOrNull()
        } ?: emptyMap()

    private fun saveAlias(fromId: String, toId: String) {
        val updated = loadAliases().toMutableMap()
        // re-point any aliases that targeted the merged-away id, then add the new one
        updated.replaceAll { _, v -> if (v == fromId) toId else v }
        updated[fromId] = toId
        prefs.edit().putString(KEY_ALIASES, GsonBuilder().create().toJson(updated)).apply()
    }

    // --- session template: repeat the most recent workout with prev-hints --------

    data class TemplateSet(val weightKg: Double?, val reps: Int?)
    data class TemplateExercise(
        val exerciseId: String,
        val name: String,
        val muscleGroup: String,
        val sets: List<TemplateSet>,
        val note: String = "",                       // sticky machine note (v3)
        val plan: Progression.Plan? = null,          // double-progression call (program days)
    )
    data class SessionTemplate(val name: String, val exercises: List<TemplateExercise>)

    suspend fun latestWorkoutTemplate(): SessionTemplate? =
        db.workoutDao().latest()?.let { templateFromWorkout(it.id) }

    /** "Repeat this workout" from History: prefill a session from any past workout. */
    suspend fun templateFromWorkout(workoutId: String): SessionTemplate? {
        val workout = db.workoutDao().byId(workoutId) ?: return null
        val sets = db.setDao().forWorkout(workout.id)
        if (sets.isEmpty()) return null
        val exercises = db.exerciseDao().getAll().associateBy { it.id }
        val grouped = sets.groupBy { it.exerciseId }
            .entries.sortedBy { entry -> entry.value.minOf { it.orderInWorkout } }
        return SessionTemplate(
            name = workout.name.ifBlank { "Workout" },
            exercises = grouped.map { (exId, exSets) ->
                TemplateExercise(
                    exerciseId = exId,
                    name = exercises[exId]?.name ?: "Unknown exercise",
                    muscleGroup = exercises[exId]?.muscles.orEmpty(),
                    // warm-ups from last time are noise as hints — only working sets
                    sets = exSets.sortedBy { it.orderInWorkout }
                        .filter { it.tag == null }
                        .ifEmpty { exSets }
                        .map { TemplateSet(it.weightKg, it.reps) },
                    note = exercises[exId]?.note.orEmpty(),
                )
            },
        )
    }

    // --- persist a finished session ----------------------------------------------

    data class SaveSet(val weightKg: Double?, val reps: Int?, val tagLetter: String?)
    data class SaveExercise(
        val dbExerciseId: String?,
        val name: String,
        val muscleGroup: String,
        val sets: List<SaveSet>,
    )

    suspend fun saveSession(
        name: String,
        startedAt: Long,
        comment: String,
        exercises: List<SaveExercise>,
    ) {
        val existing = db.exerciseDao().getAll()
        val byId = existing.associateBy { it.id }
        val byName = existing.associateBy { it.name.lowercase() }
        val workoutId = UUID.randomUUID().toString()
        val newExercises = mutableListOf<ExerciseEntity>()
        val rows = mutableListOf<SetEntity>()
        var order = 0
        exercises.forEach { ex ->
            val entity = ex.dbExerciseId?.let(byId::get)
                ?: byName[ex.name.lowercase()]
                ?: ExerciseEntity(
                    id = UUID.randomUUID().toString(),
                    name = ex.name,
                    muscles = ex.muscleGroup,
                    isCustom = true,
                ).also { newExercises += it }
            ex.sets.forEach { s ->
                rows += SetEntity(
                    id = UUID.randomUUID().toString(),
                    workoutId = workoutId,
                    exerciseId = entity.id,
                    completedAt = System.currentTimeMillis(),
                    weightKg = s.weightKg,
                    reps = s.reps,
                    tag = s.tagLetter,
                    orderInWorkout = order++,
                )
            }
        }
        if (newExercises.isNotEmpty()) db.exerciseDao().upsertAll(newExercises)
        db.workoutDao().upsertAll(
            listOf(WorkoutEntity(workoutId, name, startedAt, System.currentTimeMillis(), comment))
        )
        db.setDao().upsertAll(rows)
        // launcher widget shows streak/days-since — refresh the moment they change
        ForgeWidgetProvider.requestUpdate(appContext)
    }

    // --- home stats ----------------------------------------------------------------

    data class HomeStats(
        val hasData: Boolean,
        val workoutsThisWeek: Int,
        val doneWeekdays: Set<DayOfWeek>,
        val lastWorkoutDaysAgo: Int?,
        val streakWeeks: Int,
    )

    suspend fun homeStats(): HomeStats {
        val times = db.workoutDao().allStartTimes()
        if (times.isEmpty()) return HomeStats(false, 0, emptySet(), null, 0)
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val weekStart = today.with(DayOfWeek.MONDAY)
        val dates = times.map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
        val thisWeek = dates.filter { !it.isBefore(weekStart) }
        val weekStarts = dates.map { it.with(DayOfWeek.MONDAY) }.toSet()
        var streak = 0
        var cursor = if (weekStart in weekStarts) weekStart else weekStart.minusWeeks(1)
        while (cursor in weekStarts) {
            streak++
            cursor = cursor.minusWeeks(1)
        }
        return HomeStats(
            hasData = true,
            workoutsThisWeek = thisWeek.size,
            doneWeekdays = thisWeek.map { it.dayOfWeek }.toSet(),
            lastWorkoutDaysAgo = ChronoUnit.DAYS.between(dates.max(), today).toInt(),
            streakWeeks = streak,
        )
    }

    // --- muscle freshness (recovery) --------------------------------------------------

    data class MuscleFreshness(
        val muscle: String,
        val lastTrainedDaysAgo: Int,
        val freshnessPercent: Int,
    )

    suspend fun muscleFreshness(): List<MuscleFreshness> {
        val since = System.currentTimeMillis() - 14L * 24 * 3_600_000
        val recent = db.setDao().since(since)
        if (recent.isEmpty()) return emptyList()
        val exercises = db.exerciseDao().getAll().associateBy { it.id }
        val lastByMuscle = mutableMapOf<String, Long>()
        recent.forEach { set ->
            val muscles = exercises[set.exerciseId]?.muscles.orEmpty()
            muscles.split("·").map { it.trim() }.filter { it.isNotEmpty() }.forEach { raw ->
                ProgressionImporter.canonicalMuscle(raw)?.let { canonical ->
                    lastByMuscle[canonical] =
                        maxOf(lastByMuscle[canonical] ?: 0L, set.completedAt)
                }
            }
        }
        val now = System.currentTimeMillis()
        return lastByMuscle.map { (muscle, trainedAt) ->
            val hours = (now - trainedAt) / 3_600_000
            MuscleFreshness(
                muscle = muscle,
                lastTrainedDaysAgo = (hours / 24).toInt(),
                // same linear 72h model as SampleData — replaced by volume-weighted later
                freshnessPercent = ((hours * 100) / 72).toInt().coerceIn(0, 100),
            )
        }.sortedBy { it.freshnessPercent }
    }

    // --- history (calendar + workout log + detail) -----------------------------------

    data class HistoryRow(
        val workoutId: String,
        val name: String,
        val startedAt: Long,
        val durationMillis: Long?,
        val setCount: Int,
        val volumeKg: Double,
        val muscles: String,
    )

    data class MonthHistory(
        val rows: List<HistoryRow>,
        val volumeByDay: Map<Int, Double>,   // day of month → working volume
    )

    suspend fun earliestWorkoutStart(): Long? = db.workoutDao().earliestStart()

    suspend fun monthHistory(month: YearMonth): MonthHistory {
        val zone = ZoneId.systemDefault()
        val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val workouts = db.workoutDao().between(start, end)
        if (workouts.isEmpty()) return MonthHistory(emptyList(), emptyMap())
        val setsByWorkout = db.setDao().forWorkouts(workouts.map { it.id }).groupBy { it.workoutId }
        val exercises = db.exerciseDao().getAll().associateBy { it.id }
        val volumeByDay = mutableMapOf<Int, Double>()
        val rows = workouts.map { w ->
            val sets = setsByWorkout[w.id].orEmpty()
            // warm-ups don't count toward volume anywhere in the app (see AnalyticsEngine)
            val volume = sets.filter { it.tag != "W" }
                .sumOf { (it.weightKg ?: 0.0) * (it.reps ?: 0) }
            val day = Instant.ofEpochMilli(w.startedAt).atZone(zone).dayOfMonth
            volumeByDay[day] = (volumeByDay[day] ?: 0.0) + volume
            HistoryRow(
                workoutId = w.id,
                name = w.name.ifBlank { "Workout" },
                startedAt = w.startedAt,
                durationMillis = w.endedAt?.let { it - w.startedAt }?.takeIf { it > 0 },
                setCount = sets.size,
                volumeKg = volume,
                muscles = sets.map { it.exerciseId }.distinct()
                    .flatMap { exercises[it]?.muscles.orEmpty().split("·").map(String::trim) }
                    .mapNotNull(ProgressionImporter::canonicalMuscle)
                    .distinct()
                    .joinToString(", "),
            )
        }
        return MonthHistory(rows, volumeByDay)
    }

    /** All-time best e1RM per exercise (warm-ups excluded) — live-PR baseline for sessions. */
    suspend fun bestE1rmByExercise(): Map<String, Double> =
        db.setDao().getAll().asSequence()
            .filter { it.tag != "W" && it.weightKg != null && it.reps != null && it.reps > 0 }
            .groupBy { it.exerciseId }
            .mapValues { (_, sets) -> sets.maxOf { OneRM.estimate(it.weightKg!!, it.reps!!) } }

    data class DetailSet(
        val weightKg: Double?,
        val reps: Int?,
        val tag: String?,
        val isPr: Boolean,
    )

    data class DetailExercise(
        val exerciseId: String,
        val name: String,
        val muscles: String,
        val sets: List<DetailSet>,
    )

    data class WorkoutDetail(
        val workout: WorkoutEntity,
        val exercises: List<DetailExercise>,
        val totalSets: Int,
        val totalVolumeKg: Double,
    )

    suspend fun workoutDetail(workoutId: String): WorkoutDetail? {
        val workout = db.workoutDao().byId(workoutId) ?: return null
        val sets = db.setDao().forWorkout(workoutId)
        val names = db.exerciseDao().getAll().associateBy { it.id }
        val exerciseIds = sets.map { it.exerciseId }.distinct()
        // all-time-best e1RM per exercise before this session; a first-ever lift is no PR
        // (same baseline rule as AnalyticsEngine.prTimeline)
        val baseline = db.setDao().forExercisesBefore(exerciseIds, workout.startedAt)
            .filter { it.tag != "W" && it.weightKg != null && it.reps != null && it.reps > 0 }
            .groupBy { it.exerciseId }
            .mapValues { (_, s) -> s.maxOf { OneRM.estimate(it.weightKg!!, it.reps!!) } }
        val grouped = sets.groupBy { it.exerciseId }
            .entries.sortedBy { entry -> entry.value.minOf { it.orderInWorkout } }
        val exercises = grouped.map { (exId, exSets) ->
            var best = baseline[exId]
            DetailExercise(
                exerciseId = exId,
                name = names[exId]?.name ?: "Unknown exercise",
                muscles = names[exId]?.muscles.orEmpty(),
                sets = exSets.sortedBy { it.orderInWorkout }.map { s ->
                    val e1rm = if (s.tag != "W" && s.weightKg != null && s.reps != null && s.reps > 0) {
                        OneRM.estimate(s.weightKg, s.reps)
                    } else null
                    val isPr = e1rm != null && best != null && e1rm > best!!
                    if (e1rm != null) best = maxOf(best ?: e1rm, e1rm)
                    DetailSet(s.weightKg, s.reps, s.tag, isPr)
                },
            )
        }
        return WorkoutDetail(
            workout = workout,
            exercises = exercises,
            totalSets = sets.size,
            totalVolumeKg = sets.filter { it.tag != "W" }
                .sumOf { (it.weightKg ?: 0.0) * (it.reps ?: 0) },
        )
    }

    // --- export --------------------------------------------------------------------

    suspend fun exportJson(): String {
        val gson = GsonBuilder().setPrettyPrinting().create()
        return gson.toJson(
            mapOf(
                "format" to "repforge-v1",
                "exportedAt" to Instant.now().toString(),
                "exercises" to db.exerciseDao().getAll(),
                "workouts" to db.workoutDao().getAll(),
                "sets" to db.setDao().getAll(),
            )
        )
    }

    suspend fun exportCsv(): String {
        val exercises = db.exerciseDao().getAll().associateBy { it.id }
        val workouts = db.workoutDao().getAll().associateBy { it.id }
        val sb = StringBuilder("completed_at,workout,exercise,weight_kg,reps,tag,e1rm_kg\n")
        db.setDao().getAll().sortedBy { it.completedAt }.forEach { s ->
            val e1rm = if (s.weightKg != null && s.reps != null && s.reps > 0) {
                "%.1f".format(OneRM.estimate(s.weightKg, s.reps))
            } else ""
            sb.append(Instant.ofEpochMilli(s.completedAt)).append(',')
                .append(csv(workouts[s.workoutId]?.name.orEmpty())).append(',')
                .append(csv(exercises[s.exerciseId]?.name ?: s.exerciseId)).append(',')
                .append(s.weightKg?.let { "%.2f".format(it) }.orEmpty()).append(',')
                .append(s.reps?.toString().orEmpty()).append(',')
                .append(s.tag.orEmpty()).append(',')
                .append(e1rm).append('\n')
        }
        return sb.toString()
    }

    private fun csv(field: String): String = "\"${field.replace("\"", "\"\"")}\""

    // --- exercise catalog + CRUD -----------------------------------------------------

    private data class CatalogEntry(
        val name: String,
        val muscles: List<String>,
        val equipment: String,
        val description: String,
    )

    /** Seed the built-in catalog once (id "cat-<slug>" so templates/exports stay stable). */
    suspend fun seedCatalogIfNeeded() {
        if (prefs.getInt(KEY_CATALOG_VERSION, 0) >= CATALOG_VERSION) return
        val json = appContext.assets.open("exercise_catalog.json")
            .bufferedReader().use { it.readText() }
        val entries: List<CatalogEntry> =
            GsonBuilder().create().fromJson(json, object : TypeToken<List<CatalogEntry>>() {}.type)
        val existingNames = db.exerciseDao().getAll().map { it.name.lowercase() }.toSet()
        val toInsert = entries
            .filter { it.name.lowercase() !in existingNames }
            .map { e ->
                ExerciseEntity(
                    id = "cat-" + e.name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-'),
                    name = e.name,
                    muscles = e.muscles.joinToString(" · "),
                    equipment = e.equipment,
                    isCustom = false,
                    description = e.description,
                )
            }
        if (toInsert.isNotEmpty()) db.exerciseDao().upsertAll(toInsert)
        prefs.edit().putInt(KEY_CATALOG_VERSION, CATALOG_VERSION).apply()
    }

    /**
     * Assign target muscles (+equipment/description) to exercises that arrived without
     * them — imported/renamed names matched by token key against the curated
     * [ExerciseInfo] table (ExRx/ACE-consensus assignments). Idempotent; run at startup
     * and after imports so the library and body figures are never blank.
     */
    suspend fun fillExerciseInfo(): Int {
        var updated = 0
        db.exerciseDao().getAll().forEach { e ->
            if (e.muscles.isNotBlank() && e.description.isNotBlank()) return@forEach
            val info = ExerciseInfo.byTokenKey[CsvNamer.tokenKey(e.name)] ?: return@forEach
            db.exerciseDao().upsertAll(
                listOf(
                    e.copy(
                        muscles = e.muscles.ifBlank { info.muscles },
                        equipment = e.equipment.ifBlank { info.equipment },
                        description = e.description.ifBlank { info.description },
                    )
                )
            )
            updated++
        }
        return updated
    }

    suspend fun exerciseById(id: String): ExerciseEntity? =
        db.exerciseDao().getAll().firstOrNull { it.id == id }

    suspend fun createExercise(
        name: String,
        muscles: String,
        equipment: String,
        description: String,
    ): ExerciseEntity {
        val entity = ExerciseEntity(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            muscles = muscles,
            equipment = equipment.trim(),
            isCustom = true,
            description = description.trim(),
        )
        db.exerciseDao().upsertAll(listOf(entity))
        return entity
    }

    suspend fun updateExercise(entity: ExerciseEntity) =
        db.exerciseDao().upsertAll(listOf(entity))

    /** Delete if unused, otherwise archive (history must never be orphaned). */
    suspend fun deleteOrArchiveExercise(id: String): Boolean {
        val used = db.setDao().countForExercise(id) > 0
        if (used) {
            exerciseById(id)?.let { updateExercise(it.copy(archived = true)) }
        } else {
            db.exerciseDao().delete(id)
        }
        return !used
    }

    data class CsvNamingSummary(
        val renamed: Int,
        val merged: Int,
        val remainingPlaceholders: Int,
        val rowsParsed: Int,
    )

    /**
     * Auto-name "Exercise #N" placeholders from a Progression CSV export:
     * rows are matched to already-imported sets by timestamp (offset auto-calibrated,
     * weight/reps as tie-breakers). A placeholder whose recovered name equals an existing
     * exercise (exactly, or by word-set: "Barbell Squat" = "Squat (Barbell)") is merged
     * into it — inheriting muscles/description; otherwise it is renamed in place.
     */
    suspend fun nameExercisesFromCsv(csvText: String): CsvNamingSummary {
        val rows = CsvNamer.parse(csvText)
        val placeholderIds = db.exerciseDao().getAll()
            .filter { it.id.startsWith("pgn-") }.map { it.id }.toSet()
        val placeholderSets = db.setDao().getAll().filter { it.exerciseId in placeholderIds }
        val offset = CsvNamer.detectOffsetMillis(rows, placeholderSets)
        val winners = CsvNamer.winners(CsvNamer.votes(rows, placeholderSets, offset))

        var renamed = 0
        var merged = 0
        winners.forEach { (placeholderId, name) ->
            val named = db.exerciseDao().getAll().filterNot { it.id.startsWith("pgn-") }
            val exact = named.firstOrNull { it.name.equals(name, ignoreCase = true) }
            val byTokens = named.filter { CsvNamer.tokenKey(it.name) == CsvNamer.tokenKey(name) }
            val target = exact ?: byTokens.singleOrNull()
            if (target != null) {
                mergeExercise(placeholderId, target.id)
                merged++
            } else {
                exerciseById(placeholderId)?.let { updateExercise(it.copy(name = name)) }
                renamed++
            }
        }
        // "unnamed" = still carrying the placeholder display name (renamed ones keep pgn- ids)
        val remaining = db.exerciseDao().getAll()
            .count { it.name.matches(Regex("Exercise #\\d+")) }
        // recovered names can now pick up muscles/descriptions from the curated table
        fillExerciseInfo()
        return CsvNamingSummary(renamed, merged, remaining, rows.size)
    }

    /** Move all history from an imported placeholder onto a named exercise, then drop it. */
    suspend fun mergeExercise(fromId: String, toId: String): Int {
        if (fromId == toId) return 0
        val moved = db.setDao().reassignExercise(fromId, toId)
        db.programDao().reassignExercise(fromId, toId)
        db.exerciseDao().delete(fromId)
        saveAlias(fromId, toId)
        return moved
    }

    // --- programs ---------------------------------------------------------------------

    data class ProgramExerciseDetail(val row: ProgramExerciseEntity, val exercise: ExerciseEntity?)
    data class ProgramDayDetail(val day: ProgramDayEntity, val exercises: List<ProgramExerciseDetail>)
    data class ProgramDetail(val program: ProgramEntity, val days: List<ProgramDayDetail>)

    fun observePrograms(): Flow<List<ProgramEntity>> = db.programDao().observePrograms()

    suspend fun dayDetail(dayId: String): ProgramDayDetail? {
        val day = db.programDao().day(dayId) ?: return null
        val exercises = db.exerciseDao().getAll().associateBy { it.id }
        return ProgramDayDetail(
            day = day,
            exercises = db.programDao().dayExercises(dayId)
                .map { ProgramExerciseDetail(it, exercises[it.exerciseId]) },
        )
    }

    suspend fun programDetail(programId: String): ProgramDetail? {
        val program = db.programDao().program(programId) ?: return null
        val exercises = db.exerciseDao().getAll().associateBy { it.id }
        val days = db.programDao().days(programId).map { day ->
            ProgramDayDetail(
                day = day,
                exercises = db.programDao().dayExercises(day.id)
                    .map { ProgramExerciseDetail(it, exercises[it.exerciseId]) },
            )
        }
        return ProgramDetail(program, days)
    }

    suspend fun createProgram(name: String): String {
        val id = UUID.randomUUID().toString()
        db.programDao().upsertPrograms(listOf(ProgramEntity(id, name.trim(), System.currentTimeMillis())))
        return id
    }

    suspend fun createFromTemplate(template: ProgramTemplates.Template): String {
        val byName = db.exerciseDao().getAll().associateBy { it.name.lowercase() }
        val programId = UUID.randomUUID().toString()
        db.programDao().upsertPrograms(
            listOf(ProgramEntity(programId, template.name, System.currentTimeMillis()))
        )
        val days = mutableListOf<ProgramDayEntity>()
        val rows = mutableListOf<ProgramExerciseEntity>()
        template.days.forEachIndexed { dayIdx, day ->
            val dayId = UUID.randomUUID().toString()
            days += ProgramDayEntity(dayId, programId, day.name, dayIdx)
            day.exercises.forEachIndexed { exIdx, ex ->
                val entity = byName[ex.exerciseName.lowercase()] ?: return@forEachIndexed
                rows += ProgramExerciseEntity(
                    id = UUID.randomUUID().toString(),
                    dayId = dayId,
                    exerciseId = entity.id,
                    orderIdx = exIdx,
                    targetSets = ex.sets,
                    repMin = ex.repMin,
                    repMax = ex.repMax,
                )
            }
        }
        db.programDao().upsertDays(days)
        db.programDao().upsertExercises(rows)
        return programId
    }

    suspend fun addDay(programId: String, name: String) {
        val order = db.programDao().days(programId).size
        db.programDao().upsertDays(
            listOf(ProgramDayEntity(UUID.randomUUID().toString(), programId, name.trim(), order))
        )
    }

    suspend fun deleteDay(dayId: String) {
        db.programDao().deleteExercisesOf(dayId)
        db.programDao().deleteDay(dayId)
    }

    suspend fun deleteProgram(programId: String) {
        db.programDao().days(programId).forEach { db.programDao().deleteExercisesOf(it.id) }
        db.programDao().deleteDaysOf(programId)
        db.programDao().deleteProgram(programId)
        if (activeProgramId() == programId) setActiveProgram(null)
    }

    suspend fun addExerciseToDay(dayId: String, exerciseId: String) {
        val order = db.programDao().dayExercises(dayId).size
        db.programDao().upsertExercises(
            listOf(
                ProgramExerciseEntity(
                    id = UUID.randomUUID().toString(),
                    dayId = dayId,
                    exerciseId = exerciseId,
                    orderIdx = order,
                    targetSets = 3,
                    repMin = 8,
                    repMax = 12,
                )
            )
        )
    }

    suspend fun removeProgramExercise(id: String) = db.programDao().deleteProgramExercise(id)

    fun activeProgramId(): String? = prefs.getString(KEY_ACTIVE_PROGRAM, null)

    fun setActiveProgram(programId: String?) {
        prefs.edit()
            .putString(KEY_ACTIVE_PROGRAM, programId)
            .putInt(KEY_PROGRAM_DAY_IDX, 0)
            .apply()
    }

    data class NextProgramDay(val programName: String, val day: ProgramDayEntity)

    suspend fun nextProgramDay(): NextProgramDay? {
        val programId = activeProgramId() ?: return null
        val program = db.programDao().program(programId) ?: return null
        val days = db.programDao().days(programId)
        if (days.isEmpty()) return null
        val idx = prefs.getInt(KEY_PROGRAM_DAY_IDX, 0) % days.size
        return NextProgramDay(program.name, days[idx])
    }

    /** Called after a session started from the active program is saved. */
    suspend fun advanceProgramPointer() {
        val programId = activeProgramId() ?: return
        val count = db.programDao().days(programId).size
        if (count == 0) return
        val next = (prefs.getInt(KEY_PROGRAM_DAY_IDX, 0) + 1) % count
        prefs.edit().putInt(KEY_PROGRAM_DAY_IDX, next).apply()
    }

    /** Session template for a program day; hints come from each exercise's last logged session. */
    suspend fun sessionTemplateFromDay(dayId: String): SessionTemplate? {
        val day = db.programDao().day(dayId) ?: return null
        val exercises = db.exerciseDao().getAll().associateBy { it.id }
        val rows = db.programDao().dayExercises(day.id)
        if (rows.isEmpty()) return null
        return SessionTemplate(
            name = day.name,
            exercises = rows.mapNotNull { row ->
                val exercise = exercises[row.exerciseId] ?: return@mapNotNull null
                val (history, older) = lastTwoSessionSets(row.exerciseId)
                TemplateExercise(
                    exerciseId = exercise.id,
                    name = exercise.name,
                    muscleGroup = exercise.muscles,
                    sets = (0 until row.targetSets).map { i ->
                        val h = history.getOrNull(i)
                        TemplateSet(
                            weightKg = h?.weightKg,
                            reps = h?.reps ?: row.repMax,
                        )
                    },
                    note = exercise.note,
                    // program days carry a rep range → the app makes the loading call
                    plan = Progression.plan(
                        repMin = row.repMin,
                        repMax = row.repMax,
                        last = history.map { it.weightKg to it.reps },
                        previous = older.map { it.weightKg to it.reps },
                    ),
                )
            },
        )
    }

    /** Working sets of the two most recent workouts containing this exercise
     *  (newest first — second list may be empty). */
    private suspend fun lastTwoSessionSets(exerciseId: String): Pair<List<SetEntity>, List<SetEntity>> {
        val recent = db.setDao().recentForExercise(exerciseId).filter { it.tag != "W" }
        val workoutIds = recent.map { it.workoutId }.distinct().take(2)
        fun of(id: String?) = if (id == null) emptyList() else {
            recent.filter { it.workoutId == id }.sortedBy { it.orderInWorkout }
        }
        return of(workoutIds.getOrNull(0)) to of(workoutIds.getOrNull(1))
    }

    /** Sticky machine note (seat height, pin, grip) — lives on the exercise. */
    suspend fun setExerciseNote(exerciseId: String, note: String) =
        db.exerciseDao().updateNote(exerciseId, note.trim())

    companion object {
        private const val KEY_REST_SECONDS = "rest_seconds"
        private const val KEY_BAR_KG = "bar_kg"
        private const val KEY_CATALOG_VERSION = "catalog_version"
        private const val KEY_ALIASES = "exercise_aliases"
        private const val KEY_ACTIVE_PROGRAM = "active_program_id"
        private const val KEY_PROGRAM_DAY_IDX = "program_day_idx"
        private const val KEY_PROFILE_NAME = "profile_name"
        private const val KEY_PROFILE_WEIGHT = "profile_weight_kg"
        private const val KEY_PROFILE_HEIGHT = "profile_height_cm"
        private const val KEY_WEEKLY_GOAL = "weekly_goal"
        private const val CATALOG_VERSION = 1

        @Volatile private var instance: WorkoutRepository? = null

        fun get(context: Context): WorkoutRepository =
            instance ?: synchronized(this) {
                instance ?: WorkoutRepository(
                    GymDb.get(context),
                    context.applicationContext
                        .getSharedPreferences("repforge_prefs", Context.MODE_PRIVATE),
                    context.applicationContext,
                ).also { instance = it }
            }
    }
}
