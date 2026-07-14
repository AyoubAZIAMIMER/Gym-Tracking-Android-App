// Purpose: Bundled demonstration photos per exercise — matched from the public-domain
//          free-exercise-db (Unlicense; licenses/UNLICENSE-free-exercise-db.txt).
//          Two frames per exercise: start and end position of the movement.
// Inputs: assets/media_map.json (tokenKey → asset paths), assets/exercise_media/*.jpg
// Outputs: imagesFor(name) — empty list when no demonstration exists
package com.gymtracker.data

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

object ExerciseMedia {

    @Volatile private var map: Map<String, List<String>>? = null

    private fun load(context: Context): Map<String, List<String>> =
        map ?: synchronized(this) {
            map ?: runCatching {
                val json = context.assets.open("media_map.json")
                    .bufferedReader().use { it.readText() }
                GsonBuilder().create().fromJson<Map<String, List<String>>>(
                    json, object : TypeToken<Map<String, List<String>>>() {}.type
                )
            }.getOrNull().orEmpty().also { map = it }
        }

    /** Asset paths of the demonstration frames for this exercise (order: start, end). */
    fun imagesFor(context: Context, name: String): List<String> =
        load(context)[CsvNamer.tokenKey(name)] ?: emptyList()
}
