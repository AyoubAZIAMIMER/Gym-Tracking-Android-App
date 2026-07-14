// Purpose: Import/export state — Progression backup in, JSON/CSV out
// Inputs: SAF URIs from DataScreen; WorkoutRepository for the heavy lifting
// Outputs: DataUiState (live DB counts, busy flag, result message)
package com.gymtracker.ui.screens.data

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gymtracker.data.WorkoutRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DataUiState(
    val workouts: Int = 0,
    val sets: Int = 0,
    val exercises: Int = 0,
    val busy: Boolean = false,
    val message: String? = null,
)

class DataViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = WorkoutRepository.get(app)
    private val _ui = MutableStateFlow(DataUiState())
    val ui = _ui.asStateFlow()

    init {
        viewModelScope.launch { repo.workoutCount.collect { c -> _ui.update { it.copy(workouts = c) } } }
        viewModelScope.launch { repo.setCount.collect { c -> _ui.update { it.copy(sets = c) } } }
        viewModelScope.launch { repo.exerciseCount.collect { c -> _ui.update { it.copy(exercises = c) } } }
    }

    fun importFrom(uri: Uri) = viewModelScope.launch {
        _ui.update { it.copy(busy = true, message = null) }
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val text = getApplication<Application>().contentResolver
                    .openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("Could not read the selected file")
                repo.importProgression(text)
            }
        }
        _ui.update { st ->
            st.copy(
                busy = false,
                message = result.fold(
                    { s ->
                        "Imported ${s.workouts} workouts, ${s.sets} sets, ${s.exercises} exercises" +
                            (if (s.programs > 0) ", ${s.programs} programs" else "") + " ✓"
                    },
                    { e -> "Import failed: ${e.message}" },
                ),
            )
        }
    }

    fun importCsvNames(uri: Uri) = viewModelScope.launch {
        _ui.update { it.copy(busy = true, message = null) }
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val text = getApplication<Application>().contentResolver
                    .openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("Could not read the selected file")
                repo.nameExercisesFromCsv(text)
            }
        }
        _ui.update { st ->
            st.copy(
                busy = false,
                message = result.fold(
                    { s ->
                        "Named ${s.renamed} and merged ${s.merged} imported exercises " +
                            "(${s.remainingPlaceholders} unnamed left, ${s.rowsParsed} CSV rows) ✓"
                    },
                    { e -> "CSV naming failed: ${e.message}" },
                ),
            )
        }
    }

    fun profile(): WorkoutRepository.Profile = repo.profile()

    fun saveProfile(name: String, weightKg: Double?, heightCm: Int?, weeklyGoal: Int) =
        repo.saveProfile(name, weightKg, heightCm, weeklyGoal)

    fun exportJson(uri: Uri) = export(uri, "JSON") { repo.exportJson() }

    fun exportCsv(uri: Uri) = export(uri, "CSV") { repo.exportCsv() }

    private fun export(uri: Uri, label: String, producer: suspend () -> String) =
        viewModelScope.launch {
            _ui.update { it.copy(busy = true, message = null) }
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val text = producer()
                    getApplication<Application>().contentResolver
                        .openOutputStream(uri)?.bufferedWriter()?.use { it.write(text) }
                        ?: error("Could not open the destination file")
                }
            }
            _ui.update { st ->
                st.copy(
                    busy = false,
                    message = result.fold(
                        { "$label export saved ✓" },
                        { e -> "Export failed: ${e.message}" },
                    ),
                )
            }
        }
}
