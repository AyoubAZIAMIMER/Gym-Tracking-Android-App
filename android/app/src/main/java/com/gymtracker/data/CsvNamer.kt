// Purpose: Recover real names for imported "Exercise #N" placeholders from a Progression
//          CSV export. Handles the real format (Date + Set Timestamp columns, Weight Unit),
//          auto-calibrates the CSV's UTC offset, then matches sets exactly (±2 s + weight/reps).
// Inputs: raw CSV text + the placeholders' sets from Room
// Outputs: per-placeholder winning names
package com.gymtracker.data

import com.gymtracker.data.db.SetEntity
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.abs

object CsvNamer {

    /** [baseUtcMillis] = timestamp parsed as if UTC; real zone is found by [detectOffsetMillis]. */
    data class CsvRow(val name: String, val baseUtcMillis: Long, val weightKg: Double?, val reps: Int?)

    data class Vote(val placeholderId: String, val name: String)

    private val timeOfDayFormats = listOf("HH:mm:ss.SSS", "HH:mm:ss", "HH:mm")
        .map(DateTimeFormatter::ofPattern)
    private val dateTimeFormats = listOf(
        "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm",
        "dd.MM.yyyy HH:mm:ss", "dd.MM.yyyy HH:mm", "MM/dd/yyyy HH:mm:ss", "MM/dd/yyyy HH:mm",
    ).map(DateTimeFormatter::ofPattern)

    fun parse(csv: String): List<CsvRow> {
        val lines = csv.removePrefix("﻿").split("\r\n", "\n").filter { it.isNotBlank() }
        require(lines.size >= 2) { "CSV appears empty" }
        val header = splitCsvLine(lines.first()).map { it.trim().lowercase() }

        fun col(vararg needles: String): Int =
            header.indexOfFirst { h -> needles.any { it in h } }

        val nameCol = col("exercise", "movement").let { if (it >= 0) it else col("name") }
        val dateCol = header.indexOfFirst { it == "date" || (it.contains("date") && !it.contains("update")) }
        val setTsCol = col("set timestamp").let { if (it >= 0) it else col("timestamp") }
        val singleTimeCol = col("completed", "performed", "datetime")
        val weightCol = header.indexOfFirst { it.contains("weight") && !it.contains("unit") }
        val unitCol = header.indexOfFirst { it.contains("weight") && it.contains("unit") }
        val repsCol = col("rep")
        require(nameCol >= 0 && (setTsCol >= 0 || singleTimeCol >= 0 || dateCol >= 0)) {
            "Could not find exercise-name and timestamp columns. Headers: ${header.joinToString()}"
        }

        val rows = mutableListOf<CsvRow>()
        lines.drop(1).forEach { line ->
            val cells = splitCsvLine(line)
            fun cell(i: Int): String? = cells.getOrNull(i)?.trim()?.takeIf { it.isNotEmpty() }
            val name = cell(nameCol) ?: return@forEach

            val base: Long = when {
                // Progression format: separate "Date" + time-of-day "Set Timestamp"
                dateCol >= 0 && setTsCol >= 0 && setTsCol != dateCol -> {
                    val d = cell(dateCol)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    val t = cell(setTsCol)?.let(::parseTimeOfDay)
                    if (d != null && t != null) {
                        LocalDateTime.of(d, t).toInstant(ZoneOffset.UTC).toEpochMilli()
                    } else null
                }
                else -> cell(if (singleTimeCol >= 0) singleTimeCol else setTsCol)?.let(::parseDateTime)
            } ?: return@forEach

            val unit = if (unitCol >= 0) cell(unitCol)?.lowercase() else null
            val rawWeight = cell(weightCol.takeIf { it >= 0 } ?: -1)?.replace(",", ".")?.toDoubleOrNull()
            val weightKg = rawWeight?.let { if (unit?.contains("lb") == true) it * 0.45359237 else it }

            rows += CsvRow(
                name = name,
                baseUtcMillis = base,
                weightKg = weightKg,
                reps = cell(repsCol.takeIf { it >= 0 } ?: -1)?.toDoubleOrNull()?.toInt(),
            )
        }
        require(rows.isNotEmpty()) { "No usable rows (name + timestamp) found in the CSV" }
        return rows
    }

    /**
     * The CSV carries local times with no zone. Try offsets −14 h…+14 h in 30-min steps
     * against a sample of the placeholders' sets and keep the best-scoring one.
     */
    fun detectOffsetMillis(rows: List<CsvRow>, placeholderSets: List<SetEntity>): Long {
        val byKey = placeholderSets.groupBy { keyOf(it.weightKg, it.reps) }
        val sample = rows.take(400)
        var best = 0L
        var bestHits = -1
        for (half in -28..28) {
            val offset = half * 1_800_000L
            var hits = 0
            sample.forEach { row ->
                val t = row.baseUtcMillis + offset
                if (byKey[keyOf(row.weightKg, row.reps)].orEmpty()
                        .any { abs(it.completedAt - t) <= 2_000 }
                ) hits++
            }
            if (hits > bestHits) {
                bestHits = hits
                best = offset
            }
        }
        return best
    }

    fun votes(rows: List<CsvRow>, placeholderSets: List<SetEntity>, offsetMillis: Long): List<Vote> {
        val window = 120_000L
        val byBucket = placeholderSets.groupBy { it.completedAt / window }
        val votes = mutableListOf<Vote>()
        rows.forEach { row ->
            val t = row.baseUtcMillis + offsetMillis
            val bucket = t / window
            val matches = (bucket - 1..bucket + 1)
                .flatMap { byBucket[it].orEmpty() }
                .filter { set ->
                    abs(set.completedAt - t) <= 2_000 &&
                        (row.reps == null || set.reps == null || row.reps == set.reps) &&
                        (row.weightKg == null || set.weightKg == null ||
                            abs(set.weightKg - row.weightKg) < 0.51)
                }
            // only unambiguous matches vote
            matches.map { it.exerciseId }.distinct().singleOrNull()?.let { exerciseId ->
                votes += Vote(exerciseId, row.name)
            }
        }
        return votes
    }

    /** Winning name per placeholder: ≥3 votes and ≥80% agreement. */
    fun winners(votes: List<Vote>): Map<String, String> =
        votes.groupBy { it.placeholderId }.mapNotNull { (id, list) ->
            val (topName, topCount) = list.groupingBy { it.name }.eachCount()
                .maxByOrNull { it.value } ?: return@mapNotNull null
            if (topCount >= 3 && topCount * 5 >= list.size * 4) id to topName else null
        }.toMap()

    /** "Barbell Squat" and "Squat (Barbell)" share the same token key → safe to merge. */
    fun tokenKey(name: String): String =
        name.lowercase().replace("-", "")
            .split(Regex("[^a-z0-9]+"))
            .filter { it.isNotBlank() }
            .sorted()
            .joinToString(" ")

    private fun keyOf(weightKg: Double?, reps: Int?): String =
        "${weightKg?.let { Math.round(it * 100) } ?: -1}:${reps ?: -1}"

    private fun parseTimeOfDay(raw: String): LocalTime? {
        timeOfDayFormats.forEach { fmt ->
            runCatching { return LocalTime.parse(raw, fmt) }
        }
        return null
    }

    private fun parseDateTime(raw: String): Long? {
        raw.toLongOrNull()?.let { n ->
            return when {
                n > 100_000_000_000L -> n
                n > 1_000_000_000L -> n * 1000
                else -> null
            }
        }
        runCatching { return Instant.parse(raw).toEpochMilli() }
        dateTimeFormats.forEach { fmt ->
            runCatching {
                return LocalDateTime.parse(raw, fmt).toInstant(ZoneOffset.UTC).toEpochMilli()
            }
        }
        return null
    }

    // minimal RFC-4180-ish splitter (quoted fields, "" escapes, comma or semicolon)
    private fun splitCsvLine(line: String): List<String> {
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    sb.append('"'); i++
                }
                c == '"' -> inQuotes = !inQuotes
                (c == ',' || c == ';') && !inQuotes -> {
                    out += sb.toString(); sb.clear()
                }
                else -> sb.append(c)
            }
            i++
        }
        out += sb.toString()
        return out
    }
}
