// Purpose: Library state + exercise CRUD (create, edit, archive/delete, merge placeholders)
// Inputs: WorkoutRepository.observeExercises() + CRUD calls
// Outputs: list of LibRow via StateFlow
package com.gymtracker.ui.screens.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gymtracker.data.WorkoutRepository
import com.gymtracker.ui.screens.session.StarterExercises
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibRow(
    val id: String?,
    val name: String,
    val muscles: String,
    val equipment: String,
    val description: String,
    val isCustom: Boolean,
) {
    // imported Progression built-ins whose names weren't in the backup
    val isPlaceholder: Boolean get() = id?.startsWith("pgn-") == true
}

class LibraryViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = WorkoutRepository.get(app)

    val rows = repo.observeExercises()
        .map { list ->
            if (list.isEmpty()) {
                StarterExercises.map { LibRow(null, it.name, it.muscleGroup, "", "", false) }
            } else {
                list.map { LibRow(it.id, it.name, it.muscles, it.equipment, it.description, it.isCustom) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveExercise(
        existingId: String?,
        name: String,
        muscles: String,
        equipment: String,
        description: String,
    ) = viewModelScope.launch {
        if (existingId == null) {
            repo.createExercise(name, muscles, equipment, description)
        } else {
            repo.exerciseById(existingId)?.let { entity ->
                repo.updateExercise(
                    entity.copy(
                        name = name.trim(),
                        muscles = muscles,
                        equipment = equipment.trim(),
                        description = description.trim(),
                    )
                )
            }
        }
    }

    /** Returns via callback whether the exercise was deleted (true) or archived (false). */
    fun deleteOrArchive(id: String, onDone: (archived: Boolean) -> Unit) = viewModelScope.launch {
        // repo.deleteOrArchiveExercise returns `!used` — true means it was hard-deleted, the
        // opposite of what onDone's callers (and its own `archived` param name) expect. Found
        // live: after fixing the archive-vs-delete decision itself (WorkoutRepository.kt) to
        // also cover program references, archiving a program-referenced exercise correctly
        // kept its row and the program's reference — but the toast still said "deleted"
        // because this passed the un-inverted boolean straight through.
        onDone(!repo.deleteOrArchiveExercise(id))
    }

    fun merge(fromId: String, toId: String) = viewModelScope.launch {
        repo.mergeExercise(fromId, toId)
    }
}
